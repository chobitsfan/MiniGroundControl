package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.SetMode;

public class PX4 extends Vehicle {
    // custom_mode layout: bits 16-23 main mode, bits 24-31 sub mode
    static final int MAIN_MODE_POSCTL = 3;
    static final int MAIN_MODE_AUTO = 4;
    static final int AUTO_SUB_MODE_RTL = 5;
    static final int AUTO_SUB_MODE_LAND = 6;
    String[] MAIN_MODE = {"", "MANUAL", "ALTCTL", "POSCTL", "AUTO", "ACRO", "OFFBOARD", "STABILIZED", "RATTITUDE", "SIMPLE", "TERMINATION"};
    String[] AUTO_SUB_MODE = {"AUTO", "READY", "TAKEOFF", "LOITER", "MISSION", "RTL", "LAND", "RTGS", "FOLLOW_TARGET", "PRECLAND", "VTOL_TAKEOFF"};
    String[] POSCTL_SUB_MODE = {"POSCTL", "ORBIT", "SLOW"};
    static PX4 instance = new PX4();

    public static PX4 getInstance() {
        return instance;
    }

    static int customMode(int mainMode, int subMode) {
        return (mainMode << 16) | (subMode << 24);
    }

    @Override
    public String Mode(int customMode) {
        int main = (customMode >> 16) & 0xff;
        int sub = (customMode >> 24) & 0xff;
        if (main == MAIN_MODE_AUTO && sub < AUTO_SUB_MODE.length) return AUTO_SUB_MODE[sub];
        if (main == MAIN_MODE_POSCTL && sub < POSCTL_SUB_MODE.length) return POSCTL_SUB_MODE[sub];
        if (main > 0 && main < MAIN_MODE.length) return MAIN_MODE[main];
        return "Mode " + main + "." + sub;
    }

    @Override
    public Object Land() {
        return SetMode.builder().targetSystem(sysId).baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(customMode(MAIN_MODE_AUTO, AUTO_SUB_MODE_LAND)).build();
    }

    @Override
    public Object RTL() {
        return SetMode.builder().targetSystem(sysId).baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(customMode(MAIN_MODE_AUTO, AUTO_SUB_MODE_RTL)).build();
    }

    @Override
    public String Name() {
        return "PX4";
    }
}
