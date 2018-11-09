package com.arcsoft.sdk_demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView.OnCameraListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gqj3375 on 2017/4/28.
 */

public class DetecterActivity extends Activity implements OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback {
    private final String TAG = this.getClass().getSimpleName();

    private int mWidth, mHeight, mFormat;
    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;

    private RecyclerView rv_result;
    private List<ResultBean> resultList = new ArrayList<>();
    private List<String> nameList = new ArrayList<>();
    private ResultAdapter adapter;

    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    ASAE_FSDKVersion mAgeVersion = new ASAE_FSDKVersion();
    ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
    ASGE_FSDKVersion mGenderVersion = new ASGE_FSDKVersion();
    ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
    List<AFT_FSDKFace> result = new ArrayList<>();

    private List<AFT_FSDKFace> resultRecoder = new ArrayList<>();

    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    FRTask mFRTask = null;
    Handler mHandler;
    private float score;
    private String name;
    private Bitmap bmp;
    private FRTask.FaceMatchListener faceMatchListener = new FRTask.FaceMatchListener() {
        @Override
        public void onMatch(float score, String name, Bitmap bmp) {
            DetecterActivity.this.score = score;
            DetecterActivity.this.name = name;
            DetecterActivity.this.bmp = bmp;
            runOnUiThread(matchRunnable);
        }

        @Override
        public void onMatchDone() {
            runOnUiThread(faceMatchDoneRunnable);
        }
    };

    Runnable hide = new Runnable() {
        @Override
        public void run() {
            mTextView.setAlpha(0.5f);
            mImageView.setImageAlpha(128);
        }
    };

    Runnable matchRunnable = new Runnable() {
        @Override
        public void run() {
            onFaceMatch();
        }
    };

    Runnable faceMatchDoneRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
        }
    };
    private byte[] mImageNV21;

    private void onFaceMatch() {
        //fr success.
        if (score > 0.6f) {
            Log.d(TAG, "fit Score:" + score + ", NAME:" + name);
            final String mNameShow = name;
            mHandler.removeCallbacks(hide);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mTextView.setAlpha(1.0f);
                    mTextView.setText(mNameShow);
                    mTextView.setTextColor(Color.RED);
//                                mTextView1.setVisibility(View.VISIBLE);
                    mTextView1.setText("置信度：" + (float) ((int) (score * 1000)) / 1000.0);
                    mTextView1.setTextColor(Color.RED);
                    mImageView.setRotation(mCameraRotate);
                    if (mCameraMirror) {
                        mImageView.setScaleY(-1);
                    }
                    mImageView.setImageAlpha(255);
                    mImageView.setImageBitmap(bmp);
                    if (!nameList.contains(mNameShow)) {
                        nameList.add(mNameShow);
                        resultList.add(new ResultBean(mNameShow, "置信度：" + (float) ((int) (score * 1000)) / 1000.0, bmp));
                    }
                }
            });
        } else {
            final String mNameShow = "未识别";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setAlpha(1.0f);
//                                mTextView1.setVisibility(View.VISIBLE);
//                        mTextView1.setText(gender + "," + age);
                    mTextView1.setTextColor(Color.RED);
                    mTextView.setText(mNameShow);
                    mTextView.setTextColor(Color.RED);
                    mImageView.setImageAlpha(255);
                    mImageView.setRotation(mCameraRotate);
                    if (mCameraMirror) {
                        mImageView.setScaleY(-1);
                    }
                    mImageView.setImageBitmap(bmp);
                }
            });
        }
    }


    private TextView mTextView;
    private TextView mTextView1;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCameraID = getIntent().getIntExtra("Camera", 0) == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraRotate = getIntent().getIntExtra("Camera", 0) == 0 ? 90 : 270;
        mCameraMirror = getIntent().getIntExtra("Camera", 0) == 0 ? false : true;
        mWidth = 1280;
        mHeight = 960;
        mFormat = ImageFormat.NV21;
        mHandler = new Handler();

        setContentView(R.layout.activity_camera);
        mGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(true, false);

        rv_result = (RecyclerView) findViewById(R.id.rv_result);
        adapter = new ResultAdapter(this, resultList, mCameraRotate, mCameraMirror);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv_result.setLayoutManager(layoutManager);
        rv_result.setAdapter(adapter);

        //snap
        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setText("");
        mTextView1 = (TextView) findViewById(R.id.textView1);
        mTextView1.setText("");
        mImageView = (ImageView) findViewById(R.id.imageView);

        mTextView.setVisibility(View.INVISIBLE);
        mTextView1.setVisibility(View.INVISIBLE);
        mImageView.setVisibility(View.INVISIBLE);

        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.age_key);
        Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
        error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
        Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

        ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.gender_key);
        Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
        error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
        Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());

        mFRTask = new FRTask(mWidth, mHeight, resultRecoder);
        mFRTask.setFaceMatchListener(faceMatchListener);
        mFRTask.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFRTask.shutdown();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
    }

    @Override
    public Camera setupCamera() {
        mCamera = Camera.open(mCameraID);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);

            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
            }
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
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
        }
        return mCamera;
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
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "onPreview:AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        mImageNV21 = mFRTask.getmImageNV21();
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
//                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
                mFRTask.setmImageNV21(mImageNV21);
                for (int i = 0; i < result.size(); i++) {
                    resultRecoder.add(new AFT_FSDKFace(result.get(i)));
                }
            } else {
                mHandler.postDelayed(hide, 3000);
            }
        }
        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        result.clear();
        //return the rects for render.
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d(TAG, "Camera Focus SUCCESS!");
        }
    }
}
