package com.example.revzik.dosimeter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

public class Calibrator implements Serializable {

    private double dBOffset;
    private boolean useStorage;
    private int curve;

    private String calibrationFilePath;

    public Calibrator(String filepath, int n) {
        File calibrationFile = new File(filepath + "/calibration.cal");
        if(!calibrationFile.exists()) {
            try {
                calibrationFile.createNewFile();
                calibrationFilePath = calibrationFile.getAbsolutePath();
                useStorage = true;
            } catch (IOException e) {
                e.printStackTrace();
                useStorage = false;
            }
        } else {
            calibrationFilePath = calibrationFile.getAbsolutePath();
            useStorage = true;
        }
        if(!load()) {
            dBOffset = 98;
        }
        curve = n;
    }

    public void setCurve(int n) {
        curve = n;
    }
    public int getCurve() {
        return curve;
    }
    public double getDBOffset() {
        return dBOffset;
    }
    public void setDBOffset(double offset) {
        dBOffset = offset;
    }
    public boolean canUseStorage() {
        return useStorage;
    }
    public void setUseStorage(boolean state) {
        useStorage = state;
    }

    public boolean save() {
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(calibrationFilePath);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        try {
            try {
                fileOutputStream.write(String.valueOf(dBOffset).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            try {
                fileOutputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public boolean load() {
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(calibrationFilePath);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        try {
            dBOffset = Double.parseDouble(bufferedReader.readLine());
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
