package com.example.revzik.dosimeter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.Serializable;

public class SoundAnalyzer{

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    private short[] buffer;
    private short[] bufferA;
    private short[] bufferC;

    private double lex8Sum = 0;
    private int lex8Counter = 0;

    private AudioRecord myAudioRecorder;

    public SoundAnalyzer() {
        buffer = new short[SAMPLE_RATE];
        bufferA = new short[SAMPLE_RATE];
        bufferC = new short[SAMPLE_RATE];
    }

    public boolean start() {
        myAudioRecorder = findAudioRecord();
        if(myAudioRecorder != null){
            myAudioRecorder.startRecording();
            return true;
        }
        else{
            Log.e("Sonometer", "ERROR, could not create audio recorder");
            return false;
        }
    }

    public void stop() {
        if (myAudioRecorder != null) {
            myAudioRecorder.stop();
            myAudioRecorder.release();
        }
    }

    public AudioRecord findAudioRecord() {
        try {
            AudioRecord recorder = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, SAMPLE_RATE);

            if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
                return recorder;
            } else {
                recorder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean read() {
        try {
            myAudioRecorder.read(buffer, 0, SAMPLE_RATE);
            countC();
            countA();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean isInitialized() {
        if(myAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            return true;
        } else {
            return false;
        }
    }

    // app math
    public void countA() {
        double a = 0.984887;
        double b = 0.904869;

        double[] o = new double[SAMPLE_RATE];

        o[0] = buffer[0];
        o[1] = buffer[1];
        for(int i = 2; i<SAMPLE_RATE; i++) {
            o[i] = a*bufferC[i] - a*bufferC[i-1] + a*o[i-1];
        }

        bufferA[0] = buffer[0];
        bufferA[1] = bufferA[1];
        for(int i = 2; i<SAMPLE_RATE; i++) {
            bufferA[i] = (short)(b*o[i] - b*o[i-1] + b*bufferA[i-1]);
        }
    }

    public void countC() {
        double a = 0.634797;
        double b = 0.365203;
        double c = 0.997074;

        double[] o1 = new double[SAMPLE_RATE];
        double[] o2 = new double[SAMPLE_RATE];

        o1[0] = buffer[0];
        o1[1] = buffer[1];
        for(int i = 2; i<SAMPLE_RATE; i++) {
            o1[i] = a*buffer[i] + b*o1[i-1];
        }

        o2[0] = buffer[0];
        o2[1] = buffer[1];
        for(int i = 2; i<SAMPLE_RATE; i++) {
            o2[i] = a*o1[i] + b*o2[i-1];
        }

        o1[0] = buffer[0];
        o1[1] = buffer[1];
        for(int i = 2; i<SAMPLE_RATE; i++) {
            o1[i] = c*o2[i] - c*o2[i-1] + c*o1[i-1];
        }

        bufferC[0] = buffer[0];
        bufferC[1] = buffer[1];
        for(int i = 2; i<SAMPLE_RATE; i++) {
            bufferC[i] = (short)(c*o2[i] - c*o2[i-1] + c*bufferC[i-1]);
        }
    }

    public double countRMS(short[] values) {
        long squareSum = 0;

        for(short val : values) {
            squareSum += val*val;
        }

        return Math.sqrt((double)squareSum/values.length);
    }

    public double getLex8(double calibration, int curve) {
        lex8Counter++;
        lex8Sum += Math.pow(10, getSPL(calibration, curve)/10);

        return 10*Math.log10(lex8Sum/lex8Counter) + 10*Math.log10((double)lex8Counter/28800);
    }

    public int getLex8Counter() {
        return lex8Counter;
    }

    public void clearLex8() {
        lex8Sum = 0;
        lex8Counter = 0;
    }

    public double getSPL(double calibration, int curve) {
        if(curve == 1) {
            return 20 * Math.log10(countRMS(bufferA) / 32768) + calibration + 1.9998;
        } else if(curve == 2) {
            return 20 * Math.log10(countRMS(bufferC) / 32768) + calibration + 0.061847;
        } else {
            return 20 * Math.log10(countRMS(buffer) / 32768) + calibration;
        }
    }

    public double getPeak(double calibration, int curve) {
        int peak = 0;

        if(curve == 1) {
            for(short val : bufferA) {
                if(Math.abs(val*1.2589) > peak) {
                    peak = (int)Math.abs(val*1.2589);
                }
            }
        } else if(curve == 2) {
            for(short val : bufferC) {
                if(Math.abs(val*1.007146) > peak) {
                    peak = (int)Math.abs(val*1.007146);
                }
            }
        } else {
            for (short val : buffer) {
                if (Math.abs(val) > peak) {
                    peak = Math.abs(val);
                }
            }
        }

        return 20*Math.log10((double)peak/32768) + calibration;
    }

    public double round(double val) {
        return Math.round(10 * val) / 10.0;
    }
}
