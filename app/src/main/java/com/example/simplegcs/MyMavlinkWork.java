package com.example.simplegcs;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.Heartbeat;
import io.dronefleet.mavlink.common.Statustext;
import io.dronefleet.mavlink.protocol.MavlinkPacket;
import io.dronefleet.mavlink.protocol.MavlinkPacketReader;

public class MyMavlinkWork implements Runnable {
    MavlinkConnection mav_conn;
    //MavlinkPacketReader reader;
    public MyMavlinkWork(InputStream is, OutputStream os) {
        mav_conn = MavlinkConnection.create(is, os);
        //reader = new MavlinkPacketReader(is);
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
            if (msg.getPayload() instanceof Heartbeat) {
                // This is a heartbeat message
                Heartbeat hb = (Heartbeat)msg.getPayload();
                Log.d("chobits", "heartbeat " + hb.customMode() + "," + msg.getSequence());
            } else if (msg.getPayload() instanceof Statustext) {
                Statustext txt = (Statustext)msg.getPayload();
                Log.d("chobits", txt.text());
            }
        }
    }
}
