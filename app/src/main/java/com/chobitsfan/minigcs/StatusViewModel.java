package com.chobitsfan.minigcs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.dronefleet.mavlink.common.GlobalPositionInt;
import io.dronefleet.mavlink.common.GpsRawInt;
import io.dronefleet.mavlink.common.Heartbeat;

public class StatusViewModel extends ViewModel {
    Vehicle vehicle = ArduCopter.getInstance();
    MutableLiveData<String> curMode = new MutableLiveData<>();
    MutableLiveData<String> dstMode = new MutableLiveData<>();
    MutableLiveData<String> statusTxt = new MutableLiveData<>();
    MutableLiveData<GlobalPositionInt> globalPos = new MutableLiveData<>();
    MutableLiveData<GpsRawInt> gpsStatus = new MutableLiveData<>();
    public void setHeartbeat(Heartbeat hb) {
        vehicle = Vehicle.getInstance(hb.autopilot().entry(), hb.type().entry());
        curMode.setValue(vehicle.Mode((int)hb.customMode()));
    }
    public LiveData<String> getCurMode() {
        return curMode;
    }
    public String[] getModes() {
        return vehicle.Modes();
    }
    public void setDstMode(String mode) {
        dstMode.setValue(mode);
    }
    public LiveData<String> getDstMode() {
        return dstMode;
    }
    public void setGlobalPos(GlobalPositionInt pos) { globalPos.setValue(pos); }
    public LiveData<GlobalPositionInt> getGlobalPos() {
        return globalPos;
    }
    public void setStatusTxt(String txt) {
        statusTxt.setValue(txt);
    }
    public LiveData<String> getStatusTxt() {
        return statusTxt;
    }
    public void setGpsStatus(GpsRawInt val) { gpsStatus.setValue(val); }
    public LiveData<GpsRawInt> getGpsStatus() { return gpsStatus; }
}