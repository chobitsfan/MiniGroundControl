package com.example.simplegcs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.dronefleet.mavlink.Mavlink2Message;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.Heartbeat;
import io.dronefleet.mavlink.common.MavAutopilot;
import io.dronefleet.mavlink.common.MavState;
import io.dronefleet.mavlink.common.MavType;
import io.dronefleet.mavlink.common.Statustext;
import io.dronefleet.mavlink.protocol.MavlinkPacket;
import io.dronefleet.mavlink.protocol.MavlinkPacketReader;

public class MyMavlinkWork implements Runnable {
    MavlinkConnection mav_conn;
    Handler ui_handler;
    static String[] flight_mode = {"STABILIZE", "ACRO", "ALT_HOLD", "AUTO", "GUIDED", "LOITER", "RTL", "CIRCLE", "POSITION", "LAND"};
    //MavlinkPacketReader reader;
    public MyMavlinkWork(Handler handler, InputStream is, OutputStream os) {
        mav_conn = MavlinkConnection.create(is, os);
        //reader = new MavlinkPacketReader(is);
        ui_handler = handler;
    }

    @Override
    public void run() {
        /*MavlinkPacket packet = null;
        while (true) {
            try {
                packet = reader.next();
            } catch (IOException e) {
                Log.d("chobits", e.getMessage());
            }
            if (packet == null) break;
            Log.d("chobits", "mavlink msg " + packet.getMessageId());
        }*/
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
            if (msg.getPayload() instanceof Heartbeat) {
                // This is a heartbeat message
                Heartbeat hb = (Heartbeat)msg.getPayload();
                Log.d("chobits", "heartbeat " + msg.getOriginSystemId() + "," + hb.customMode() + "," + msg.getSequence());
                Message ui_msg = ui_handler.obtainMessage(1, flight_mode[(int)hb.customMode()]);
                ui_handler.sendMessage(ui_msg);
                try {
                    mav_conn.send1(255,0,Heartbeat.builder().type(MavType.MAV_TYPE_GCS).autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID).mavlinkVersion(3).build());
                } catch (IOException e) {
                    Log.d("chobits", e.getMessage());
                }
            } else if (msg.getPayload() instanceof Statustext) {
                Statustext txt = (Statustext)msg.getPayload();
                Log.d("chobits", msg.getOriginSystemId() + "," + txt.text());
                Message ui_msg = ui_handler.obtainMessage(2, txt.text());
                //Bundle data = new Bundle();
                //data.putString("message", txt.text());
                //ui_msg.setData(data);
                ui_handler.sendMessage(ui_msg);
            } else {
                Log.d("chobits", msg.getPayload().getClass().getSimpleName());
            }
        }
    }
}
