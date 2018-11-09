package com.arcsoft.sdk_demo;

import android.util.Log;

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

import java.util.List;

public class FRManager {

    private String TAG = getClass().getSimpleName();

    private final AFT_FSDKVersion version;

    private final AFT_FSDKEngine engine;
    private final ASAE_FSDKVersion mAgeVersion;
    private final ASAE_FSDKEngine mAgeEngine;
    private final ASGE_FSDKVersion mGenderVersion;
    private final ASGE_FSDKEngine mGenderEngine;

    public FRTask getmFRTask() {
        return mFRTask;
    }

    private FRTask mFRTask;

    public FRManager() {
        version = new AFT_FSDKVersion();
        engine = new AFT_FSDKEngine();
        mAgeVersion = new ASAE_FSDKVersion();
        mAgeEngine = new ASAE_FSDKEngine();
        mGenderVersion = new ASGE_FSDKVersion();
        mGenderEngine = new ASGE_FSDKEngine();
    }

    public void init(int mWidth, int mHeight, List<AFT_FSDKFace> resultRecorder) {
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

        mFRTask = new FRTask(mWidth, mHeight, resultRecorder);
    }

    public void setFaceMatchListener(FRTask.FaceMatchListener listener) {
        if (null == mFRTask)
            throw new IllegalStateException("call method<init> first!");
        if (null != listener)
            mFRTask.setFaceMatchListener(listener);
    }

    public void startFRTask() {
        if (null == mFRTask)
            throw new IllegalStateException("call method<init> first!");
        mFRTask.start();
    }

    public void destroy() {
        mFRTask.shutdown();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
    }

    public AFT_FSDKEngine getEngine() {
        return engine;
    }

    public byte[] getmImageNV21() {
        if (null == mFRTask)
            throw new IllegalStateException("call method<init> first!");
        return mFRTask.getmImageNV21();
    }

    public void setmImageNV21(byte[] mImageNV21) {
        if (null == mFRTask)
            throw new IllegalStateException("call method<init> fist!");
        mFRTask.setmImageNV21(mImageNV21);
    }

}
