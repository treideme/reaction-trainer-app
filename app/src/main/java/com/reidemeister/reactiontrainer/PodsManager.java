/**
 * @file PodsManager.java
 * @brief Handler for Managing Multiple Pods.
 * @version 1.0
 * @author Thomas Reidemeister <treideme@gmail.com>
 * @copyright 2023 Thomas Reidemeister
 * @license Apache-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.reidemeister.reactiontrainer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class PodsManager {
    private final Context context;
    private final Pod.Callback callback;
    private BluetoothLeScanner bluetoothLeScanner;

    private final Map<String, Pod> knownDevices = new HashMap<>();

    private boolean hasPermission = false;
    private boolean scanStarted = false;

    // Device scan callback.
    @SuppressLint("MissingPermission")
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address = device.getAddress();
            String name = device.getName();
            if (name != null && name.startsWith("Reaction Trainer")) {
                if (!knownDevices.containsKey(address)) {
                    Pod p = new Pod(context, device);
                    p.registerCallback(callback);
                    knownDevices.put(address, p);
                    Log.d("PodsManager", "added (" + address + ", " + name + ") = " + result.toString());
                }
            }
        }
    };

    public PodsManager(Context context, Pod.Callback callback) {
        this.context = context;
        this.callback = callback;
        Log.d("PodsManager", "Created");
    }

    public void onHasPermission() {
        hasPermission = true;
        onResume();
        Log.d("PodsManager", "onHasPermission");
    }

    public void onResume() {
        if(!scanStarted && hasPermission) {
            findPods();
        }
        Log.d("PodsManager", "onStart");
    }

    @SuppressLint("MissingPermission")
    public void onStop() {
        if(scanStarted) {
            bluetoothLeScanner.stopScan(scanCallback);
            scanStarted = false;
        }
        Log.d("PodsManager", "onStop");
    }

    @SuppressLint("MissingPermission")
    private void findPods() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(scanCallback);
        scanStarted = true;
        Log.d("PodsManager", "findPods");
    }
}
