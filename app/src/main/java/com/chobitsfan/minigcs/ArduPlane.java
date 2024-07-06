package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.SetMode;

public class ArduPlane extends Vehicle {
    String[] FLIGHT_MODE = {"MANUAL", "CIRCLE", "STABILIZE", "TRAINING", "ACRO", "FBWA", "FBWB", "CRUISE", "AUTOTUNE", "", "AUTO", "RTL", "LOITER", "TAKEOFF", "AVOID_ADSB", "GUIDED"};
    static ArduPlane instance = new ArduPlane();

    public static ArduPlane getInstance() {
        return instance;
    }

    @Override
    public String Mode(int customMode) {
        if (customMode >= FLIGHT_MODE.length) return "Mode " + customMode; else return FLIGHT_MODE[customMode];
    }

    @Override
    public String[] Modes() {
        return FLIGHT_MODE;
    }

    @Override
    public Object setMode(String mode) {
        int i;
        for (i=0;i< FLIGHT_MODE.length;i++) {
            if (mode.compareTo(FLIGHT_MODE[i]) == 0) break;
        }
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(i).build();
    }
    @Override
    public String Name() {
        return "ArduPlane";
    }
}
