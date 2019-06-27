package com.arcsoft.sdk_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.os.SystemClock;
import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;

import java.io.IOException;
import java.util.List;

public class FRTask extends AbsLoop {

    private String TAG = getClass().getSimpleName();

    private AFR_FSDKVersion version = new AFR_FSDKVersion();
    private AFR_FSDKEngine engine = new AFR_FSDKEngine();
    private AFR_FSDKFace regResult = new AFR_FSDKFace();

    //已注册人脸信息
    private List<FaceDB.FaceRegist> mResgist;

    private int mWidth, mHeight;

    private byte[] mImageNV21 = null;
    private AFT_FSDKFace mAFT_FSDKFace = null;
    private List<AFT_FSDKFace> resultRecorder;

    private boolean loop = true;
    private int delay = 500;//每次识别出人脸后停顿时间

    private FaceMatchListener faceMatchListener;
    private OnFaceDetectedListener onFaceDetectedListener;

    private boolean supportMultiFace = false;

    public FRTask(List<FaceDB.FaceRegist> mResgist, int mWidth, int mHeight, List<AFT_FSDKFace> resultRecorder) {
        this(mResgist, mWidth, mHeight, resultRecorder, false);
    }

    public FRTask(List<FaceDB.FaceRegist> mResgist, int mWidth, int mHeight, List<AFT_FSDKFace> resultRecorder, boolean supportMultiFace) {
        this.mResgist = mResgist;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.resultRecorder = resultRecorder;
        this.supportMultiFace = supportMultiFace;
    }


    @Override
    public void setup() {
        AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
        Log.d(TAG, "AFR_FSDK_InitialEngine = " + error.getCode());
        error = engine.AFR_FSDK_GetVersion(version);
        Log.d(TAG, "FR=" + version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
    }

    @Override
    public void loop() {
        if (!loop) return;
//        SystemClock.sleep(500);
        for (int i = 0; i < resultRecorder.size(); i++) {
            loop = false;
            AFT_FSDKFace aft_fsdkFace = resultRecorder.get(i);
            if (null == aft_fsdkFace) continue;
            mAFT_FSDKFace = aft_fsdkFace.clone();
            if (mImageNV21 != null) {
                Log.i(TAG, "loop: mImageNV21 != null");
                long time = System.currentTimeMillis();
                AFR_FSDKError error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), regResult);
                Log.d(TAG, "AFR_FSDK_ExtractFRFeature cost :" + (System.currentTimeMillis() - time) + "ms");
                Log.d(TAG, "Face=" + regResult.getFeatureData()[0] + "," + regResult.getFeatureData()[1] + "," + regResult.getFeatureData()[2] + "," + error.getCode());

                if (null != regResult.getFeatureData()) {
                    if (null != onFaceDetectedListener)
                        onFaceDetectedListener.onFaceDetected(regResult.clone());
                }
//                一次只处理一个人脸数据
//                if (i == 0) break;
                AFR_FSDKMatching score = new AFR_FSDKMatching();
                float max = 0.0f;
                String name = null;
                for (FaceDB.FaceRegist fr : mResgist) {
                    for (AFR_FSDKFace face : fr.mFaceList) {
                        error = engine.AFR_FSDK_FacePairMatching(regResult, face, score);
                        Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                        if (max < score.getScore()) {
                            max = score.getScore();
                            name = fr.mName;
                        }
                    }
                }
                //crop
                byte[] data = mImageNV21;
                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                yuv.compressToJpeg(mAFT_FSDKFace.getRect(), 80, ops);
                final Bitmap bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
                try {
                    ops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (null != faceMatchListener)
                    faceMatchListener.onMatch(max, name, bmp);
                if (!supportMultiFace && i == 0) {
                    Log.i("supportMultiFace", supportMultiFace + ">>>" + i);
                    mImageNV21 = null;
                    if (null != faceMatchListener)
                        faceMatchListener.onMatchDone();
                    break;
                } else {
                    if (i == resultRecorder.size() - 1) {
                        mImageNV21 = null;
                        if (null != faceMatchListener)
                            faceMatchListener.onMatchDone();
                    }
                }
            }
        }
        if (!loop) {
            SystemClock.sleep(delay);
            loop = true;
        }
//        regResult.setFeatureData(null);
        resultRecorder.clear();
    }

    @Override
    public void over() {
        AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
        Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
        engine = null;
        if (null != regResult) {
            regResult.setFeatureData(null);
            regResult = null;
        }
        mAFT_FSDKFace = null;
        resultRecorder.clear();
    }

    public byte[] getmImageNV21() {
        return mImageNV21;
    }

    public void setmImageNV21(byte[] mImageNV21) {
        this.mImageNV21 = mImageNV21;
    }

    /**
     * 匹配到人脸回调接口
     */
    public interface FaceMatchListener {
        void onMatch(float score, String name, Bitmap bmp);

        void onMatchDone();
    }

    public void setFaceMatchListener(FaceMatchListener faceMatchListener) {
        this.faceMatchListener = faceMatchListener;
    }

    /**
     * 检测到人脸回调接口
     */
    public interface OnFaceDetectedListener {
        void onFaceDetected(AFR_FSDKFace face);
    }

    public void setOnFaceDetectedListener(OnFaceDetectedListener onFaceDetectedListener) {
        this.onFaceDetectedListener = onFaceDetectedListener;
    }

    public boolean isLoop() {
        return loop;
    }

    /**
     * 设置识别延时时间，单位：ms
     *
     * @param delay 时间
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }
}
