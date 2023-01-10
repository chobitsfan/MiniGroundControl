package com.example.simplegcs;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

import io.dronefleet.mavlink.Mavlink2Message;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.CommandLong;
import io.dronefleet.mavlink.common.Heartbeat;
import io.dronefleet.mavlink.common.MavAutopilot;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavType;
import io.dronefleet.mavlink.common.Statustext;
import io.dronefleet.mavlink.common.SysStatus;

public class MyMavlinkWork implements Runnable {
    MavlinkConnection mav_conn;
    Handler ui_handler;
    static String[] flight_mode = {"STABILIZE", "ACRO", "ALT_HOLD", "AUTO", "GUIDED", "LOITER", "RTL", "CIRCLE", "POSITION", "LAND"};
    long last_sys_status_ts = 0;
    long last_gps_raw_ts = 0;
    long last_hb_ts = 0;
    public MyMavlinkWork(Handler handler, InputStream is, OutputStream os) {
        mav_conn = MavlinkConnection.create(is, os);
        ui_handler = handler;
    }

    @Override
    public void run() {
        MavlinkMessage msg;
        while (true) {
            try {
                msg = mav_conn.next();
            } catch (IOException e) {
                Log.d("chobits", e.getMessage());
                break;
            }
            if (msg == null) break;
            if (msg instanceof Mavlink2Message) {
                Log.d("chobits", "mavlink 2 msg");
            }
            Object msg_payload = msg.getPayload();
            if (msg_payload instanceof Heartbeat) {
                // This is a heartbeat message
                Heartbeat hb = (Heartbeat)msg_payload;
                Log.d("chobits", "heartbeat " + msg.getOriginSystemId() + "," + hb.customMode() + "," + msg.getSequence());
                Message ui_msg = ui_handler.obtainMessage(1, flight_mode[(int)hb.customMode()]);
                ui_handler.sendMessage(ui_msg);

                if (last_hb_ts == 0) {
                    ui_msg = ui_handler.obtainMessage(2, "vehicle " + msg.getOriginSystemId() + " connected " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
                    ui_handler.sendMessage(ui_msg);
                }
                last_hb_ts = SystemClock.elapsedRealtime();

                //send hb to fc, keeping link active, otherwise we will not rcv status_txt
                try {
                    mav_conn.send1(255,0,Heartbeat.builder().type(MavType.MAV_TYPE_GCS).autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID).mavlinkVersion(3).build());
                } catch (IOException e) {
                    Log.d("chobits", e.getMessage());
                }

                long ts = SystemClock.elapsedRealtime();
                if (ts - last_sys_status_ts > 3000) {
                    try {
                        mav_conn.send1(255, 0, CommandLong.builder().command(MavCmd.MAV_CMD_SET_MESSAGE_INTERVAL).param1(1).param2(1000000).build());
                    } catch (IOException e) {
                        Log.d("chobits", e.getMessage());
                    }
                }
            } else if (msg_payload instanceof Statustext) {
                Statustext txt = (Statustext)msg_payload;
                Log.d("chobits", msg.getOriginSystemId() + "," + txt.text());
                Message ui_msg = ui_handler.obtainMessage(2, txt.text());
                //Bundle data = new Bundle();
                //data.putString("message", txt.text());
                //ui_msg.setData(data);
                ui_handler.sendMessage(ui_msg);
            } else if (msg_payload instanceof SysStatus) {
                SysStatus status = (SysStatus)msg_payload;
                Message ui_msg = ui_handler.obtainMessage(3, status.voltageBattery(), status.currentBattery());
                ui_handler.sendMessage(ui_msg);
            } else {
                //Log.d("chobits", msg.getPayload().getClass().getSimpleName());
            }
        }
    }
}
