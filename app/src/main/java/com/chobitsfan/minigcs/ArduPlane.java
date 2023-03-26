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
    public Object Land() {
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(11).build();
    }

    @Override
    public Object RTL() {
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(11).build();
    }

    @Override
    public String Name() {
        return "ArduPlane";
    }
}
