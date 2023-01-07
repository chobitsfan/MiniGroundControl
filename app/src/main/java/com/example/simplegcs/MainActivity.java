package com.example.simplegcs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {
    UsbSerialPort port = null;
    PipedOutputStream p_os;
    SerialInputOutputManager usbIoManager;
    MyMavlinkWork mav_work;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PipedInputStream p_is = new PipedInputStream();
        try {
            p_os = new PipedOutputStream(p_is);
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
        }
        mav_work = new MyMavlinkWork(p_is, new ByteArrayOutputStream(1024));
        Thread t1 = new Thread(mav_work);
        t1.start();
        detectMyDevice();
    }

    /*class MyUSBStatus extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Objects.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                detectMyDevice();
            }
        }
    }*/

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
        TextView tv1 = (TextView)findViewById(R.id.test_tv);

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        tv1.setText(driver.toString());
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Log.d("chobits", "need usb permission");
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            PendingIntent p_intent = PendingIntent.getBroadcast(this, 0,
                    new Intent("com.example.simplegcs.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
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
        tv1.setText("serial port ok");

        usbIoManager = new SerialInputOutputManager(port, this);
        usbIoManager.start();
    }

    @Override
    public void onNewData(byte[] data) {
        //Log.d("chobits", "new data " + data.length);
        try {
            p_os.write(data);
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
        }
        /*MavlinkMessage msg;
        try {
            msg = mav_conn.next();
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
            return;
        }
        while (msg != null) {
            if (msg.getPayload() instanceof Heartbeat) {
                // This is a heartbeat message
                Heartbeat hb = (Heartbeat)msg.getPayload();
                Log.d("chobits", "heartbeat " + hb.customMode() + "," + msg.getSequence());
            }
            try {
                msg = mav_conn.next();
            } catch (IOException e) {
                Log.d("chobits", e.getMessage());
                return;
            }
        }*/
    }

    @Override
    public void onRunError(Exception e) {
        Log.d("chobits", e.getMessage());
    }
}