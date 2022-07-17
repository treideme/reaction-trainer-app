/**
 * @file Pod.java
 * @brief Pod management interface. This class provides methods for connecting to a BLE Peripheral.
 * @version 1.0
 * @author Thomas Reidemeister <treideme@gmail.com>
 * @copyright 2023 Thomas Reidemeister
 * @license Apache-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.reidemeister.reactiontrainer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pod management interface. This class provides methods for connecting to a BLE Peripheral.
 */
public class Pod extends BluetoothGattCallback {
    // UUIDs for UART service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9f");
    public static UUID RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9f");
    public static UUID TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9f");

    // UUIDs for the Device Information service and associated characeristics.
    public static UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_MANUF_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_HWREV_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static UUID DIS_SWREV_UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Internal UART state.
    private final WeakHashMap<Callback, Object> callbacks;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private volatile boolean writeInProgress; // Flag to indicate a write is currently in progress

    // Device Information state.
    private BluetoothGattCharacteristic descriptorManufacturer;
    private BluetoothGattCharacteristic descriptorModel;
    private BluetoothGattCharacteristic descriptorHardwareRevision;
    private BluetoothGattCharacteristic descriptorSoftwareRevision;
    private volatile boolean disAvailable;

    // Queues for characteristic read (synchronous)
    private final Queue<BluetoothGattCharacteristic> readQueue;
    private final Queue<byte[]>sendQueue;

    // Interface for a BluetoothLeUart client to be notified of UART actions.
    public interface Callback {
        void onConnected(Pod pod);
        void onConnectFailed(Pod pod);
        void onDisconnected(Pod pod);
        void onReceive(Pod pod, BluetoothGattCharacteristic rx);
        void onDeviceInfoAvailable(Pod pod);
    }

    @SuppressLint("MissingPermission")
    public Pod(Context context, BluetoothDevice device) {
        super();
        this.callbacks = new WeakHashMap<>();
        this.tx = null;
        this.rx = null;
        this.descriptorManufacturer = null;
        this.descriptorModel = null;
        this.descriptorHardwareRevision = null;
        this.descriptorSoftwareRevision = null;
        this.disAvailable = false;
        this.writeInProgress = false;
        this.readQueue = new ConcurrentLinkedQueue<>();
        this.sendQueue = new ConcurrentLinkedQueue<>();
        gatt = device.connectGatt(context, true, this);
    }

    // Return true if connected to UART device, false otherwise.
    public boolean isConnected() {
        return (tx != null && rx != null);
    }

    public String getDeviceInfo() {
        if (tx == null || !disAvailable ) {
            // Do nothing if there is no connection.
            return "";
        }
        return "Manufacturer : " + descriptorManufacturer.getStringValue(0) + "\n" +
                "Model        : " + descriptorModel.getStringValue(0) + "\n" +
                "Firmware     : " + descriptorSoftwareRevision.getStringValue(0) + "\n";
    }

    public boolean deviceInfoAvailable() { return disAvailable; }

    @SuppressLint("MissingPermission")
    private boolean kickSend() {
        Log.d("ReactionTrainerLog", "kickSend");
        if (!writeInProgress) {
            if(disAvailable) {
                writeInProgress = true; // Set the write in progress flag
                byte [] value = sendQueue.poll();
                if(value != null) {
                    tx.setValue(value);
                    if (!gatt.writeCharacteristic(tx)) {
                        Log.d("ReactionTrainerLog", "send failed for "+new String(value, StandardCharsets.UTF_8));
                        writeInProgress = false;
                        return false;
                    }
                } else {
                    writeInProgress = false;
                }
            }
        }
        return true;
    }

    // Send data to connected UART device.
    @SuppressLint("MissingPermission")
    public boolean send(byte[] data) {
        if (tx == null || data == null || data.length == 0) {
            Log.d("ReactionTrainerLog", "send failed");
            // Do nothing if there is no connection or message to send.
            return false;
        }
        sendQueue.add(data);
        Log.d("ReactionTrainerLog", "sendQueue size: " + sendQueue.size());
        return kickSend();
    }

    // Send data to connected UART device.
    public void send(String data) {
        if (data != null && !data.isEmpty()) {
            send(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    // Register the specified callback to receive UART callbacks.
    public void registerCallback(Callback callback) {
        Log.d("ReactionTrainerLog", "registerCallback");
        callbacks.put(callback, null);
    }

    // Unregister the specified callback.
    public void unregisterCallback(Callback callback) {
        Log.d("ReactionTrainerLog", "unregisterCallback");
        callbacks.remove(callback);
    }

    // Disconnect to a device if currently connected.
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
        }
        gatt = null;
        tx = null;
        rx = null;
    }

    // Handlers for BluetoothGatt and LeScan events.
    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Connected to device, start discovering services.
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

    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.d("ReactionTrainerLog", "onServicesDiscovered");
        // Notify connection failure if service discovery failed.
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectFailure();
            return;
        }

