package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.SetMode;

public class ArduCopter extends Vehicle {
    String[] FLIGHT_MODE = {"STABILIZE", "ACRO", "ALT_HOLD", "AUTO", "GUIDED", "LOITER", "RTL", "CIRCLE", "POSITION", "LAND", "OF_LOITER", "DRIFT", "", "SPORT", "FLIP", "AUTOTUNE", "POSHOLD", "BRAKE", "THROW", "AVOID_ADSB", "GUIDED_NOGPS", "SMART_RTL"};

    @Override
    public String Mode(int customMode) {
        if (customMode >= FLIGHT_MODE.length) return "Mode " + customMode; else return FLIGHT_MODE[customMode];
    }

    @Override
    public Object Land() {
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(9).build();
    }

    @Override
    public Object RTL() {
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(6).build();
    }

    @Override
    public String Name() {
        return "ArduCopter";
    }
}
