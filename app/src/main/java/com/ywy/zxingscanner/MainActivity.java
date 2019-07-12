package com.ywy.zxingscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }

    public void dumpScannerActivity(View view) {
        if (hasPermission(Manifest.permission.CAMERA, 1)) {
            Intent intent = new Intent(this, ScannerActivity.class);
            startActivity(intent);
        }
    }

    public void dumpScannerFragment(View view) {
        if (hasPermission(Manifest.permission.CAMERA, 2)) {
            Intent intent = new Intent(this, ScannerFragmentActivity.class);
            startActivity(intent);
        }
    }


    private boolean hasPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                Intent intent = new Intent(this, ScannerActivity.class);
                startActivity(intent);
                break;
            case 2:
                break;
            default:
                break;
        }
    }
}
