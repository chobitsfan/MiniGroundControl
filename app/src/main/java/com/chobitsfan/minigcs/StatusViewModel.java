package com.chobitsfan.minigcs;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.dronefleet.mavlink.common.GlobalPositionInt;
import io.dronefleet.mavlink.common.GpsRawInt;
import io.dronefleet.mavlink.common.Heartbeat;
import io.dronefleet.mavlink.common.SysStatus;

public class StatusViewModel extends ViewModel {
    Vehicle vehicle = ArduCopter.getInstance();
    MutableLiveData<String> curMode = new MutableLiveData<>();
    MutableLiveData<String> dstMode = new MutableLiveData<>();
    MutableLiveData<String> statusTxt = new MutableLiveData<>();
    MutableLiveData<GlobalPositionInt> globalPos = new MutableLiveData<>();
    MutableLiveData<GpsRawInt> gpsStatus = new MutableLiveData<>();
    MutableLiveData<Float> batVol = new MutableLiveData<>();
    MutableLiveData<String> paramRead = new MutableLiveData<>();
    MutableLiveData<Pair<String,Float>> paramValue = new MutableLiveData<Pair<String, Float>>();
    MutableLiveData<Pair<String,Float>> paramWrite = new MutableLiveData<Pair<String, Float>>();
    public void setParamWrite(String name, float val) {
        paramWrite.setValue(new Pair<>(name, val));
    }
    public LiveData<Pair<String, Float>> getParamWrite() {
        return paramWrite;
    }
    public void setParamValue(String name, float val) {
        paramValue.setValue(new Pair<>(name, val));
    }
    public LiveData<Pair<String, Float>> getParamValue() {
        return paramValue;
    }
    public void setParamRead(String name) {
        paramRead.setValue(name);
    }
    public LiveData<String> getParamRead() {
        return paramRead;
    }
    public void setSysStatus(SysStatus status) {
        batVol.setValue(status.voltageBattery()*0.001f);
    }
    public LiveData<Float> getBatVol() {
        return batVol;
    }
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