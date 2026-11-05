package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavAutopilot;
import io.dronefleet.mavlink.common.MavType;

public abstract class Vehicle {
    int sysId = 1;
    public abstract String Mode(int customMode);
    public abstract Object Land();
    public abstract Object RTL();
    public abstract String Name();
    public static Vehicle getInstance(MavAutopilot autopilot, MavType type) {
        if (autopilot == MavAutopilot.MAV_AUTOPILOT_PX4) {
            return PX4.getInstance();
        } else if (type == MavType.MAV_TYPE_FIXED_WING) {
            return ArduPlane.getInstance();
        } else {
            return ArduCopter.getInstance();
        }
    }
}
