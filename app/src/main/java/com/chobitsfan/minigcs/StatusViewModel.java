package com.chobitsfan.minigcs;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StatusViewModel extends ViewModel {
    MutableLiveData<String> flightMode = new MutableLiveData<>();
    MutableLiveData<String> statusTxt = new MutableLiveData<>();
    public void setFlightMode(String mode) {
        flightMode.setValue(mode);
    }
    public LiveData<String> getFlightMode() {
        return flightMode;
    }
    public void setStatusTxt(String txt) {
        statusTxt.setValue(txt);
    }

    public LiveData<String> getStatusTxt() {
        return statusTxt;
    }
}