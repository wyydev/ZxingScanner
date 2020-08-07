package com.ywy.zxinglib.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.ywy.util.DisplayUtils;
import com.ywy.zxinglib.camera.BarcodeScannerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 扫码界面
 *
 * @author ywy
 * @date 2019/7/8
 */
public class ZXingScannerView extends BarcodeScannerView {
    private static final String TAG = "ZXingScannerView";
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // We want at least 2 threads and at most 4 threads in the core pool,
    // preferring to have 1 less than the CPU count to avoid saturating
    // the CPU with background work
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;
    private AtomicBoolean abortingScan = new AtomicBoolean(false);
    private int previewFrameCount = 0;
    public interface ResultHandler {
        void handleResult(Result rawResult);
    }

    private MultiFormatReader mMultiFormatReader;
    public static final List<BarcodeFormat> ALL_FORMATS = new ArrayList<>();
    private List<BarcodeFormat> mFormats;
    private ResultHandler mResultHandler;
    private ThreadPoolExecutor threadPoolExecutor;

    static {
        ALL_FORMATS.add(BarcodeFormat.AZTEC);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.MAXICODE);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.RSS_EXPANDED);
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.UPC_EAN_EXTENSION);
    }

    public ZXingScannerView(Context context) {
        super(context);
        threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(128), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
        initMultiFormatReader();
    }

    public ZXingScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(128), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
        initMultiFormatReader();
    }

    public void setFormats(List<BarcodeFormat> formats) {
        mFormats = formats;
        initMultiFormatReader();
    }

    public void setResultHandler(ResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    public Collection<BarcodeFormat> getFormats() {
        if (mFormats == null) {
            return ALL_FORMATS;
        }
        return mFormats;
    }

    private void initMultiFormatReader() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, getFormats());
        mMultiFormatReader = new MultiFormatReader();
        mMultiFormatReader.setHints(hints);
    }

    @Override
    protected void setUpCamera(Camera camera) {
        super.setUpCamera(camera);
        if (mMultiFormatReader != null) {
            mMultiFormatReader.setCamera(camera);
        }
    }


    @Override
    public void setAutoZoom(boolean mAutoZoom) {
        super.setAutoZoom(mAutoZoom);
        if (mMultiFormatReader != null) {
            mMultiFormatReader.setAutoZoom(mAutoZoom);
        }
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (mResultHandler == null) {
            return;
        }

//        try {
//            Camera.Parameters parameters = camera.getParameters();
//            Camera.Size size = parameters.getPreviewSize();
//            int width = size.width;
//            int height = size.height;
//
//            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
//                int rotationCount = getRotationCount();
//                if (rotationCount == 1 || rotationCount == 3) {
//                    int tmp = width;
//                    width = height;
//                    height = tmp;
//                }
//                data = getRotatedData(data, camera);
//            }
//
//            Result rawResult = null;
//            PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);
//
//            if (source != null) {
//                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//                try {
//                    rawResult = mMultiFormatReader.decodeWithState(bitmap);
//                } catch (ReaderException re) {
//                    // continue
//                } catch (NullPointerException npe) {
//                    // This is terrible
//                } catch (ArrayIndexOutOfBoundsException aoe) {
//
//                } finally {
//                    mMultiFormatReader.reset();
//                }
//
//                if (rawResult == null) {
//                    LuminanceSource invertedSource = source.invert();
//                    bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
//                    try {
//                        rawResult = mMultiFormatReader.decodeWithState(bitmap);
//                    } catch (NotFoundException e) {
//                        // continue
//                    } finally {
//                        mMultiFormatReader.reset();
//                    }
//                }
//            }
//
//            final Result finalRawResult = rawResult;
//
//            if (finalRawResult != null) {
//                Handler handler = new Handler(Looper.getMainLooper());
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        // Stopping the preview can take a little long.
//                        // So we want to set result handler to null to discard subsequent calls to
//                        // onPreviewFrame.
//                        ResultHandler tmpResultHandler = mResultHandler;
//                        mResultHandler = null;
//
//                        stopCameraPreview();
//                        if (tmpResultHandler != null) {
//                            tmpResultHandler.handleResult(finalRawResult);
//                        }
//                    }
//                });
//            } else {
//                camera.setOneShotPreviewCallback(this);
//            }
//        } catch (RuntimeException e) {
//            // TODO: Terrible hack. It is possible that this method is invoked after camera is released.
//            Log.e(TAG, e.toString(), e);
//        }

//        Log.e("ZXing","onPreviewFrame"+abortingScan.get());

        if (abortingScan.get()) {
            return;
        }

        // 节流
        if (previewFrameCount++ % 5 != 0) {
            return;
        }

        camera.addCallbackBuffer(mPreview.getBuffer());


        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ZXingScannerView.this.onDetect(data, camera);
            }
        });
    }

    public void resumeCameraPreview(ResultHandler resultHandler) {
        mResultHandler = resultHandler;
        if (abortingScan.get()){
            abortingScan.compareAndSet(true,false);
        }
        super.resumeCameraPreview();
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview(width, height);
        mMultiFormatReader.setFramingRect(rect);
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        PlanarYUVLuminanceSource source = null;

        try {
            if (mScanFullScreen) {
                //识别区域改为全屏
                source = new PlanarYUVLuminanceSource(data, width, height, 0, 0,
                        width, height, false);
            } else {
                source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                        rect.width(), rect.height(), false);
            }

        } catch (Exception e) {
        }

        return source;
    }


    private void onDetect(byte[] data, final Camera camera) {
        if (camera == null || data == null) {
            //相机已关闭
            return;
        }

        // 扫描成功阻止多余的操作
        if (abortingScan.get()) {
            return;
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int width = size.width;
            int height = size.height;

            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
                int rotationCount = getRotationCount();
                if (rotationCount == 1 || rotationCount == 3) {
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
                data = getRotatedData(data, camera);
            }

            Result rawResult = null;
            PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);

            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = mMultiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    // continue
                } catch (NullPointerException npe) {
                    // This is terrible
                } catch (ArrayIndexOutOfBoundsException aoe) {

                } finally {
                    mMultiFormatReader.reset();
                }

                if (rawResult == null) {
                    LuminanceSource invertedSource = source.invert();
                    bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
                    try {
                        rawResult = mMultiFormatReader.decodeWithState(bitmap);
                    } catch (NotFoundException e) {
                        // continue
                    } finally {
                        mMultiFormatReader.reset();
                    }
                }
            }

            final Result finalRawResult = rawResult;
//            Log.e("ZXing","解析失败");
            if (finalRawResult != null) {

                if (!abortingScan.compareAndSet(false, true)) {//有一个扫描成功
                    return;
                }

//                Log.e("ZXing","解析成功");

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Stopping the preview can take a little long.
                        // So we want to set result handler to null to discard subsequent calls to
                        // onPreviewFrame.
                        ResultHandler tmpResultHandler = mResultHandler;
                        mResultHandler = null;
                        stopCameraPreview();
                        if (tmpResultHandler != null) {
                            tmpResultHandler.handleResult(finalRawResult);
                        }
                    }
                });

            }
        } catch (OutOfMemoryError error) {
            //内存溢出则取消当次操作
        } catch (RuntimeException e) {
            // TODO: Terrible hack. It is possible that this method is invoked after camera is released.
            Log.e(TAG, e.toString(), e);
        }
    }


}
