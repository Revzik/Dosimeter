package com.example.revzik.dosimeter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class HomeActivity extends AppCompatActivity {

    public static final String CALIBRATOR = "calibrator";
    public static final String LEX8_CURVE = "lex8";
    public static final String SPL_CURVE = "spl";
    public static final String PEAK_CURVE = "peak";
    public static final String SHOULD_RESET = "reset";
    private static final int CALIBRATOR_ACTIVITY = 1;
    private static final int SETTINGS_ACTIVITY = 2;

    private static final int RECORD_AUDIO_REQUEST = 1;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 2;
    private static final int READ_EXTERNAL_STORAGE_REQUEST = 3;

    private boolean running;
    private double lex8;
    private double spl;
    private double peak;
    private int lex8Curve;
    private int splCurve;
    private int peakCurve;
    private boolean shouldReset;

    private TextView lex8Value;
    private TextView lex8Unit;
    private ImageView lex8State;
    private TextView splValue;
    private TextView splUnit;
    private ImageView splState;
    private TextView peakValue;
    private TextView peakUnit;
    private ImageView peakState;

    private ImageButton optionsButton;
    private Button calibrateButton;
    private Button exitButton;

    private SoundAnalyzer soundAnalyzer;
    private Calibrator calibrator;
    private AppLog log;

    private String logDirectory;
    private String calibrationDirectory;

    private Thread thread;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homelayout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        createDirectory();
        requestMicPermission();
        requestReadStoragePermission();
        setComponents();

        if(soundAnalyzer.start()) {
            thread = new Thread(new BackgroundThread());
            thread.start();
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    lex8 = msg.getData().getDouble("lex8");
                    spl = msg.getData().getDouble("spl");
                    peak = msg.getData().getDouble("peak");
                }
            };
            try {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        long frameTime = System.nanoTime();

                        if(lex8Curve == 1) {
                            lex8Unit.setText(R.string.dBA);
                        } else if(lex8Curve == 2) {
                            lex8Unit.setText(R.string.dBC);
                        } else {
                            lex8Unit.setText(R.string.dB);
                        }
                        lex8Value.setText(String.valueOf(lex8));
                        if(lex8 > 85) {
                            lex8State.setImageResource(R.color.colorOverdose);
                        } else if(lex8 > 75) {
                            lex8State.setImageResource(R.color.colorWarning);
                        } else {
                            lex8State.setImageResource(R.color.colorGood);
                        }
                        if(splCurve == 1) {
                            splUnit.setText(R.string.dBA);
                        } else if(splCurve == 2) {
                            splUnit.setText(R.string.dBC);
                        } else {
                            splUnit.setText(R.string.dB);
                        }
                        splValue.setText(String.valueOf(spl));
                        if(spl > 115) {
                            splState.setImageResource(R.color.colorOverdose);
                            if(log.canUseStorage()) {
                                log.exceededSPL(spl);
                            }
                        } else if(spl > 105) {
                            splState.setImageResource(R.color.colorWarning);
                        } else {
                            splState.setImageResource(R.color.colorGood);
                        }
                         if(peakCurve == 1) {
                            peakUnit.setText(R.string.dBA);
                        } else if(peakCurve == 2) {
                            peakUnit.setText(R.string.dBC);
                        } else {
                            peakUnit.setText(R.string.dB);
                        }
                        peakValue.setText(String.valueOf(peak));
                        if(peak > 135) {
                            peakState.setImageResource(R.color.colorOverdose);
                            if(log.canUseStorage()) {
                                log.exceededPeak(peak);
                            }
                        } else if(peak > 120) {
                            peakState.setImageResource(R.color.colorWarning);
                        } else {
                            peakState.setImageResource(R.color.colorGood);
                        }

                        try {
                            handler.postDelayed(this, (frameTime - System.nanoTime() + 1000000000) / 1000000);
                        } catch(IllegalArgumentException iae) {
                            handler.postDelayed(this, 1000);
                        }
                    }
                };
                handler.postDelayed(r, 1000);
            } catch(Exception e) {
                e.printStackTrace();
                finish();
            }
        } else {
            Toast.makeText(this, R.string.init_failed, Toast.LENGTH_LONG).show();
            soundAnalyzer.stop();
            finish();
        }

        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    openActivity(view, SETTINGS_ACTIVITY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    openActivity(view, CALIBRATOR_ACTIVITY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    class BackgroundThread implements Runnable {
        @Override
        public void run() {
            Bundle bundle = new Bundle(3);
            Message message;
            while (running) {
                long frameTime = System.nanoTime();

                message = Message.obtain();

                soundAnalyzer.read();

                bundle.putDouble("lex8", soundAnalyzer.round(soundAnalyzer.getLex8(calibrator.getDBOffset(), lex8Curve)));
                bundle.putDouble("spl", soundAnalyzer.round(soundAnalyzer.getSPL(calibrator.getDBOffset(), splCurve)));
                bundle.putDouble("peak", soundAnalyzer.round(soundAnalyzer.getPeak(calibrator.getDBOffset(), peakCurve)));

                message.setData(bundle);
                handler.sendMessage(message);

                try {
                    Thread.sleep((frameTime - System.nanoTime() + 1000000000) / 1000000);
                } catch(IllegalArgumentException iae) {
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException ie) {
                        ie.printStackTrace();
                    }
                } catch(InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    //assign components
    public void setComponents() {
        lex8Value = findViewById(R.id.lex8_value);
        lex8State = findViewById(R.id.lex8_state);
        lex8Unit = findViewById(R.id.lex8_unit);
        splValue = findViewById(R.id.spl_value);
        splState = findViewById(R.id.spl_state);
        splUnit = findViewById(R.id.spl_unit);
        peakValue = findViewById(R.id.peak_value);
        peakState = findViewById(R.id.peak_state);
        peakUnit = findViewById(R.id.peak_unit);

        optionsButton = findViewById(R.id.options_button);
        calibrateButton = findViewById(R.id.calibrate_button);
        exitButton = findViewById(R.id.exit_button);

        soundAnalyzer = new SoundAnalyzer();
        calibrator = new Calibrator(calibrationDirectory, 3);
        log = new AppLog(logDirectory);

        lex8Curve = 1;
        splCurve = 1;
        peakCurve = 2;
        shouldReset = false;
        running = true;
    }

    // permissions
    public void requestMicPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, R.string.mic_perm, Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST);
            }
        }
    }

    public void createDirectory() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, R.string.storage_perm, Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST);
            }
        }

        File mainFolder = new File(Environment.getExternalStorageDirectory() + "/Dosimeter");
        boolean success = true;
        if (!mainFolder.exists()) {
            success = mainFolder.mkdir();
        }
        if (!success) {
            Toast.makeText(getApplicationContext(), R.string.save_dir_fail, Toast.LENGTH_SHORT).show();
        }

        File calibrationFolder = new File(mainFolder.getAbsolutePath() + "/Calibration");
        success = true;
        if (!calibrationFolder.exists()) {
            success = calibrationFolder.mkdir();
        }
        if (!success) {
            Toast.makeText(getApplicationContext(), R.string.cal_dir_fail, Toast.LENGTH_SHORT).show();
        }

        File logFolder = new File(mainFolder.getAbsolutePath() + "/Log");
        success = true;
        if (!logFolder.exists()) {
            success = logFolder.mkdir();
        }
        if (!success) {
            Toast.makeText(getApplicationContext(), R.string.log_dir_fail, Toast.LENGTH_SHORT).show();
        }

        logDirectory = logFolder.getAbsolutePath();
        calibrationDirectory = calibrationFolder.getAbsolutePath();
    }

    public void requestReadStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, R.string.storage_perm, Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST);
            }
        }
    }

    public void openActivity(View view, int id) {
        if(id == 1) {
            Intent intent = new Intent(this, CalibratorActivity.class);
            intent.putExtra(CALIBRATOR, calibrator);
            startActivityForResult(intent, 1);
        } else if(id == 2) {
            Intent intent = new Intent(this, AppSettingsActivity.class);
            intent.putExtra(CALIBRATOR, calibrator);
            intent.putExtra(LEX8_CURVE, lex8Curve);
            intent.putExtra(SPL_CURVE, splCurve);
            intent.putExtra(PEAK_CURVE, peakCurve);
            intent.putExtra(SHOULD_RESET, shouldReset);
            startActivityForResult(intent, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(getApplicationContext(), R.string.record_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    calibrator.setUseStorage(true);
                    log.setUseStorage(true);
                } else {
                    calibrator.setUseStorage(false);
                    log.setUseStorage(false);
                }
                return;
            }
            case 3: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    calibrator.setUseStorage(false);
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(soundAnalyzer.isInitialized()) {
            soundAnalyzer.stop();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        soundAnalyzer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(log.canUseStorage()) {
            if(lex8 > 85) {
                log.Lex8(lex8, soundAnalyzer.getLex8Counter(), true);
            } else {
                log.Lex8(lex8, soundAnalyzer.getLex8Counter(), false);
            }
        }
        if(soundAnalyzer.isInitialized()) {
            soundAnalyzer.stop();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                calibrator = (Calibrator)data.getSerializableExtra(CALIBRATOR);
            }
        } else if(requestCode == 2) {
            if(resultCode == RESULT_OK) {
                calibrator = (Calibrator)data.getSerializableExtra(CALIBRATOR);
                lex8Curve = data.getIntExtra(LEX8_CURVE, 1);
                splCurve = data.getIntExtra(SPL_CURVE, 1);
                peakCurve = data.getIntExtra(PEAK_CURVE, 2);
                if (data.getBooleanExtra(SHOULD_RESET, false)) {
                    soundAnalyzer.clearLex8();
                }
            }
        }
    }
}