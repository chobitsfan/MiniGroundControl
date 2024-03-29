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
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    UsbSerialPort port = null;
    SerialInputOutputManager usbIoManager;
    MyMavlinkWork mav_work;
    MyUSBSerialListener serialListener;
    long reboot_ts = 0;
    TextToSpeech tts;
    Handler ui_handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            TextView tv;
            Bundle data;
            //String result = msg.getData().getString("message");
            //update ui
            switch (msg.what) {
                case MyMavlinkWork.UI_FLIGHT_MODE:
                    tv = (TextView)findViewById(R.id.flight_mode);
                    tv.setText((String)msg.obj);
                    break;
                case MyMavlinkWork.UI_STATUS_TXT:
                    if (msg.arg2 == 0) {
                        tv = (TextView) findViewById(R.id.status_txt);
                        tv.append((String) msg.obj + "\n");
                    }
                    if (msg.arg1 > 0) tts.speak((String)msg.obj, TextToSpeech.QUEUE_ADD, null);
                    break;
                case MyMavlinkWork.UI_BAT_STATUS:
                    tv = (TextView)findViewById(R.id.bat_status);
                    tv.setText(Html.fromHtml(String.format("<small>Battery</small><br><big><b>%.1f</b></big><small>v</small>", msg.arg1*0.001), Html.FROM_HTML_MODE_COMPACT));
                    break;
                case MyMavlinkWork.UI_GPS_STATUS:
                    data = msg.getData();
                    tv = (TextView)findViewById(R.id.gps_status);
                    tv.setText(Html.fromHtml("<small>GPS</small><br><big><b>"+data.getString("fix")+"</b></big>", Html.FROM_HTML_MODE_COMPACT));
                    tv = (TextView)findViewById(R.id.gps_hdop);
                    tv.setText(Html.fromHtml("<small>HDOP</small><br><big><b>"+data.getString("hdop")+"</b></big>", Html.FROM_HTML_MODE_COMPACT));
                    tv = (TextView)findViewById(R.id.gps_satellites);
                    tv.setText(Html.fromHtml("<small>Satellites</small><br><big><b>"+data.getInt("satellites")+"</b></big>", Html.FROM_HTML_MODE_COMPACT));
                    break;
                case MyMavlinkWork.UI_PARAM_VAL:
                    data = msg.getData();
                    tv = (TextView)findViewById(R.id.param_val);
                    if (((TextView)findViewById(R.id.param_name)).getText().toString().equalsIgnoreCase(data.getString("name"))) {
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
                case MyMavlinkWork.UI_GLOBAL_POS:
                    tv = (TextView)findViewById(R.id.alt_status);
                    tv.setText(Html.fromHtml(String.format("<small>Altitude</small><br><big><b>%.1f</b></big><small>m</small>", msg.arg2*0.001), Html.FROM_HTML_MODE_COMPACT));
                    tv = (TextView)findViewById(R.id.alt_msl_status);
                    tv.setText(Html.fromHtml(String.format("<small>Altitude MSL</small><br><big><b>%.1f</b></big><small>m</small>", msg.arg1*0.001), Html.FROM_HTML_MODE_COMPACT));
                    break;
                case MyMavlinkWork.UI_AP_NAME:
                    tv = (TextView)findViewById(R.id.ap_name);
                    tv.setText((String)msg.obj);
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

    public void onRebootBtn(View view) {
        long ts = SystemClock.elapsedRealtime();
        if (ts - reboot_ts > 3000) {
            reboot_ts = ts;
            Toast.makeText(this, "tap again to reboot FC", Toast.LENGTH_SHORT).show();
        } else {
            reboot_ts = 0;
            mav_work.rebootFC();
            Toast.makeText(this, "rebooting FC", Toast.LENGTH_SHORT).show();
        }
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

        tts = new TextToSpeech(this, this);

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
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    void detectMyDevice() {
        //TextView tv1 = (TextView)findViewById(R.id.test_tv);

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        ProbeTable myProbeTable = UsbSerialProber.getDefaultProbeTable();
        myProbeTable.addProduct(0x1209, 0x5741, CdcAcmSerialDriver.class); // ardupilot fc
        myProbeTable.addProduct(0x2dae, 0x1016, CdcAcmSerialDriver.class); // cube orange
        UsbSerialProber myProber = new UsbSerialProber(myProbeTable);
        List<UsbSerialDriver> availableDrivers = myProber.findAllDrivers(manager);
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

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
           tts.setLanguage(Locale.US);
        }
    }
}