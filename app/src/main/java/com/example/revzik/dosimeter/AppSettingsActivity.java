package com.example.revzik.dosimeter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

public class AppSettingsActivity extends AppCompatActivity {

    private RadioGroup calibration;
    private RadioGroup lex8;
    private RadioGroup spl;
    private RadioGroup peak;

    private Button back;
    private Button loadCal;
    private Button saveCal;

    private CheckBox reset;

    private Calibrator calibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appsettingslayout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setComponents();

        Intent intent = getIntent();
        calibrator = (Calibrator)intent.getSerializableExtra(HomeActivity.CALIBRATOR);
        if(calibrator.getCurve() == 1) {
            calibration.check(R.id.cal_a);
        } else if(calibrator.getCurve() == 2) {
            calibration.check(R.id.cal_c);
        } else {
            calibration.check(R.id.cal_lin);
        }
        int lex8Curve = intent.getIntExtra(HomeActivity.LEX8_CURVE, 1);
        if(lex8Curve == 1) {
            lex8.check(R.id.lex8_a);
        } else if(lex8Curve == 2) {
            lex8.check(R.id.lex8_c);
        } else {
            lex8.check(R.id.lex8_lin);
        }
        int splCurve = intent.getIntExtra(HomeActivity.SPL_CURVE, 1);
        if(splCurve == 1) {
            spl.check(R.id.spl_a);
        } else if(splCurve == 2) {
            spl.check(R.id.spl_c);
        } else {
            spl.check(R.id.spl_lin);
        }
        int peakCurve = intent.getIntExtra(HomeActivity.PEAK_CURVE, 2);
        if(peakCurve == 1) {
            peak.check(R.id.peak_a);
        } else if(peakCurve == 2) {
            peak.check(R.id.peak_c);
        } else {
            peak.check(R.id.peak_lin);
        }

        saveCal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(calibrator.canUseStorage()) {
                    if (calibrator.save()) {
                        Toast.makeText(getApplicationContext(), R.string.cal_save_success, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.cal_save_fail, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.storage_denied, Toast.LENGTH_LONG).show();
                }
            }
        });

        loadCal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(calibrator.canUseStorage()) {
                    if (calibrator.load()) {
                        Toast.makeText(getApplicationContext(), R.string.cal_load_success, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.cal_load_fail, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.storage_denied, Toast.LENGTH_LONG).show();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                if(calibration.getCheckedRadioButtonId() == R.id.cal_a) {
                    calibrator.setCurve(1);
                } else if(calibration.getCheckedRadioButtonId() == R.id.cal_c) {
                    calibrator.setCurve(2);
                } else {
                    calibrator.setCurve(3);
                }
                intent.putExtra(HomeActivity.CALIBRATOR, calibrator);
                if(lex8.getCheckedRadioButtonId() == R.id.lex8_a) {
                    intent.putExtra(HomeActivity.LEX8_CURVE, 1);
                } else if(lex8.getCheckedRadioButtonId() == R.id.lex8_c) {
                    intent.putExtra(HomeActivity.LEX8_CURVE, 2);
                } else {
                    intent.putExtra(HomeActivity.LEX8_CURVE, 3);
                }
                if(spl.getCheckedRadioButtonId() == R.id.spl_a) {
                    intent.putExtra(HomeActivity.SPL_CURVE, 1);
                } else if(spl.getCheckedRadioButtonId() == R.id.spl_c) {
                    intent.putExtra(HomeActivity.SPL_CURVE, 2);
                } else {
                    intent.putExtra(HomeActivity.SPL_CURVE, 3);
                }
                if(peak.getCheckedRadioButtonId() == R.id.peak_a) {
                    intent.putExtra(HomeActivity.PEAK_CURVE, 1);
                } else if(peak.getCheckedRadioButtonId() == R.id.peak_c) {
                    intent.putExtra(HomeActivity.PEAK_CURVE, 2);
                } else {
                    intent.putExtra(HomeActivity.PEAK_CURVE, 3);
                }
                if(reset.isChecked()) {
                    intent.putExtra(HomeActivity.SHOULD_RESET, true);
                } else {
                    intent.putExtra(HomeActivity.SHOULD_RESET, false);
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    public void setComponents() {
        calibration = findViewById(R.id.cal_setting);
        lex8 = findViewById(R.id.lex8_setting);
        spl = findViewById(R.id.SPL_setting);
        peak = findViewById(R.id.peak_setting);
        back = findViewById(R.id.back_button);
        loadCal = findViewById(R.id.load_cal);
        saveCal = findViewById(R.id.save_cal);
        reset = findViewById(R.id.reset_measurement);
    }
}
