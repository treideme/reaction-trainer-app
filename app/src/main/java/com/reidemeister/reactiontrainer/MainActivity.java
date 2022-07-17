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
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements Pod.Callback, TextWatcher {
    private PodsManager podsManager;
    private List<Pod> knownPods;
    private List<Button> knownPodsBtns;
    private Handler statusHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ReactionTrainerLog", "onCreate");
        podsManager = new PodsManager(getApplicationContext(), this);
        knownPods = new LinkedList<>();
        knownPodsBtns = new LinkedList<>();
        setContentView(R.layout.activity_main);
        EditText txt = findViewById(R.id.editTimeout);
        txt.setText("0");
        txt.addTextChangedListener(this);
        requestPermissions();
        statusHandler=new Handler();
        statusHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handleUpdate();
                //your code
                statusHandler.postDelayed(this,100);
            }
        },100);
    }

    protected void handleUpdate() {
        for(Pod pod : knownPods) {
            pod.send("status");
        }
        Log.d("ReactionTrainerLog", "handleUpdate");
    }

    protected void onResume() {
        super.onResume();
        podsManager.onResume();
        Log.d("ReactionTrainerLog", "onResume");
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
            podsManager.onHasPermission();
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
            podsManager.onHasPermission();
        }
    }

    private void addPod(Pod pod) {
        Log.d("ReactionTrainerLog", "Adding Pod");
        for(Pod knownPod : knownPods) {
            if(knownPod == pod) {
                return;
            }
        }
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        Button newButton = new Button(this);
        newButton.setText("Pod "+knownPods.size());
        newButton.setOnClickListener(v -> {
            EditText txt = findViewById(R.id.editTimeout);
            try {
                int timeout = Integer.parseInt(String.valueOf(txt.getText()));
                pod.send("L 1 "+timeout);
            } catch (Exception e) {
                Log.d("ReactionTrainerLog", "Invalid timeout");
                return;
            }
        });

        knownPodsBtns.add(newButton);
        knownPods.add(pod);

        // Force GUI update
        runOnUiThread(() -> buttonContainer.addView(newButton));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ReactionTrainerLog", "onStop");
        podsManager.onStop();
    }

    // Pod callback handlers
    @Override
    public void onConnected(Pod pod) {
        Log.d("ReactionTrainerLog", "Connected to Pod");
        addPod(pod);
    }

    @Override
    public void onConnectFailed(Pod pod) {
        Log.d("ReactionTrainerLog", "Failed to connect to Pod");
    }

    @Override
    public void onDisconnected(Pod pod) {
        Log.d("ReactionTrainerLog", "Disconnected from Pod");
    }

    @Override
    public void onReceive(Pod pod, BluetoothGattCharacteristic rx) {
        String result = rx.getStringValue(0);
        Log.d("ReactionTrainerLog", "Received data from Pod: "+result);
        if(result.startsWith("V")) {
            String[] parts = result.split(" ");

            if (parts.length == 3 && parts[0].equals("V")) {
                try {
                    int vbat = Integer.parseInt(parts[1]);
                    int led = Integer.parseInt(parts[2]);
                    Log.d("ReactionTrainerLog", "Vbat: "+vbat+" LED: "+led);
                    for(int i = 0; i < knownPods.size(); i++) {
                        if(knownPods.get(i) == pod) {
                            Button btn = knownPodsBtns.get(i);
                            runOnUiThread(() -> {
                                btn.setText(String.format("Battery %4d mV", vbat));
                                if(led == 1) {
                                    btn.setBackgroundColor(Color.GREEN);
                                } else {
                                    btn.setBackgroundColor(Color.GRAY);
                                }
                            });
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e("ReactionTrainerLog", "Invalid number format");
                }
            } else {
                Log.e("ReactionTrainerLog", "Invalid number format");
            }
        }
    }

    @Override
    public void onDeviceInfoAvailable(Pod pod) {
        Log.d("ReactionTrainerLog", "Pod info available");
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // pass
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // pass
    }

    @Override
    public void afterTextChanged(Editable s) {
        // pass
        String input = s.toString();
        EditText editText = findViewById(R.id.editTimeout);
        if (!input.isEmpty()) {
            editText.removeTextChangedListener(this);
            int number = Integer.parseInt(input);
            editText.setText(String.valueOf(number));
            editText.setSelection(editText.getText().length());
            editText.addTextChangedListener(this);
        } else {
            editText.setText("0");
        }
    }
}