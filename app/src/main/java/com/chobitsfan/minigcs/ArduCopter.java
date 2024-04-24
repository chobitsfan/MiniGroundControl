package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.SetMode;

public class ArduCopter extends Vehicle {
    String[] FLIGHT_MODE = {"STABILIZE", "ACRO", "ALT_HOLD", "AUTO", "GUIDED", "LOITER", "RTL", "CIRCLE", "POSITION", "LAND", "OF_LOITER", "DRIFT", "", "SPORT", "FLIP", "AUTOTUNE", "POSHOLD", "BRAKE", "THROW", "AVOID_ADSB", "GUIDED_NOGPS", "SMART_RTL"};
    static ArduCopter instance = new ArduCopter();

    public static ArduCopter getInstance() {
        return instance;
    }

    @Override
    public String Mode(int customMode) {
        if (customMode >= FLIGHT_MODE.length) return "Mode " + customMode; else return FLIGHT_MODE[customMode];
    }

    @Override
    public Object Land() {
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(9).build();
    }

    public Object Guided(){
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(4).build();
    }

    public Object Auto(){
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(3).build();
    }

    public Object Stabilize(){
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(0).build();
    }

    @Override
    public String Name() {
        return "ArduCopter";
    }
}
