package com.reidemeister.reactiontrainer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;


public class MainActivity extends AppCompatActivity {
    private static PodsManager podsManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(podsManager == null) {
            podsManager = new PodsManager(this, getApplicationContext());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
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
        Log.d("ReactionTrainer", "After Permissions before finding Pods " + requested_permission);
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
    protected void onStart() {
        super.onStart();
        podsManager.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        podsManager.onStop();
    }
}