        // Save reference to each UART characteristic.
        tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
        rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

        // Save reference to each DIS characteristic.
        descriptorManufacturer = gatt.getService(DIS_UUID).getCharacteristic(DIS_MANUF_UUID);
        descriptorModel = gatt.getService(DIS_UUID).getCharacteristic(DIS_MODEL_UUID);
        descriptorHardwareRevision = gatt.getService(DIS_UUID).getCharacteristic(DIS_HWREV_UUID);
        descriptorSoftwareRevision = gatt.getService(DIS_UUID).getCharacteristic(DIS_SWREV_UUID);

        // Add device information characteristics to the read queue
        readQueue.offer(descriptorManufacturer);
        readQueue.offer(descriptorModel);
        readQueue.offer(descriptorHardwareRevision);
        readQueue.offer(descriptorSoftwareRevision);

        // Setup notifications on RX characteristic changes (i.e. data received).
        if (!gatt.setCharacteristicNotification(rx, true)) {
            // Stop if the characteristic notification setup failed.
            Log.d("ReactionTrainerLog", "setCharacteristicNotification failed");
            connectFailure();
            return;
        }
        // Next update the RX characteristic's client descriptor to enable notifications.
        BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
        if (desc == null) {
            Log.d("ReactionTrainerLog", "getDescriptor failed");
            // Stop if the RX characteristic has no client descriptor.
            connectFailure();
            return;
        }
        if(!desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            Log.d("ReactionTrainerLog", "setValue failed");
            // Stop if the RX client descriptor could not be updated.
            connectFailure();
            return;
        }
        if(!gatt.writeDescriptor(desc)) {
            Log.w("ReactionTrainerLog", "writeDescriptor failed");
            notifyOnConnectFailed(this);
            return;
        }

        // Notify of connection completion.
        notifyOnConnected(this);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        notifyOnReceive(this, characteristic);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Log.d("ReactionTrainerLog", "onCharacteristicRead");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Check if there is anything left in the queue
            BluetoothGattCharacteristic nextRequest = readQueue.poll();
            if(nextRequest != null){
                // Send a read request for the next item in the queue
                gatt.readCharacteristic(nextRequest);
            }
            else {
                // We've reached the end of the queue
                disAvailable = true;
                notifyOnDeviceInfoAvailable();
                kickSend();
            }
        }
        else {
            Log.w("ReactionTrainerLog", "Failed reading characteristic " + characteristic.getUuid().toString());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.d("ReactionTrainerLog", "onCharacteristicWrite");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("ReactionTrainerLog", "Characteristic write successful");
        }
        writeInProgress = false;
        kickSend();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.d("ReactionTrainerLog", "onDescriptorWrite"+descriptor.getUuid().toString()+" "+status);
        // Start read queue after descriptor write
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Check if there is anything left in the queue
            BluetoothGattCharacteristic nextRequest = readQueue.poll();
            if(nextRequest != null){
                // Send a read request for the next item in the queue
                gatt.readCharacteristic(nextRequest);
            }
            else {
                // We've reached the end of the queue
                disAvailable = true;
                notifyOnDeviceInfoAvailable();
            }
        }
        else {
            Log.w("ReactionTrainerLog", "Failed to set client characteristic notification for " + descriptor.getCharacteristic().getUuid().toString());
        }
    }

    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        Log.d("ReactionTrainerLog", "onDescriptorRead");
    }

    // Private functions to simplify the notification of all callbacks of a certain event.
    private void notifyOnConnected(Pod uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnected(uart);
            }
        }
    }

    private void notifyOnConnectFailed(Pod uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onConnectFailed(uart);
            }
        }
    }

    private void notifyOnDisconnected(Pod uart) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDisconnected(uart);
            }
        }
    }

    private void notifyOnReceive(Pod uart, BluetoothGattCharacteristic rx) {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null ) {
                cb.onReceive(uart, rx);
            }
        }
    }

    private void notifyOnDeviceInfoAvailable() {
        for (Callback cb : callbacks.keySet()) {
            if (cb != null) {
                cb.onDeviceInfoAvailable(this);
            }
        }
    }

    // Notify callbacks of connection failure, and reset connection state.
    private void connectFailure() {
        rx = null;
        tx = null;
        notifyOnConnectFailed(this);
    }
}