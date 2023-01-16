package com.chobitsfan.minigcs;

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
import android.widget.EditText;
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
            Bundle data;
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
                case MyMavlinkWork.UI_PARAM_VAL:
                    data = msg.getData();
                    tv = (TextView)findViewById(R.id.param_val);
                    if (((TextView)findViewById(R.id.param_name)).getText().toString().toLowerCase().equals(data.getString("name"))) {
                        if (data.getBoolean("is_float")) {
                            tv.setText(Float.toString(data.getFloat("val")));
                        } else {
                            tv.setText(Integer.toString(data.getInt("val")));
                        }
                    } else if (data.getString("name").equals("chobits_param_rw_failed")) {
                        tv.setHint("failed");
                        this.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)findViewById(R.id.param_val)).setHint("parameter value");
                            }
                        }, 3000);
                    }
                    break;
            }
        }
    };
    View.OnFocusChangeListener myClearHint = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (hasFocus) {
                EditText et = (EditText) view;
                et.setHint("parameter value");
            }
        }
    };

    public void onLandBtn(View view) {
        mav_work.setModeLand();
    }

    public void onRTLBtn(View view) {
        mav_work.setModeRTL();
    }

    public void onReadParam(View view) {
        TextView tv = (TextView)findViewById(R.id.param_name);
        String param_name = tv.getText().toString();
        if (!param_name.equals("")) {
            tv = (TextView)findViewById(R.id.param_val);
            tv.setText("");
            tv.setHint("reading...");
            mav_work.readParam(param_name);
        }
    }

    public void onWriteParam(View view) {
        TextView tv = (TextView)findViewById(R.id.param_name);
        String param_name = tv.getText().toString();
        if (param_name.equals("")) return;
        tv = (TextView)findViewById(R.id.param_val);
        float param_val;
        try {
            param_val = Float.parseFloat(tv.getText().toString());
        } catch (NumberFormatException e) {
            return;
        }
        tv.setText("");
        tv.setHint("writing...");
        mav_work.writeParam(param_name, param_val);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.status_txt)).setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.param_val).setOnFocusChangeListener(myClearHint);

        PipedInputStream mav_work_is = new PipedInputStream();
        PipedOutputStream serial_os = new PipedOutputStream();
        try {
            serial_os.connect(mav_work_is);
        } catch (IOException e) {
            if (MyAppConfig.DEBUG) Log.d("chobits", e.getMessage());
        }
        PipedInputStream serial_is = new PipedInputStream();
        PipedOutputStream mav_work_os = new PipedOutputStream();
        try {
            mav_work_os.connect(serial_is);
        } catch (IOException e) {
            if (MyAppConfig.DEBUG) Log.d("chobits", e.getMessage());
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
            if (MyAppConfig.DEBUG) Log.d("chobits", "need usb permission");
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