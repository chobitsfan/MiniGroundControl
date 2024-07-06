package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.SetMode;

public class ArduCopter extends Vehicle {
    String[] FLIGHT_MODE = {"STABILIZE", "ACRO", "ALT_HOLD", "AUTO", "GUIDED", "LOITER", "RTL", "CIRCLE", "POSITION", "LAND", "", "DRIFT", "", "SPORT", "FLIP", "AUTOTUNE", "POSHOLD", "BRAKE", "THROW", "AVOID_ADSB", "GUIDED_NOGPS", "SMART_RTL"};
    static ArduCopter instance = new ArduCopter();

    public static ArduCopter getInstance() {
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
        return "ArduCopter";
    }
}
