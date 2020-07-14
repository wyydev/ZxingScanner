package com.ywy.zxingscanner;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.ywy.zxinglib.view.ZXingScannerView;

import java.util.ArrayList;
import java.util.List;

public class ScannerActivity extends BaseActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        setupToolbar();
        ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);
        mScannerView = new ZXingScannerView(this);
        List<BarcodeFormat> formatList = new ArrayList<>();
        formatList.add(BarcodeFormat.QR_CODE);
        formatList.add(BarcodeFormat.EAN_13);
        mScannerView.setFormats(formatList);
        mScannerView.setRectWidthRatio(0.8f);
//        mScannerView.setRectWidthHeightRatio(1.4f);
        mScannerView.setRectWidthHeightRatio(0.7f);
        mScannerView.setBorderColor(Color.WHITE);
        mScannerView.setMaskColor(Color.parseColor("#00000000"));
//        mScannerView.setCornerRounded(true);
//        mScannerView.setCornerRadius(20);
        mScannerView.setAutoZoom(true);
        mScannerView.setScanFullScreen(false);
        mScannerView.setAutoFocusInterval(1000);
        mScannerView.setAutoFocus(false);
        mScannerView.setCornerInRect(true);
        mScannerView.setAspectTolerance(1.2f);
        contentFrame.addView(mScannerView);
    }



    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }



    @Override
    public void handleResult(Result rawResult) {
        Toast.makeText(this, "Contents = " + rawResult.getText() +
                ", Format = " + rawResult.getBarcodeFormat().toString(), Toast.LENGTH_SHORT).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(ScannerActivity.this);
            }
        }, 2000);
    }
}
