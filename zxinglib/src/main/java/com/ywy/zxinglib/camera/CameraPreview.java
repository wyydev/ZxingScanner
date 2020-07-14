package com.ywy.zxinglib.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.common.detector.MathUtils;
import com.ywy.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 相机预览
 *
 * @author ywy
 * @date 2019/7/8
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";

    private CameraWrapper mCameraWrapper;
    private Handler mAutoFocusHandler;
    private boolean mPreviewing = true;
    private boolean mAutoFocus = true;
    private boolean mSurfaceCreated = false;
    private boolean mShouldScaleToFill = true;
    private Camera.PreviewCallback mPreviewCallback;
    private float mAspectTolerance = 0.1f;
    private long mAutoFocusInterval = 1000L;

    public CameraPreview(Context context, CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        super(context);
        init(cameraWrapper, previewCallback);
    }

    public CameraPreview(Context context, AttributeSet attrs, CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        super(context, attrs);
        init(cameraWrapper, previewCallback);
    }

    public void init(CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        setCamera(cameraWrapper, previewCallback);
        mAutoFocusHandler = new Handler();
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        mCameraWrapper = cameraWrapper;
        mPreviewCallback = previewCallback;
    }

    public void setShouldScaleToFill(boolean scaleToFill) {
        mShouldScaleToFill = scaleToFill;
    }

    public void setAutoFocusInterval(long autoFocusInterval) {
        this.mAutoFocusInterval = autoFocusInterval;
    }

    /**
     * 宽高比差值
     *
     * @param aspectTolerance
     */
    public void setAspectTolerance(float aspectTolerance) {
        mAspectTolerance = aspectTolerance;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        stopCameraPreview();
        showCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceCreated = false;
        stopCameraPreview();
    }


    private float mOldDis;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        //手势放大和缩小
        int pointCount = event.getPointerCount();
        if (pointCount == 1) {
            //一个手指不处理
        } else if (pointCount >= 2) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    mOldDis = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDis = getFingerSpacing(event);
                    if (newDis > mOldDis) {
                        handleZoom(true);
                    } else if (newDis < mOldDis) {
                        handleZoom(false);
                    }
                    mOldDis = newDis;
                    break;
                default:
                    break;
            }
        }
        return true;
    }


    private float getFingerSpacing(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            return MathUtils.distance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        }
        return -1;
    }

    private void handleZoom(boolean zoomIn) {
        if (mCameraWrapper == null) {
            return;
        }
        Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
        if (parameters.isZoomSupported()) {
            int curZoom = parameters.getZoom();
            int maxZoom = parameters.getMaxZoom();
            if (zoomIn) {
                if (curZoom < maxZoom) {
                    curZoom++;
                }
            } else {
                if (curZoom > 0) {
                    curZoom--;
                }
            }
            parameters.setZoom(curZoom);
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    private GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mCameraWrapper != null) {
                Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
                if (parameters.isZoomSupported()) {
                    int curZoom = parameters.getZoom();
                    int maxZoom = parameters.getMaxZoom();
                    if (curZoom >= maxZoom / 2) {
                        parameters.setZoom(0);
                    } else if (curZoom < maxZoom / 2) {
                        parameters.setZoom(maxZoom / 2);
                    }
                    mCameraWrapper.mCamera.setParameters(parameters);
                } else {
                    Log.e(TAG, "不支持放大");
                }
            }
            return super.onDoubleTap(e);

        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
//            Toast.makeText(getContext(), "单击", Toast.LENGTH_SHORT).show();
            handleFocus(e, mCameraWrapper.mCamera);
            return super.onSingleTapConfirmed(e);
        }
    });


    private static Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    private static void handleFocus(MotionEvent event, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, previewSize);

        camera.cancelAutoFocus();

        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            Log.i(TAG, "focus areas not supported");
        }


        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, previewSize);
        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            params.setMeteringAreas(meteringAreas);
        } else {
            Log.i(TAG, "metering areas not supported");
        }

        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
    }


    public void showCameraPreview() {
        if (mCameraWrapper != null) {
            try {
                getHolder().addCallback(this);
                mPreviewing = true;
                setupCameraParameters();
                mCameraWrapper.mCamera.setPreviewDisplay(getHolder());
                mCameraWrapper.mCamera.setDisplayOrientation(getDisplayOrientation());
                mCameraWrapper.mCamera.setOneShotPreviewCallback(mPreviewCallback);
                mCameraWrapper.mCamera.startPreview();
                if (mAutoFocus) {
                    if (mSurfaceCreated) {
                        // surface 创建后自动聚焦
                        safeAutoFocus();
                    } else {
                        // 延时聚焦
                        scheduleAutoFocus();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    public void safeAutoFocus() {
        try {
            mCameraWrapper.mCamera.autoFocus(autoFocusCB);
        } catch (RuntimeException re) {
            scheduleAutoFocus();
        }
    }

    public void stopCameraPreview() {
        if (mCameraWrapper != null) {
            try {
                mPreviewing = false;
                getHolder().removeCallback(this);
                mCameraWrapper.mCamera.cancelAutoFocus();
                mCameraWrapper.mCamera.setOneShotPreviewCallback(null);
                mCameraWrapper.mCamera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    public void setupCameraParameters() {
        Camera.Size optimalSize = getOptimalPreviewSize();
        Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        if (!mAutoFocus) {
            //连续拍照模式
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCameraWrapper.mCamera.setParameters(parameters);
        adjustViewSize(optimalSize);
    }

    private void adjustViewSize(Camera.Size cameraSize) {
        Point previewSize = convertSizeToLandscapeOrientation(new Point(getWidth(), getHeight()));
        float cameraRatio = ((float) cameraSize.width) / cameraSize.height;
        float screenRatio = ((float) previewSize.x) / previewSize.y;

        if (screenRatio > cameraRatio) {
            setViewSize((int) (previewSize.y * cameraRatio), previewSize.y);
        } else {
            setViewSize(previewSize.x, (int) (previewSize.x / cameraRatio));
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Point convertSizeToLandscapeOrientation(Point size) {
        if (getDisplayOrientation() % 180 == 0) {
            return size;
        } else {
            return new Point(size.y, size.x);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setViewSize(int width, int height) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        int tmpWidth;
        int tmpHeight;
        if (getDisplayOrientation() % 180 == 0) {
            tmpWidth = width;
            tmpHeight = height;
        } else {
            tmpWidth = height;
            tmpHeight = width;
        }

        if (mShouldScaleToFill) {
            int parentWidth = ((View) getParent()).getWidth();
            int parentHeight = ((View) getParent()).getHeight();
            float ratioWidth = (float) parentWidth / (float) tmpWidth;
            float ratioHeight = (float) parentHeight / (float) tmpHeight;

            float compensation;

            if (ratioWidth > ratioHeight) {
                compensation = ratioWidth;
            } else {
                compensation = ratioHeight;
            }

            tmpWidth = Math.round(tmpWidth * compensation);
            tmpHeight = Math.round(tmpHeight * compensation);
        }

        layoutParams.width = tmpWidth;
        layoutParams.height = tmpHeight;
        setLayoutParams(layoutParams);
    }

    public int getDisplayOrientation() {
        if (mCameraWrapper == null) {
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        if (mCameraWrapper.mCameraId == -1) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        } else {
            Camera.getCameraInfo(mCameraWrapper.mCameraId, info);
        }

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            // compensate the mirror
            result = (360 - result) % 360;
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Camera.Size getOptimalPreviewSize() {
        if (mCameraWrapper == null) {
            return null;
        }

        List<Camera.Size> sizes = mCameraWrapper.mCamera.getParameters().getSupportedPreviewSizes();
        int w = getWidth();
        int h = getHeight();
        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
            int portraitWidth = h;
            h = w;
            w = portraitWidth;
        }

        double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > mAspectTolerance) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void setAutoFocus(boolean state) {
        if (mCameraWrapper != null && mPreviewing) {
            if (state == mAutoFocus) {
                return;
            }
            mAutoFocus = state;
            if (mAutoFocus) {
                if (mSurfaceCreated) {
                    // check if surface created before using autofocus
                    Log.v(TAG, "Starting autofocus");
                    safeAutoFocus();
                } else {
                    scheduleAutoFocus(); // wait 1 sec and then do check again
                }
            } else {
                Log.v(TAG, "Cancelling autofocus");
                mCameraWrapper.mCamera.cancelAutoFocus();
            }
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        @Override
        public void run() {
            if (mCameraWrapper != null && mPreviewing && mAutoFocus && mSurfaceCreated) {
                safeAutoFocus();
            }
        }
    };


    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            scheduleAutoFocus();
        }
    };

    private void scheduleAutoFocus() {
        mAutoFocusHandler.postDelayed(doAutoFocus, mAutoFocusInterval);
    }
}
