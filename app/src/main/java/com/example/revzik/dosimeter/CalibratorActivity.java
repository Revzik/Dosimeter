package com.example.revzik.dosimeter;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class CalibratorActivity extends AppCompatActivity {

    private TextView amplitudeValue;
    private TextView amplitudeUnit;
    private TextView measuredUnit;
    private TextView referenceUnit;

    private EditText measured;
    private EditText reference;

    private Button getValue;
    private Button nextPoint;
    private Button apply;
    private RadioGroup radioGroup;

    private SoundAnalyzer soundAnalyzer;
    private Calibrator calibrator;

    private Thread thread;
    private Handler handler;

    private double ls;
    private double lp;
    private double la;
    private int laCounter;
    private double amplitude;
    private double average;
    private int averageCounter;
    private int time;
    private boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibrationlayout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setComponents();

        Intent intent = getIntent();
        calibrator = (Calibrator)intent.getSerializableExtra(HomeActivity.CALIBRATOR);

        if(soundAnalyzer.start()) {
            thread = new Thread(new BackgroundThread());
            thread.start();
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    amplitude = msg.getData().getDouble("level");
                }
            };

            try {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        long frameTime = System.nanoTime();

                        amplitudeValue.setText(String.valueOf(amplitude));
                        if(calibrator.getCurve() == 1) {
                            amplitudeUnit.setText(R.string.dBA);
                            measuredUnit.setText(R.string.dBA);
                            referenceUnit.setText(R.string.dBA);
                        } else if(calibrator.getCurve() == 2) {
                            amplitudeUnit.setText(R.string.dBC);
                            measuredUnit.setText(R.string.dBC);
                            referenceUnit.setText(R.string.dBC);
                        } else {
                            amplitudeUnit.setText(R.string.dB);
                            measuredUnit.setText(R.string.dB);
                            referenceUnit.setText(R.string.dB);
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
            }
        } else {
            Toast.makeText(getApplicationContext(), "Could not initialize AudioRecorder!", Toast.LENGTH_LONG).show();
        }

        getValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    measured.setText(String.valueOf(amplitude));
                } catch(NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "@string/invalid_value", Toast.LENGTH_LONG).show();
                }
            }
        });

        nextPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ls = Double.parseDouble(measured.getText().toString());
                    lp = Double.parseDouble(reference.getText().toString());
                    la += lp - ls;
                    laCounter++;
                    measured.setText("");
                    reference.setText("");
                } catch(NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "@string/invalid_value", Toast.LENGTH_LONG).show();
                }
            }
        });

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(laCounter > 0) {
                    calibrator.setDBOffset(la/laCounter);
                    Intent intent = new Intent();
                    intent.putExtra(HomeActivity.CALIBRATOR, calibrator);
                    setResult(RESULT_OK, intent);
                }
                if(soundAnalyzer.isInitialized()) {
                    soundAnalyzer.stop();
                }
                finish();
            }
        });
    }

    class BackgroundThread implements Runnable {
        @Override
        public void run() {
            Bundle bundle = new Bundle(1);
            Message message;
            while (running) {
                long frameTime = System.nanoTime();

                message = Message.obtain();

                soundAnalyzer.read();
                setTime();

                if(time < averageCounter) {
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    average = 0;
                    averageCounter = 0;
                }

                averageCounter++;
                average += Math.pow(10, soundAnalyzer.getSPL(0, calibrator.getCurve())/10);

                if(averageCounter == time) {
                    bundle.putDouble("level", soundAnalyzer.round(10*Math.log10(average/averageCounter)));
                    average = 0;
                    averageCounter = 0;
                }

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

    public void setComponents() {
        amplitudeValue = findViewById(R.id.amplitude_val);
        amplitudeUnit = findViewById(R.id.amplitude_unit);
        measuredUnit = findViewById(R.id.measured_unit);
        referenceUnit = findViewById(R.id.reference_unit);

        measured = findViewById(R.id.measured_value);
        reference = findViewById(R.id.reference_value);

        getValue = findViewById(R.id.get_value);
        nextPoint = findViewById(R.id.next_point);
        apply = findViewById(R.id.apply);
        radioGroup = findViewById(R.id.radioGroup);

        handler = new Handler();
        soundAnalyzer = new SoundAnalyzer();
        la = 0;
        laCounter = 0;
        average = 0;
        averageCounter = 0;

        running = true;
    }

    public void setTime() {
        if(radioGroup.getCheckedRadioButtonId() == R.id.one_second) {
            time = 1;
        } else if(radioGroup.getCheckedRadioButtonId() == R.id.two_seconds) {
            time = 2;
        } else {
            time = 5;
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
        if(soundAnalyzer.isInitialized()) {
            soundAnalyzer.stop();
        }
    }
}
