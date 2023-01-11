package com.example.simplegcs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    UsbSerialPort port = null;
    SerialInputOutputManager usbIoManager;
    MyMavlinkWork mav_work;
    MyUSBSerialListener serialListener;
    Handler ui_handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            TextView tv;
            //String result = msg.getData().getString("message");
            //update ui
            switch (msg.what) {
                case 1:
                    tv = (TextView)findViewById(R.id.flight_mode);
                    tv.setText((String)msg.obj);
                    break;
                case 2:
                    tv = (TextView)findViewById(R.id.status_txt);
                    tv.append((String)msg.obj+"\n");
                    break;
                case 3:
                    tv = (TextView)findViewById(R.id.bat_status);
                    tv.setText(String.format("%.1f", msg.arg1 * 0.001f)+"V");
                    break;
                case 4:
                    tv = (TextView)findViewById(R.id.gps_status);
                    tv.setText((String)msg.obj);
                    break;
            }
        }
    };

    public void onLandBtn(View view) {
        mav_work.setModeLand();
    }

    public void onRTLBtn(View view) {
        mav_work.setModeRTL();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.status_txt)).setMovementMethod(new ScrollingMovementMethod());

        PipedInputStream mav_work_is = new PipedInputStream();
        PipedOutputStream serial_os = new PipedOutputStream();
        try {
            serial_os.connect(mav_work_is);
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
        }
        PipedInputStream serial_is = new PipedInputStream();
        PipedOutputStream mav_work_os = new PipedOutputStream();
        try {
            mav_work_os.connect(serial_is);
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
        }
        mav_work = new MyMavlinkWork(ui_handler, mav_work_is, mav_work_os);
        Thread t1 = new Thread(mav_work);
        t1.start();
        serialListener = new MyUSBSerialListener(serial_is, serial_os);
        Thread t2 = new Thread(serialListener);
        t2.start();

        detectMyDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void detectMyDevice() {
        //TextView tv1 = (TextView)findViewById(R.id.test_tv);

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        //tv1.setText(driver.toString());
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Log.d("chobits", "need usb permission");
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            PendingIntent p_intent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.simplegcs.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
            manager.requestPermission(driver.getDevice(), p_intent);
            return;
        }

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //tv1.setText("serial port ok");

        serialListener.port = port;
        usbIoManager = new SerialInputOutputManager(port, serialListener);
        usbIoManager.start();
    }
}