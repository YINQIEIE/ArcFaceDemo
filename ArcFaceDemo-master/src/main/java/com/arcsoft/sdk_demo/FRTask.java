package com.arcsoft.sdk_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.util.Log;

import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FRTask extends AbsLoop {

    private String TAG = getClass().getSimpleName();

    private AFR_FSDKVersion version = new AFR_FSDKVersion();
    private AFR_FSDKEngine engine = new AFR_FSDKEngine();
    private AFR_FSDKFace regResult = new AFR_FSDKFace();
    //已注册人脸信息
    private List<FaceDB.FaceRegist> mResgist = MyApplication.mFaceDB.mRegister;
    private int mWidth, mHeight;
    private byte[] mImageNV21 = null;
    private AFT_FSDKFace mAFT_FSDKFace = null;
    private List<AFT_FSDKFace> resultRecorder;

    //年龄性别识别相关
    private ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
    private ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
    private List<ASAE_FSDKFace> face1 = new ArrayList<>();
    private List<ASGE_FSDKFace> face2 = new ArrayList<>();
    private List<ASAE_FSDKAge> ages = new ArrayList<>();
    private List<ASGE_FSDKGender> genders = new ArrayList<>();

    public boolean isLoop() {
        return loop;
    }

    private boolean loop = true;
    private FaceMatchListener faceMatchListener;

    public FRTask(int mWidth, int mHeight, List<AFT_FSDKFace> resultRecorder) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.resultRecorder = resultRecorder;
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
        Log.d(TAG, "loop: looping");
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
                //age & gender
                face1.clear();
                face2.clear();
                face1.add(new ASAE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
                face2.add(new ASGE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
                ASAE_FSDKError error1 = mAgeEngine.ASAE_FSDK_AgeEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face1, ages);
                ASGE_FSDKError error2 = mGenderEngine.ASGE_FSDK_GenderEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face2, genders);
                Log.d(TAG, "ASAE_FSDK_AgeEstimation_Image:" + error1.getCode() + ",ASGE_FSDK_GenderEstimation_Image:" + error2.getCode());
                Log.d(TAG, "age:" + ages.get(0).getAge() + ",gender:" + genders.get(0).getGender());
                final String age = ages.get(0).getAge() == 0 ? "年龄未知" : ages.get(0).getAge() + "岁";
                final String gender = genders.get(0).getGender() == -1 ? "性别未知" : (genders.get(0).getGender() == 0 ? "男" : "女");

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
                if (i == resultRecorder.size() - 1) {
                    mImageNV21 = null;
                    if (null != faceMatchListener)
                        faceMatchListener.onMatchDone();
                }
            }
        }
        loop = true;
        resultRecorder.clear();
    }

    @Override
    public void over() {
        AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
        Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
    }

    public byte[] getmImageNV21() {
        return mImageNV21;
    }

    public void setmImageNV21(byte[] mImageNV21) {
        this.mImageNV21 = mImageNV21;
    }

    public interface FaceMatchListener {
        void onMatch(float score, String name, Bitmap bmp);

        void onMatchDone();
    }

    public void setFaceMatchListener(FaceMatchListener faceMatchListener) {
        this.faceMatchListener = faceMatchListener;
    }

}
