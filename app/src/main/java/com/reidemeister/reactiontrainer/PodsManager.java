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
    private MainActivity parent;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private Map<String, BluetoothDevice> knownDevices = new HashMap<String, BluetoothDevice>();

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
            if (name.startsWith("Reaction Trainer")) {
                if (knownDevices.containsKey(address)) {
                    // FIXME: Update
                } else {
                    knownDevices.put(address, device);
                    device.connectGatt(context, true, this);
                    Log.d("PodsManager", "added (" + address + ", " + name + ") = " + result.toString());
                }
            }
        }
    };

    public PodsManager(MainActivity owner, Context context) {
        this.parent = owner;
        this.context = context;
        Log.d("PodsManager", "Created");
    }

    public void onHasPermission() {
        hasPermission = true;
        onStart();
        Log.d("PodsManager", "onHasPermission");
    }

    public void onStart() {
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
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(scanCallback);
        scanStarted = true;
        Log.d("PodsManager", "findPods");
    }
}
