package com.ywy.zxinglib.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.ywy.util.DisplayUtils;
import com.ywy.zxinglib.R;
import com.ywy.zxinglib.view.IViewFinder;
import com.ywy.zxinglib.view.ViewFinderView;

import static com.ywy.zxinglib.view.IViewFinder.GRAVITY_BOTTOM;

/**
 * 扫码界面
 *
 * @author ywy
 * @date 2019/7/8
 */
public abstract class BarcodeScannerView extends FrameLayout implements Camera.PreviewCallback {

    private CameraWrapper mCameraWrapper;
    private CameraPreview mPreview;
    private IViewFinder mViewFinderView;
    private Rect mFramingRectInPreview;
    private CameraHandlerThread mCameraHandlerThread;
    private Boolean mFlashState;
    private boolean mAutofocusState = true;
    private boolean mShouldScaleToFill = true;

    private float mAspectTolerance = 0.1f;
    @ColorInt
    private int mMaskColor;
    @ColorInt
    private int mScanLineColor;
    private int mScanLineHeight;
    private int mScanLineMargin;
    private Drawable mScanLineDrawable;
    private Bitmap mScanLineBitmap;
    private int mScanLineMoveDistance;
    private long mAnimTime;
    private boolean mRectSquare;
    private int mRectTopOffset;
    private int mBorderColor;
    private int mBorderStrokeWidth;
    private int mCornerColor;
    private int mCornerStrokeWidth;
    private int mCornerLineLength;
    private boolean mCornerRounded;
    private int mCornerRadius;
    private boolean mCornerInRect;
    protected boolean mAutoZoom;
    private int mAutoFocusInterval;
    protected boolean mScanFullScreen;
    private float mRectWidthRatio;
    private float mRectWidthHeightRatio;
    private float mSquareDimensionRatio;
    private String mTipText;
    private int mTextColor;
    private int mTextSize;
    private int mTextMargin;
    @IViewFinder.TextGravity
    int mTextGravity;
    private boolean mTextSingleLine;


    public BarcodeScannerView(Context context) {
        super(context);
        initDefault(context);
        init();
    }

