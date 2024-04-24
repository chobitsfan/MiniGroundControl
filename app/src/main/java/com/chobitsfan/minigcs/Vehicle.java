package com.chobitsfan.minigcs;

import io.dronefleet.mavlink.common.MavAutopilot;
import io.dronefleet.mavlink.common.MavType;

public abstract class Vehicle {
    public abstract String Mode(int customMode);
    public abstract Object Land();
    public abstract Object Guided();
    public abstract Object Auto();
    public abstract Object Stabilize();
    public abstract String Name();
    public static Vehicle getInstance(MavAutopilot autopilot, MavType type) {return ArduCopter.getInstance();}
}
