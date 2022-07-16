/**
 * @file MainActivity.java
 * @brief Main UI Handlers.
 * @version 1.0
 * @author Thomas Reidemeister <treideme@gmail.com>
 * @copyright 2023 Thomas Reidemeister
 * @license Apache-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.reidemeister.reactiontrainer;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import com.reidemeister.reactiontrainer.Pod;


public class MainActivity extends AppCompatActivity implements Pod.Callback {
    private Pod pod;
    private boolean hasPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ReactionTrainerLog", "onCreate");
//        if(podsManager == null) {
//            podsManager = new PodsManager(this, getApplicationContext());
//        }
        pod = new Pod(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
    }

    protected void onResume() {
        super.onResume();
        Log.d("ReactionTrainerLog", "onResume");
        if(hasPermission) {
            pod.registerCallback(this);
            pod.connectFirstAvailable();
        }
    }

    private void onHasPermission() {
        Log.d("ReactionTrainerLog", "onHasPermission");
        hasPermission = true;
    }

    /**
     * Status updates to the App.
     * @param status
     */
    void changeStatus(String status) {
        TextView view = findViewById(R.id.textStatus);
        view.setText(status);
    }

    /**
     * Request the required permissions to use this app.
     */
    protected void requestPermissions() {
        final String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
        };
        boolean requested_permission = false;

        for (final String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this, permissions, 0);
                requested_permission = true;
            }
        }
        Log.d("ReactionTrainerLog", "After Permissions before finding Pods " + requested_permission);
        if (!requested_permission) {
            onHasPermission();
        } else {
            changeStatus("Insufficient Permissions");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionDenied = false;
        for(int i = 0; i < permissions.length; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                permissionDenied = true;
            }
        }
        if(permissionDenied) {
            changeStatus("Insufficient Permissions");
        } else {
            onHasPermission();
        }
    }

    protected boolean addButtons() {
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        for (int i = 1; i <= 3; i++) {
            Button newButton = new Button(this);
            newButton.setText("Button " + i);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            newButton.setLayoutParams(layoutParams);
            newButton.setOnClickListener(v -> {
                // Handle button click here
            });

            buttonContainer.addView(newButton);
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ReactionTrainerLog", "onStop");
        if(hasPermission) {
            pod.unregisterCallback(this);
            pod.disconnect();
        }
    }

    // Pod callback handlers

    @Override
    public void onConnected(Pod uart) {
        Log.d("ReactionTrainerLog", "Connected to Pod");
        uart.send("Hello World!\n");
    }

    @Override
    public void onConnectFailed(Pod uart) {
        Log.d("ReactionTrainerLog", "Failed to connect to Pod");
    }

    @Override
    public void onDisconnected(Pod uart) {
        Log.d("ReactionTrainerLog", "Disconnected from Pod");
    }

    @Override
    public void onReceive(Pod uart, BluetoothGattCharacteristic rx) {
        Log.d("ReactionTrainerLog", "Received data from Pod");
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        Log.d("ReactionTrainerLog", "Found Pod");
    }

    @Override
    public void onDeviceInfoAvailable() {
        Log.d("ReactionTrainerLog", "Pod info available");
    }
}