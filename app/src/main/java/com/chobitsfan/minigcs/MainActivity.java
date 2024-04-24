package com.chobitsfan.minigcs;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.File;
import java.util.ArrayList;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    MapView map = null;
    UsbSerialPort port = null;
    SerialInputOutputManager usbIoManager;
    MyMavlinkWork mav_work;
    MyUSBSerialListener serialListener;
    long reboot_ts = 0;
    TextToSpeech tts;
    private Marker lastMarker = null;


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
                case MyMavlinkWork.UI_GLOBAL_POS:
                    if (map != null) {
                        if (lastMarker != null) {
                            map.getOverlays().remove(lastMarker);
                        }

                        Drawable icon = getResources().getDrawable(R.drawable.uav);
                        Marker startMarker = new Marker(map);
                        GeoPoint currentPosition = new GeoPoint(msg.arg1 / 1E7, msg.arg2 / 1E7);

                        startMarker.setPosition(currentPosition);
                        startMarker.setIcon(icon);
                        startMarker.setTitle("Marker Title");

                        // Add the new marker to the map
                        map.getOverlays().add(startMarker);
                        map.invalidate(); // Refresh the map

                        // Update the lastMarker reference to the new marker
                        lastMarker = startMarker;
                    }
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

    public void onArmBtn(View view) {
        mav_work.forceArm();
    }

    public void onTakeoffBtn(View view){ mav_work.takeoff(); }

    public void onGuidedBtn(View view){ mav_work.setModeGuided(); }

    public void onDisarmBtn(View view){mav_work.disarm();}

    public void onStabilizeBtn(View view){ mav_work.setModeAuto(); }

    public void onSendWaypoint(View view){
        mav_work.setGPSOrigin();
        MyMavlinkWork.sendGlobalWaypoint1(38.54, -106.92, 2);
        Toast.makeText(this, "sending GLOBAL waypoint", Toast.LENGTH_SHORT).show();
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

    public void onSubmitWaypoint(View view) {
        float lat = 0;
        float lon = 0;
        float alt = 0;
        mav_work.sendLocalWaypoint();
    }



    public void setupMapTileStorage() {
        // Setting the user agent to avoid getting banned from the OSM servers
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        // Getting the external storage directory
        File osmdroidBasePath = new File(Environment.getExternalStorageDirectory(), "osmdroid");
        File osmdroidTileCache = new File(osmdroidBasePath, "tile");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);
        Configuration.getInstance().setOsmdroidTileCache(osmdroidTileCache);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get permission to use location and storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION},
                    1);  // 1 is requestCode, a unique identifier for this request
        }



        tts = new TextToSpeech(this, this);

        ((TextView)findViewById(R.id.status_txt)).setMovementMethod(new ScrollingMovementMethod());

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

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = findViewById(R.id.osmmap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);


        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Projection projection = map.getProjection();
                GeoPoint geoPoint = (GeoPoint) projection.fromPixels((int) event.getX(), (int) event.getY());
                handleGeoPoint(geoPoint);
                return true;
            }
            return false;
        });

        IMapController mapController = map.getController();
        mapController.setZoom(13);
        GeoPoint startPoint = new GeoPoint(38.49480, -106.99539);
        mapController.setCenter(startPoint);




        //requestPermissions(arrayOf(
                // if you need to show the current location, uncomment the line below
                // Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
               // Manifest.permission.WRITE_EXTERNAL_STORAGE
        //));



        detectMyDevice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMapAndDownloadTiles();
            } else {
                Toast.makeText(this, "Permissions denied, can't operate fully.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleGeoPoint(GeoPoint geoPoint) {
        Toast.makeText(this, "Lat: " + geoPoint.getLatitude() + ", Lon: " + geoPoint.getLongitude(), Toast.LENGTH_SHORT).show();
        MyMavlinkWork.sendGlobalWaypoint(geoPoint.getLatitude(), geoPoint.getLongitude(), 3);
    }
    private void initializeMapAndDownloadTiles() {
        MapView map = findViewById(R.id.osmmap);
        setupMapTileStorage();
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        BoundingBox bbox = new BoundingBox(38.56, -106.89, 38.47, -106.97);
        CacheManager cacheManager = new CacheManager(map);
        cacheManager.downloadAreaAsync(this, bbox, 10, 19);  // Assuming permissions are already granted
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause(); // pause the map view properly
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume(); // resume the map view
        }
    }


    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
       /* if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }*/
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