package com.example.revzik.dosimeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import android.text.format.DateFormat;

public class AppLog {

    private boolean useStorage;

    private File saveFile;
    private FileOutputStream fileOutputStream;

    public AppLog(String filepath) {
        saveFile = new File(filepath + "/" + getDate(true) + ".txt");
        try {
            saveFile.createNewFile();
            useStorage = true;
        } catch(IOException e) {
            e.printStackTrace();
            useStorage = false;
        }
    }

    public String getDate(boolean file) {
        if(file) {
            return DateFormat.format("yyyy-MM-dd_HH:mm:ss", new Date()).toString();
        } else {
            return DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString();
        }
    }

    public void exceededSPL(double value) {
        writeMessage(getDate(false) + ": exceeded SPL, level: " + String.valueOf(value) + "\n");
    }

    public void exceededPeak(double value) {
        writeMessage(getDate(false) + ": exceeded peak, level: " + String.valueOf(value) + "\n");
    }

    public void Lex8(double value, int time, boolean exceeded) {
        if (exceeded) {
            writeMessage("exceeded Lex8, level: " + String.valueOf(value) + ", exposure time: " + String.valueOf(time) + " seconds");
        } else {
            writeMessage("Lex8: " + String.valueOf(value) + ", exposure time: " + String.valueOf(time) + " seconds");
        }
    }

    public void writeMessage(String message) {
        try {
            fileOutputStream = new FileOutputStream(saveFile, true);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            try {
                fileOutputStream.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                fileOutputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean canUseStorage() {
        return useStorage;
    }

    public void setUseStorage(boolean state) {
        useStorage = state;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
