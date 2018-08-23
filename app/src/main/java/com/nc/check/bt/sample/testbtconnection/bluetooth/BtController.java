package com.nc.check.bt.sample.testbtconnection.bluetooth;

import android.util.Pair;

import java.util.List;

public interface BtController {
    List<Pair<String, String>> getDevicesAddresses();

    void start();

    void connect();

    void connect(String deviceAddress);

    void stop();

    /* Call this from the main activity to send data to the remote device */
    void write(String message);

    void setSelectedMacAddress(String selectedMacAddress);
}
