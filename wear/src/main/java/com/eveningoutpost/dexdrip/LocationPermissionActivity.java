package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import android.support.wearable.activity.WearableActivity;
//import android.support.v4.os.ResultReceiver;
import android.util.Log;

import android.view.View;

import com.eveningoutpost.dexdrip.models.JoH;

/**
 * Simple Activity for displaying Permission Rationale to user.
 */
public class LocationPermissionActivity extends WearableActivity {//KS

    private static final String TAG = LocationPermissionActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);
        JoH.vibrateNotice();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        // On 23+ (M+) devices, GPS permission not granted. Request permission.
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT
        }, PERMISSION_REQUEST_FINE_LOCATION);
    }

    /*
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult()");

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(TAG, "onRequestPermissionsResult() granted");
                finish();
            }
        }

        if (requestCode == PERMISSION_REQUEST_BLUETOOTH_CONNECT) {
            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(TAG, "BLUETOOTH_CONNECT granted");
                finish();
            }
        }
    }
}