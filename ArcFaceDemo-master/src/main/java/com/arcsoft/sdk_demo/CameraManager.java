package com.arcsoft.sdk_demo;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

public class CameraManager {

    private static final String TAG = "CameraManager";

    public static Camera setUpCamera(int mCameraID, int surfaceWidth, int surfaceHeight, int mFormat) {
        Camera mCamera = Camera.open(mCameraID);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getFitPreviewSize(parameters, surfaceWidth, surfaceHeight);
            Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
            parameters.setPreviewSize(size.width, size.height);
            parameters.setPreviewFormat(mFormat);
            for (Integer format : parameters.getSupportedPreviewFormats()) {
                Log.d(TAG, "FORMAT:" + format);
            }
            List<int[]> fps = parameters.getSupportedPreviewFpsRange();
            for (int[] count : fps) {
                Log.d(TAG, "T:");
                for (int data : count) {
                    Log.d(TAG, "V=" + data);
                }
            }
            //parameters.setPreviewFpsRange(15000, 30000);
            //parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
            //parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            //parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
            //parmeters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            //parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mCamera;
    }

    public static Camera.Size getFitPreviewSize(Camera.Parameters parameters, int surfaceWidth, int surfaceHeight) {
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width == surfaceWidth && size.height == surfaceHeight)
                return size;
        }
        Camera.Size resultSize = null;
        double targetRatio = (double) surfaceWidth / surfaceHeight;
        double MIN_TOLERANCE = Double.MAX_VALUE;
        double currentRatio;
        int minWidth = 1024;
        //在预览分辨率大于1024中间寻找最合适的值
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width < minWidth) continue;
            currentRatio = (double) size.width / size.height;
            if (Math.abs(targetRatio - currentRatio) < MIN_TOLERANCE) {
                resultSize = size;
                MIN_TOLERANCE = Math.abs(targetRatio - currentRatio);
            }
        }
        if (null != resultSize) return resultSize;
        //如果相机支持预览分辨率宽都低于1024的情况
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            currentRatio = (double) size.width / size.height;
            if (Math.abs(targetRatio - currentRatio) < MIN_TOLERANCE) {
                resultSize = size;
                MIN_TOLERANCE = Math.abs(targetRatio - currentRatio);
            }
        }
        return resultSize;
    }
}