    private void initDefault(Context context) {
        mScanLineColor = Color.WHITE;
        mScanLineHeight = DisplayUtils.dp2px(context, 2);
        mScanLineMargin = 0;
        mScanLineMoveDistance = mScanLineHeight;
        mAnimTime = 20L;
        mMaskColor = Color.parseColor("#33FFFFFF");
        mBorderColor = Color.WHITE;
        mBorderStrokeWidth = DisplayUtils.dp2px(context, 1);
        mCornerColor = Color.WHITE;
        mCornerStrokeWidth = DisplayUtils.dp2px(context, 3);
        mCornerLineLength = DisplayUtils.dp2px(context, 20);
        mScanLineDrawable = null;
        Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.icon_default_scan_line)).getBitmap();
        mScanLineBitmap = DisplayUtils.makeTintBitmap(bitmap, mScanLineColor);
        mCornerInRect = false;
        mCornerRounded = false;
        mRectSquare = false;
        mCornerRadius = DisplayUtils.dp2px(context, 2);
        mAutoFocusInterval = 1000;
        mSquareDimensionRatio = 5f / 8;
        int orientation = DisplayUtils.getScreenOrientation(getContext());
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mRectWidthRatio = 6f / 8;
            mRectWidthHeightRatio = 0.75f;
        } else {
            //横屏
            mRectWidthRatio = 5f / 8;
            mRectWidthHeightRatio = 1.4f;
        }
        mTextColor = Color.WHITE;
        mTextSize = DisplayUtils.sp2px(context, 14);
        mTipText = "将二维码/条码放入框内，即可自动扫描";
        mTextGravity = IViewFinder.GRAVITY_BOTTOM;
        mTextSingleLine = true;
        mTextMargin = DisplayUtils.dp2px(context, 5);
    }

    public BarcodeScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.BarcodeScannerView,
                0, 0);

        try {
            setShouldScaleToFill(a.getBoolean(R.styleable.BarcodeScannerView_shouldScaleToFill, true));
            mMaskColor = a.getColor(R.styleable.BarcodeScannerView_maskColor, Color.parseColor("#33ffffff"));
            mScanLineColor = a.getColor(R.styleable.BarcodeScannerView_scanLineColor, Color.WHITE);
            mScanLineHeight = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_scanLineHeight, DisplayUtils.dp2px(context, 2));
            mScanLineMargin = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_scanLineMargin, DisplayUtils.dp2px(context, 4));
            mScanLineDrawable = a.getDrawable(R.styleable.BarcodeScannerView_scanLineDrawable);
            if (mScanLineDrawable == null) {
                Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.icon_default_scan_line)).getBitmap();
                mScanLineBitmap = DisplayUtils.makeTintBitmap(bitmap, mScanLineColor);
            }
            int defaultMoveDistance = mScanLineDrawable == null ? mScanLineHeight : mScanLineDrawable.getIntrinsicHeight();
            mScanLineMoveDistance = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_scanLineMoveDistance, defaultMoveDistance);
            mAnimTime = a.getInteger(R.styleable.BarcodeScannerView_animTime, 20);
            float defaultSquareDimensionRatio = 5f / 8;
            float defaultRectWidthRatio;
            float defaultRectWidthHeightRatio;
            int orientation = DisplayUtils.getScreenOrientation(getContext());
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                //竖屏
                defaultRectWidthRatio = 6f / 8;
                defaultRectWidthHeightRatio = 0.75f;
            } else {
                //横屏
                defaultRectWidthRatio = 5f / 8;
                defaultRectWidthHeightRatio = 1.4f;
            }
            mRectWidthRatio = a.getFloat(R.styleable.BarcodeScannerView_rectWidthRatio, defaultRectWidthRatio);
            mRectWidthHeightRatio = a.getFloat(R.styleable.BarcodeScannerView_rectWidthHeightRatio, defaultRectWidthHeightRatio);
            mSquareDimensionRatio = a.getFloat(R.styleable.BarcodeScannerView_squareDimensionRatio, defaultSquareDimensionRatio);
            mRectSquare = a.getBoolean(R.styleable.BarcodeScannerView_rectSquare, false);
            mRectTopOffset = a.getDimensionPixelOffset(R.styleable.BarcodeScannerView_rectTopOffset, 0);
            mBorderColor = a.getColor(R.styleable.BarcodeScannerView_borderColor, Color.WHITE);
            mBorderStrokeWidth = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_borderStrokeWidth, DisplayUtils.dp2px(context, 1));
            mCornerColor = a.getColor(R.styleable.BarcodeScannerView_cornerColor, Color.WHITE);
            mCornerStrokeWidth = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_cornerStrokeWidth, DisplayUtils.dp2px(context, 3));
            mCornerLineLength = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_cornerLineLength, DisplayUtils.dp2px(context, 20));
            mCornerRounded = a.getBoolean(R.styleable.BarcodeScannerView_cornerRounded, false);
            mCornerRadius = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_cornerRadius, 0);
            mCornerInRect = a.getBoolean(R.styleable.BarcodeScannerView_cornerInRect, false);
            mAutoZoom = a.getBoolean(R.styleable.BarcodeScannerView_autoZoom, true);
            mAutoFocusInterval = a.getInteger(R.styleable.BarcodeScannerView_autoFocusInterval, 1000);
            mScanFullScreen = a.getBoolean(R.styleable.BarcodeScannerView_scanFullScreen, false);
            mTipText = a.getString(R.styleable.BarcodeScannerView_tipText);
            if (TextUtils.isEmpty(mTipText)){
                mTipText = "将二维码/条码放入框内，即可自动扫描";
            }
            mTextColor = a.getColor(R.styleable.BarcodeScannerView_tipTextColor, Color.WHITE);
            mTextSize = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_tipTextSize, DisplayUtils.sp2px(context, 14));
            mTextMargin = a.getDimensionPixelSize(R.styleable.BarcodeScannerView_tipTextMargin, DisplayUtils.dp2px(context, 5));
            mTextGravity = a.getInt(R.styleable.BarcodeScannerView_tipTextGravity, GRAVITY_BOTTOM);
            mTextSingleLine = a.getBoolean(R.styleable.BarcodeScannerView_tipTextSingleLine, true);
        } finally {
            a.recycle();
        }
        init();
    }

    private void init() {
        mViewFinderView = createViewFinderView(getContext());
    }

    public final void setupLayout(CameraWrapper cameraWrapper) {
        removeAllViews();

        mPreview = new CameraPreview(getContext(), cameraWrapper, this);
        mPreview.setAspectTolerance(mAspectTolerance);
        mPreview.setShouldScaleToFill(mShouldScaleToFill);
        mPreview.setAutoFocusInterval(mAutoFocusInterval);
        if (!mShouldScaleToFill) {
            RelativeLayout relativeLayout = new RelativeLayout(getContext());
            relativeLayout.setGravity(Gravity.CENTER);
            relativeLayout.setBackgroundColor(Color.BLACK);
            relativeLayout.addView(mPreview);
            addView(relativeLayout);
        } else {
            addView(mPreview);
        }

        if (mViewFinderView instanceof View) {
            addView((View) mViewFinderView);
        } else {
            throw new IllegalArgumentException("IViewFinder object returned by " +
                    "'createViewFinderView()' should be instance of android.view.View");
        }
    }


    protected IViewFinder createViewFinderView(Context context) {
        ViewFinderView viewFinderView = new ViewFinderView(context);
        viewFinderView.setMaskColor(mMaskColor);
        viewFinderView.setScanLineColor(mScanLineColor);
        viewFinderView.setScanLineHeight(mScanLineHeight);
        viewFinderView.setScanLineMargin(mScanLineMargin);
        viewFinderView.setScanLineDrawable(mScanLineDrawable);
        viewFinderView.setScanLineBitmap(mScanLineBitmap);
        viewFinderView.setScanLineMoveDistance(mScanLineMoveDistance);
        viewFinderView.setAnimTime(mAnimTime);
        viewFinderView.setRectWidthRatio(mRectWidthRatio);
        viewFinderView.setRectWidthHeightRatio(mRectWidthHeightRatio);
        viewFinderView.setRectSquare(mRectSquare);
        viewFinderView.setSquareDimensionRatio(mSquareDimensionRatio);
        viewFinderView.setRectTopOffset(mRectTopOffset);
        viewFinderView.setBorderColor(mBorderColor);
        viewFinderView.setBorderStrikeWidth(mBorderStrokeWidth);
        viewFinderView.setCornerColor(mCornerColor);
        viewFinderView.setCornerStrokeWidth(mCornerStrokeWidth);
        viewFinderView.setCornerLineLength(mCornerLineLength);
        viewFinderView.setCornerRounded(mCornerRounded);
        viewFinderView.setCornerRadius(mCornerRadius);
        viewFinderView.setCornerInRect(mCornerInRect);
        viewFinderView.setTipText(mTipText);
        viewFinderView.setTextColor(mTextColor);
        viewFinderView.setTextSize(mTextSize);
        viewFinderView.setTextGravity(mTextGravity);
        viewFinderView.setTextMargin(mTextMargin);
        viewFinderView.setTextSingleLine(mTextSingleLine);
        return viewFinderView;
    }


    public void setScanLineColor(int scanLineColor) {
        mScanLineColor = scanLineColor;
        mViewFinderView.setScanLineColor(mScanLineColor);
        mViewFinderView.setupViewFinder();
    }

    public void setScanLineHeight(int scanLineHeight) {
        mScanLineHeight = scanLineHeight;
        mViewFinderView.setScanLineHeight(mScanLineHeight);
        mViewFinderView.setupViewFinder();
    }

    public void setScanLineMargin(int scanLineMargin) {
        mScanLineMargin = scanLineMargin;
        mViewFinderView.setScanLineMargin(mScanLineMargin);
        mViewFinderView.setupViewFinder();
    }

    public void setScanLineMoveDistance(int scanLineMoveDistance) {
        mScanLineMoveDistance = scanLineMoveDistance;
        mViewFinderView.setScanLineMoveDistance(mScanLineMoveDistance);
        mViewFinderView.setupViewFinder();
    }

    public void setScanLineDrawable(Drawable scanLineDrawable) {
        mScanLineDrawable = scanLineDrawable;
        mViewFinderView.setScanLineDrawable(mScanLineDrawable);
        mViewFinderView.setupViewFinder();
    }

    public void setScanLineBitmap(Bitmap bitmap) {
        mViewFinderView.setScanLineBitmap(bitmap);
        mViewFinderView.setupViewFinder();
    }

    public void setAnimTime(long animTime) {
        mAnimTime = animTime;
        mViewFinderView.setAnimTime(mAnimTime);
        mViewFinderView.setupViewFinder();
    }

    public void setMaskColor(int maskColor) {
        mMaskColor = maskColor;
        mViewFinderView.setMaskColor(mMaskColor);
        mViewFinderView.setupViewFinder();
    }


    public void setRectWidthRatio(@FloatRange(from = 0.0, to = 1.0) float rectWidthRatio) {
        mRectWidthRatio = rectWidthRatio;
        mViewFinderView.setRectWidthRatio(rectWidthRatio);
        mViewFinderView.setupViewFinder();
    }

    public void setRectWidthHeightRatio(float rectWidthHeightRatio) {
        mRectWidthHeightRatio = rectWidthHeightRatio;
        mViewFinderView.setRectWidthHeightRatio(mRectWidthHeightRatio);
        mViewFinderView.setupViewFinder();
    }


    public void setSquareDimensionRatio(float squareDimensionRatio) {
        mSquareDimensionRatio = squareDimensionRatio;
        mViewFinderView.setSquareDimensionRatio(mSquareDimensionRatio);
        mViewFinderView.setupViewFinder();
    }

    public void setRectSquare(boolean square) {
        mRectSquare = square;
        mViewFinderView.setRectSquare(mRectSquare);
        mViewFinderView.setupViewFinder();
    }


    public void setRectTopOffset(int rectTopOffset) {
        mRectTopOffset = rectTopOffset;
        mViewFinderView.setRectTopOffset(mRectTopOffset);
        mViewFinderView.setupViewFinder();
    }

    public void setBorderColor(int borderColor) {
        mBorderColor = borderColor;
        mViewFinderView.setBorderColor(mBorderColor);
        mViewFinderView.setupViewFinder();
    }

    public void setBorderStrikeWidth(int borderStrikeWidth) {
        mBorderStrokeWidth = borderStrikeWidth;
        mViewFinderView.setBorderStrikeWidth(mBorderStrokeWidth);
        mViewFinderView.setupViewFinder();
    }

    public void setCornerColor(int cornerColor) {
        mCornerColor = cornerColor;
        mViewFinderView.setCornerColor(mCornerColor);
        mViewFinderView.setupViewFinder();
    }

    public void setCornerStrokeWidth(int cornerStrokeWidth) {
        mCornerStrokeWidth = cornerStrokeWidth;
        mViewFinderView.setCornerStrokeWidth(mCornerStrokeWidth);
        mViewFinderView.setupViewFinder();
    }

    public void setCornerLineLength(int cornerLineLength) {
        mCornerLineLength = cornerLineLength;
        mViewFinderView.setCornerLineLength(mCornerLineLength);
        mViewFinderView.setupViewFinder();
    }

    public void setCornerRounded(boolean cornerRounded) {
        mCornerRounded = cornerRounded;
        mViewFinderView.setCornerRounded(mCornerRounded);
        mViewFinderView.setupViewFinder();
    }

    public void setCornerRadius(int cornerRadius) {
        mCornerRadius = cornerRadius;
        mViewFinderView.setCornerRadius(mCornerRadius);
        mViewFinderView.setupViewFinder();
    }

    public void setCornerInRect(boolean inRect) {
        mCornerInRect = inRect;
        mViewFinderView.setCornerInRect(mCornerInRect);
        mViewFinderView.setupViewFinder();
    }

    public void setScanFullScreen(boolean scanFullScreen) {
        mScanFullScreen = scanFullScreen;
    }


    public void setTipText(String tipText) {
        this.mTipText = tipText;
        mViewFinderView.setTipText(mTipText);
        mViewFinderView.setupViewFinder();
    }

    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mViewFinderView.setTextColor(mTextColor);
        mViewFinderView.setupViewFinder();
    }

    public void setTextSize(int textSize) {
        this.mTextSize = textSize;
        mViewFinderView.setTextSize(mTextSize);
        mViewFinderView.setupViewFinder();
    }

    public void setTextMargin(int textMargin) {
        this.mTextMargin = textMargin;
        mViewFinderView.setTextMargin(mTextMargin);
        mViewFinderView.setupViewFinder();
    }

    public void setTextGravity(@IViewFinder.TextGravity int textGravity) {
        this.mTextGravity = textGravity;
        mViewFinderView.setTextGravity(mTextGravity);
        mViewFinderView.setupViewFinder();
    }

    public void setTextSingleLine(boolean textSingleLine) {
        this.mTextSingleLine = textSingleLine;
        mViewFinderView.setTextSingleLine(mTextSingleLine);
        mViewFinderView.setupViewFinder();
    }


    public void startCamera(int cameraId) {
        if (mCameraHandlerThread == null) {
            mCameraHandlerThread = new CameraHandlerThread(this);
        }
        mCameraHandlerThread.startCamera(cameraId);
    }

    public void setupCameraPreview(CameraWrapper cameraWrapper) {
        mCameraWrapper = cameraWrapper;
        if (mCameraWrapper != null) {
            setUpCamera(mCameraWrapper.mCamera);
            setupLayout(mCameraWrapper);
            mViewFinderView.setupViewFinder();
            if (mFlashState != null) {
                setFlash(mFlashState);
            }
            setAutoFocus(mAutofocusState);
        }
    }


    /**
     * 用于放大
     *
     * @param camera
     */
    protected void setUpCamera(Camera camera) {

    }

    public void startCamera() {
        startCamera(CameraUtils.getDefaultCameraId());
    }

    public void stopCamera() {
        if (mCameraWrapper != null) {
            setUpCamera(null);
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
            mCameraWrapper.mCamera.release();
            mCameraWrapper = null;
        }
        if (mCameraHandlerThread != null) {
            mCameraHandlerThread.quit();
            mCameraHandlerThread = null;
        }
    }

    public void stopCameraPreview() {
        if (mPreview != null) {
            mPreview.stopCameraPreview();
        }
    }

    protected void resumeCameraPreview() {
        if (mPreview != null) {
            mPreview.showCameraPreview();
        }
    }

    public synchronized Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if (mFramingRectInPreview == null) {
            Rect framingRect = mViewFinderView.getFramingRect();
            int viewFinderViewWidth = mViewFinderView.getWidth();
            int viewFinderViewHeight = mViewFinderView.getHeight();
            if (framingRect == null || viewFinderViewWidth == 0 || viewFinderViewHeight == 0) {
                return null;
            }

            Rect rect = new Rect(framingRect);

            if (previewWidth < viewFinderViewWidth) {
                rect.left = rect.left * previewWidth / viewFinderViewWidth;
                rect.right = rect.right * previewWidth / viewFinderViewWidth;
            }

            if (previewHeight < viewFinderViewHeight) {
                rect.top = rect.top * previewHeight / viewFinderViewHeight;
                rect.bottom = rect.bottom * previewHeight / viewFinderViewHeight;
            }

            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }

    public void setFlash(boolean flag) {
        mFlashState = flag;
        if (mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {

            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if (flag) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    public boolean getFlash() {
        if (mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {
            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void toggleFlash() {
        if (mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {
            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        mAutofocusState = state;
        if (mPreview != null) {
            mPreview.setAutoFocus(state);
        }
    }

    public void setShouldScaleToFill(boolean shouldScaleToFill) {
        mShouldScaleToFill = shouldScaleToFill;
    }


    public void setAspectTolerance(float aspectTolerance) {
        mAspectTolerance = aspectTolerance;
    }

    public void setAutoZoom(boolean mAutoZoom) {
        this.mAutoZoom = mAutoZoom;
    }

    public void setAutoFocusInterval(int mAutoFocusInterval) {
        this.mAutoFocusInterval = mAutoFocusInterval;
    }

    public byte[] getRotatedData(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int width = size.width;
        int height = size.height;

        int rotationCount = getRotationCount();

        if (rotationCount == 1 || rotationCount == 3) {
            for (int i = 0; i < rotationCount; i++) {
                byte[] rotatedData = new byte[data.length];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        rotatedData[x * height + height - y - 1] = data[x + y * width];
                    }
                }
                data = rotatedData;
                int tmp = width;
                width = height;
                height = tmp;
            }
        }

        return data;
    }

    public int getRotationCount() {
        int displayOrientation = mPreview.getDisplayOrientation();
        return displayOrientation / 90;
    }
}
