package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.SetMode;

public class PX4 extends Vehicle {
    String[] FLIGHT_MODE = {"MANUAL", "STABILIZED", "ACRO", "ALTCTL", "POSCTL", "OFFBOARD", "TAKEOFF", "HOLD", "MISSION", "RTL", "LAND", "FOLLOW_TARGET", "PRECLAND"};
    static PX4 instance = new PX4();

    public static PX4 getInstance() {
        return instance;
    }

    @Override
    public String Mode(int customMode) {
        // px4_custom_mode: bits 16-23 main_mode, bits 24-31 sub_mode
        int mainMode = (customMode >> 16) & 0xff;
        int subMode = (customMode >> 24) & 0xff;
        switch (mainMode) {
            case 1: return "MANUAL";
            case 2: return "ALTCTL";
            case 3:
                if (subMode == 1) return "ORBIT";
                return "POSCTL";
            case 4:
                switch (subMode) {
                    case 1: return "READY";
                    case 2: return "TAKEOFF";
                    case 3: return "HOLD";
                    case 4: return "MISSION";
                    case 5: return "RTL";
                    case 6: return "LAND";
                    case 8: return "FOLLOW_TARGET";
                    case 9: return "PRECLAND";
                    case 10: return "VTOL_TAKEOFF";
                    default: return "AUTO";
                }
            case 5: return "ACRO";
            case 6: return "OFFBOARD";
            case 7: return "STABILIZED";
            case 8: return "RATTITUDE";
            default: return "Mode " + customMode;
        }
    }

    @Override
    public String[] Modes() {
        return FLIGHT_MODE;
    }

    @Override
    public Object setMode(String mode) {
        int mainMode;
        int subMode = 0;
        switch (mode) {
            case "MANUAL": mainMode = 1; break;
            case "ALTCTL": mainMode = 2; break;
            case "POSCTL": mainMode = 3; break;
            case "ACRO": mainMode = 5; break;
            case "OFFBOARD": mainMode = 6; break;
            case "STABILIZED": mainMode = 7; break;
            case "TAKEOFF": mainMode = 4; subMode = 2; break;
            case "HOLD": mainMode = 4; subMode = 3; break;
            case "MISSION": mainMode = 4; subMode = 4; break;
            case "RTL": mainMode = 4; subMode = 5; break;
            case "LAND": mainMode = 4; subMode = 6; break;
            case "FOLLOW_TARGET": mainMode = 4; subMode = 8; break;
            case "PRECLAND": mainMode = 4; subMode = 9; break;
            default: return null;
        }
        // px4_custom_mode: bits 16-23 main_mode, bits 24-31 sub_mode
        int customMode = (mainMode << 16) | (subMode << 24);
        return SetMode.builder().baseMode(MavModeFlag.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED).customMode(customMode).build();
    }

    @Override
    public String Name() {
        return "PX4";
    }
}
