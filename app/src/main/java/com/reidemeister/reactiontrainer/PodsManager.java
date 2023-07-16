package com.reidemeister.reactiontrainer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

class Pod extends BluetoothGattCallback {
    private BluetoothDevice device;
    private Context context;

    @SuppressLint("MissingPermission")
    public Pod(BluetoothDevice device, Context context) {
        this.device = device;
        this.context = context;
        device.connectGatt(context, true, this);
    }

    // Handlers for BluetoothGatt and LeScan events.
    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!gatt.discoverServices()) {
                    // Error starting service discovery.
                    connectFailure();
                }
            }
            else {
                // Error connecting to device.
                connectFailure();
            }
        }
        else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // Disconnected, notify callbacks of disconnection.
            rx = null;
            tx = null;
            notifyOnDisconnected(this);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        // Notify connection failure if service discovery failed.
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectFailure();
            return;
        }
        gatt.getServices()

        // Save reference to each UART characteristic.
        tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
        rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

        // Save reference to each DIS characteristic.
        disManuf = gatt.getService(DIS_UUID).getCharacteristic(DIS_MANUF_UUID);
        disModel = gatt.getService(DIS_UUID).getCharacteristic(DIS_MODEL_UUID);
        disHWRev = gatt.getService(DIS_UUID).getCharacteristic(DIS_HWREV_UUID);
        disSWRev = gatt.getService(DIS_UUID).getCharacteristic(DIS_SWREV_UUID);

        // Add device information characteristics to the read queue
        // These need to be queued because we have to wait for the response to the first
        // read request before a second one can be processed (which makes you wonder why they
        // implemented this with async logic to begin with???)
        readQueue.offer(disManuf);
        readQueue.offer(disModel);
        readQueue.offer(disHWRev);
        readQueue.offer(disSWRev);

        // Request a dummy read to get the device information queue going
        gatt.readCharacteristic(disManuf);

        // Setup notifications on RX characteristic changes (i.e. data received).
        // First call setCharacteristicNotification to enable notification.
        if (!gatt.setCharacteristicNotification(rx, true)) {
            // Stop if the characteristic notification setup failed.
            connectFailure();
            return;
        }
        // Next update the RX characteristic's client descriptor to enable notifications.
        BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
        if (desc == null) {
            // Stop if the RX characteristic has no client descriptor.
            connectFailure();
            return;
        }
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(desc)) {
            // Stop if the client descriptor could not be written.
            connectFailure();
            return;
        }
        // Notify of connection completion.
        notifyOnConnected(this);
    }

}

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
