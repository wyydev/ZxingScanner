package com.ywy.zxinglib.camera;

import android.hardware.Camera;

import java.util.List;

/**
 *相机操作工具类
 *
 *@author ywy
 *@date 2019/7/8
 */
public class CameraUtils {


    public static Camera getCameraInstance() {
        return getCameraInstance(getDefaultCameraId());
    }

    public static int getDefaultCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int defaultCameraId = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            defaultCameraId = i;
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return defaultCameraId;
    }


    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            if (cameraId == -1) {
                c = Camera.open();
            } else {
                c = Camera.open(cameraId);
            }
        } catch (Exception e) {
            //相机正在被使用或者不可用
        }
        return c;
    }

    public static boolean isFlashSupported(Camera camera) {

        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            if (parameters.getFlashMode() == null) {
                return false;
            }

            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes == null || supportedFlashModes.isEmpty() ||
                    supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
}
