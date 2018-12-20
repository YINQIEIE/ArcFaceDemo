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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView.OnCameraListener;

import java.util.ArrayList;
import java.util.List;

import static com.arcsoft.sdk_demo.CameraManager.setUpCamera;

/**
 * Created by gqj3375 on 2017/4/28.
 */

public class DetecterActivity extends Activity implements OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback {
    private final String TAG = this.getClass().getSimpleName();

    private TextView mTextView;
    private TextView mTextView1;
    private ImageView mImageView;
    private int mWidth, mHeight;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;

    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    Handler mHandler;
    //识别结果展示横向列表
    private RecyclerView rv_result;
    //识别结果集合
    private List<ResultBean> resultList = new ArrayList<>();
    private List<String> nameList = new ArrayList<>();
    private ResultAdapter adapter;

    List<AFT_FSDKFace> result = new ArrayList<>();
    private List<AFT_FSDKFace> resultRecorder = new ArrayList<>();
    private float score;
    private String name;
    private Bitmap bmp;
    private byte[] mImageNV21;
    private FRManager frManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mHandler = new Handler();
        initCamera();
        initSurfaceView();
        initFR();
        initRv();
        initPhotoView();
    }

    private void initPhotoView() {
        //snap
        mTextView = findViewById(R.id.textView);
        mTextView.setText("");
        mTextView1 = findViewById(R.id.textView1);
        mTextView1.setText("");
        mImageView = findViewById(R.id.imageView);

        mTextView.setVisibility(View.INVISIBLE);
        mTextView1.setVisibility(View.INVISIBLE);
        mImageView.setVisibility(View.INVISIBLE);
    }

    private void initRv() {
        rv_result = findViewById(R.id.rv_result);
        adapter = new ResultAdapter(this, resultList, mCameraRotate, mCameraMirror);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv_result.setLayoutManager(layoutManager);
        rv_result.setAdapter(adapter);
    }

    private void initSurfaceView() {
        mGLSurfaceView = findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        CameraSurfaceView mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(true, false);
    }

    private void initCamera() {
        int cameraId = getIntent().getIntExtra("Camera", 0);
        mCameraID = cameraId == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraRotate = cameraId == 0 ? 90 : 270;
        mCameraMirror = cameraId != 0;
        int[] size = getSurfaceSize();
        mCamera = setUpCamera(mCameraID, size[0], size[1], ImageFormat.NV21);
    }

    /**
     * 获取 surfaceView 的尺寸，来设置相机预览分辨率
     *
     * @return int[]
     */
    private int[] getSurfaceSize() {
        RelativeLayout rlCamera = findViewById(R.id.rlCamera);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) rlCamera.getLayoutParams();
        int top = layoutParams.topMargin;
        Log.d(TAG, "getSurfaceSize: width = " + layoutParams.width + " height = " + layoutParams.height);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        return new int[]{screenHeight - top, screenWidth};
    }

    /**
     * 初始化人脸识别
     */
    private void initFR() {
        mWidth = mCamera.getParameters().getPreviewSize().width;
        mHeight = mCamera.getParameters().getPreviewSize().height;
        Log.d(TAG, "initFR: " + mWidth + "x" + mHeight);
        Toast.makeText(this, "initFR: " + mWidth + "x" + mHeight, Toast.LENGTH_LONG).show();
        frManager = new FRManager();
        frManager.init(mWidth, mHeight, resultRecorder, MyApplication.mFaceDB.getmRegister());
        frManager.setFaceMatchListener(faceMatchListener);
        frManager.startFRTask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != frManager)
            frManager.destroy();
    }

    @Override
    public Camera setupCamera() {
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
        AFT_FSDKError err = frManager.getEngine().AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "onPreview:AFT_FSDK_FaceFeatureDetect =" + err.getCode());
//        if (result.isEmpty()) return new Rect[0];
        mImageNV21 = frManager.getmImageNV21();
        if (frManager.getmFRTask().isLoop()) {
            resultList.clear();
            nameList.clear();
            if (!result.isEmpty()) {
//                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
                frManager.setmImageNV21(mImageNV21);
                for (int i = 0; i < result.size(); i++) {
                    resultRecorder.add(new AFT_FSDKFace(result.get(i)));
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

    private void onFaceMatch() {
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
}
