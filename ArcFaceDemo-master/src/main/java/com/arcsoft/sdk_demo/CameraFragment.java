package com.arcsoft.sdk_demo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraSurfaceView;

import java.util.ArrayList;
import java.util.List;

public class CameraFragment extends Fragment implements View.OnTouchListener, CameraSurfaceView.OnCameraListener, Camera.AutoFocusCallback, ViewTreeObserver.OnGlobalLayoutListener {

    private final String TAG = getClass().getSimpleName();

    private static final String CAMERA_ID = "cameraID";

    private Camera mCamera;
    private int mCameraID;
    private int mCameraRotate;
    private boolean mCameraMirror;
    private CameraSurfaceView surfaceView;
    private MyCameraGLSurfaceView glSurfaceView;
    private View rootView;
    private int[] surfaceSize;
    private int additionalRotation = 0;//额外旋转角度，特殊情况如一些定制设备，硬件安装不规范的时候使用

    private FRManager frManager;

    private FRTask.OnFaceDetectedListener onFaceDetectedListener;
    List<AFT_FSDKFace> result = new ArrayList<>();//识别结果
    private List<AFT_FSDKFace> resultRecorder = new ArrayList<>();

    public static CameraFragment createInstance(int mCameraID) {
        CameraFragment fragment = new CameraFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(CAMERA_ID, mCameraID);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FRTask.OnFaceDetectedListener)
            this.onFaceDetectedListener = (FRTask.OnFaceDetectedListener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mCameraID = bundle.getInt(CAMERA_ID, 0);
        initCameraParams();
    }

    /**
     * 相机初始化
     */
    private void initCameraParams() {
        mCameraID = mCameraID == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
//        mCameraRotate = mCameraID == 0 ? 90 : 270;
        mCameraRotate = getCameraOri(getActivity().getWindowManager().getDefaultDisplay().getRotation());
//        mCameraMirror = cameraId == 0;//由于一体机后置摄像头朝前所以要加镜像
        mCameraMirror = mCameraID != 0;//平板等其他设备正常
    }

    /**
     * 获取预览应旋转的角度
     *
     * @param rotation 屏幕旋转信息
     * @return 角度
     */
    private int getCameraOri(int rotation) {
        int degrees = rotation * 90;
        additionalRotation /= 90;
        additionalRotation *= 90;
        degrees += additionalRotation;
        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSurfaceView(view);
    }

    private void initSurfaceView(View view) {
        surfaceView = view.findViewById(R.id.surfaceView);
        glSurfaceView = view.findViewById(R.id.glSurfaceView);
        glSurfaceView.setOnTouchListener(this);
        surfaceView.setOnCameraListener(this);
        surfaceView.setupGLSurafceView(glSurfaceView, true, mCameraMirror, mCameraRotate);
        surfaceView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        surfaceView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        surfaceSize = getSurfaceSize();
    }

    /**
     * 获取 surfaceview 大小计算合适的分辨率
     *
     * @return 宽高对调后的宽高数组
     */
    private int[] getSurfaceSize() {
        View view = rootView.findViewById(R.id.glSurfaceView);
        int orientation = getResources().getConfiguration().orientation;
//        Log.i(TAG, "getSurfaceSize: width = " + view.getMeasuredWidth() + " height = " + view.getMeasuredHeight());
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            return new int[]{view.getMeasuredHeight(), view.getMeasuredWidth()};
        else
            return new int[]{view.getMeasuredWidth(), view.getMeasuredHeight()};
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override
    public Camera setupCamera() {
        mCamera = CameraManager.setUpCamera(mCameraID, surfaceSize[0], surfaceSize[1], ImageFormat.NV21);
        if (null != mCamera && null == frManager)
            initFR();
        return mCamera;
    }

    /**
     * 初始化人脸识别
     */
    private void initFR() {
        int mWidth = mCamera.getParameters().getPreviewSize().width;
        int mHeight = mCamera.getParameters().getPreviewSize().height;
        frManager = new FRManager();
        frManager.init(mWidth, mHeight, resultRecorder);
        frManager.setDelay(1000);
//        frManager.setFaceMatchListener(faceMatchListener);
        frManager.setOnFaceDetectedListener((afrFace, faceBitmap) -> {
                    if (null != onFaceDetectedListener)
                        onFaceDetectedListener.onFaceDetected(afrFace, faceBitmap);
                }
        );
        frManager.startFRTask();
    }

    @Override
    public void setupChanged(int format, int width, int height) {

    }

    @Override
    public boolean startPreviewLater() {
        return false;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        AFT_FSDKError err = frManager.getFdEngine().AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "onPreview:AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        byte[] mImageNV21;
        if (frManager.getmFRTask().isLoop()) {
            if (!result.isEmpty()) {
                mImageNV21 = data.clone();
                frManager.setmImageNV21(mImageNV21);
                for (int i = 0; i < result.size(); i++) {
                    resultRecorder.add(new AFT_FSDKFace(result.get(i)));
                }
            }
        }
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        result.clear();
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        glSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != frManager)
            frManager.destroy();
    }
}
