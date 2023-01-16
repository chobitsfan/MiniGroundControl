package com.chobitsfan.minigcs;

import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MyUSBSerialListener implements SerialInputOutputManager.Listener, Runnable {
    InputStream is;
    OutputStream os;
    public UsbSerialPort port = null;

    public  MyUSBSerialListener(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    @Override
    public void onNewData(byte[] data) {
        try {
            os.write(data);
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
        }
    }

    @Override
    public void onRunError(Exception e) {
        Log.d("chobits", e.getMessage());
    }

    @Override
    public void run() {
        byte[] buf = new byte[1024];
        int len = 0;
        while (true) {
            try {
                len = is.read(buf);
            } catch (IOException e) {
                Log.d("chobits", e.getMessage());
            }
            if (len > 0 && port != null) {
                try {
                    port.write(Arrays.copyOfRange(buf, 0, len), 500);
                } catch (IOException e) {
                    Log.d("chobits", e.getMessage());
                }
            } else break;
        }
    }
}
