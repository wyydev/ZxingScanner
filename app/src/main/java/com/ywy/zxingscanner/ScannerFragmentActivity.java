package com.ywy.zxingscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ScannerFragmentActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_fragment);
        setupToolbar();
    }
}
