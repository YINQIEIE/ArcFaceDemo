package com.arcsoft.sdk_demo;

import android.graphics.Bitmap;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FRManager {

    private String TAG = getClass().getSimpleName();

    private final AFT_FSDKVersion version;

    private AFT_FSDKEngine fdEngine;
    private ASAE_FSDKVersion mAgeVersion;
    private ASAE_FSDKEngine mAgeEngine;
    private ASGE_FSDKVersion mGenderVersion;
    private ASGE_FSDKEngine mGenderEngine;
    private FRTask mFRTask;
    private boolean supportMultiFace;

    //对比本地人脸数据使用

    public static FaceDB mFaceDB;//本地人脸管理对象
    private static AFR_FSDKEngine frEngine = new AFR_FSDKEngine();
    private static AFR_FSDKError error = frEngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);

    private FaceMatchListener faceMatchListener;

    public FRManager() {
        this(false);
    }

    public FRManager(boolean supportMultiFace) {
        version = new AFT_FSDKVersion();
        fdEngine = new AFT_FSDKEngine();
        mAgeVersion = new ASAE_FSDKVersion();
        mAgeEngine = new ASAE_FSDKEngine();
        mGenderVersion = new ASGE_FSDKVersion();
        mGenderEngine = new ASGE_FSDKEngine();
        this.supportMultiFace = supportMultiFace;
    }

    public void init(int mWidth, int mHeight, List<AFT_FSDKFace> resultRecorder) {
        AFT_FSDKError err = fdEngine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());

        err = fdEngine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.age_key);
        Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
        error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
        Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

        ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.gender_key);
        Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
        error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
        Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());

        mFRTask = new FRTask(mWidth, mHeight, resultRecorder, supportMultiFace);
    }

    public void setFaceMatchListener(FaceMatchListener listener) {
        if (null != listener)
            this.faceMatchListener = listener;
    }

    public void startFRTask() {
        checkTaskNotNull();
        mFRTask.start();
    }

    public void destroy() {
        mFRTask.shutdown();
        AFT_FSDKError err = fdEngine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());

        frEngine.AFR_FSDK_UninitialEngine();

        fdEngine = null;
        frEngine = null;
        if (null != mFaceDB)
            mFaceDB.destroy();
    }

    public AFT_FSDKEngine getFdEngine() {
        return fdEngine;
    }

    public byte[] getmImageNV21() {
        checkTaskNotNull();
        return mFRTask.getmImageNV21();
    }

    public void setmImageNV21(byte[] mImageNV21) {
        checkTaskNotNull();
        mFRTask.setmImageNV21(mImageNV21);
    }

    private void checkTaskNotNull() {
        if (null == mFRTask)
            throw new IllegalStateException("call method<init> fist!");
    }

    public FRTask getmFRTask() {
        return mFRTask;
    }

    public void setOnFaceDetectedListener(FRTask.OnFaceDetectedListener onFaceDetectedListener) {
        checkTaskNotNull();
        mFRTask.setOnFaceDetectedListener(onFaceDetectedListener);
    }

    public boolean isTaskRunning() {
        checkTaskNotNull();
        return mFRTask.isAlive();
    }

    /**
     * 设置识别延时时间，单位：ms
     *
     * @param delay 时间
     */
    public void setDelay(int delay) {
        checkTaskNotNull();
        mFRTask.setDelay(delay);
    }

    public boolean isSupportMultiFace() {
        return supportMultiFace;
    }

    /**
     * 初始化本地人脸数据管理对象
     *
     * @param path 人脸特征路径
     */
    public static void initFaceDB(String path) {
        synchronized (FaceDB.class) {
            mFaceDB = new FaceDB(path);
        }
    }

    /**
     * 本地人脸对比
     *
     * @param regResult 提取脸部特征数据
     * @return 对比到的文件
     */
    @WorkerThread
    public static String compareWithLocalFaces(AFR_FSDKFace regResult) {
        ExecutorService service = null;
        try {
            List<FaceDB.FaceRegist> mRegister = mFaceDB.getmRegister();
            //计算线程数，最大10个
            int faceNumPerThread = 100;
            int threadNum = mRegister.size() / faceNumPerThread;
            if (mRegister.size() % faceNumPerThread > 0)
                threadNum++;
            if (threadNum > 10) {
                threadNum = 10;
                faceNumPerThread = mRegister.size() / 10;
            }
            service = Executors.newFixedThreadPool(threadNum);
            //初始化线程比对任务
            List<FaceDB.FaceRegist> partFaceList;
            FaceRecCallable task;
            List<FaceRecCallable> taskList = new ArrayList<>(threadNum);
            for (int i = 0; i < threadNum; i++) {
                if (i == threadNum - 1)
                    partFaceList = mRegister.subList(faceNumPerThread * i, mRegister.size());
                else
                    partFaceList = mRegister.subList(faceNumPerThread * i, faceNumPerThread * (i + 1));
                task = new FaceRecCallable(partFaceList, regResult);
                taskList.add(task);
            }
            List<Future<CompareResult>> futureList = service.invokeAll(taskList);
            CompareResult compareResult = null;
            for (int i = 0; i < futureList.size(); i++) {
                if (null == compareResult)
                    compareResult = futureList.get(i).get();
                else if (futureList.get(i).get().getScore() > compareResult.getScore())
                    compareResult = futureList.get(i).get();
            }
            service.shutdown();
            if (null != compareResult && compareResult.getScore() > 0.6f)
                return compareResult.getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (null != service)
            service.shutdown();
        return null;
    }

    private static class FaceRecCallable implements Callable<CompareResult> {

        private List<FaceDB.FaceRegist> faceRegists;
        private AFR_FSDKFace cameraFace;

        private FaceRecCallable(List<FaceDB.FaceRegist> faceRegists, AFR_FSDKFace cameraFace) {
            this.faceRegists = faceRegists;
            this.cameraFace = cameraFace;
        }

        @Override
        public CompareResult call() throws Exception {
            AFR_FSDKMatching score = new AFR_FSDKMatching();
            float max = 0.0f;
            String name = null;
            for (FaceDB.FaceRegist fr : faceRegists) {
                for (AFR_FSDKFace face : fr.getmFaceList()) {
                    frEngine.AFR_FSDK_FacePairMatching(cameraFace, face, score);
                    if (max < score.getScore()) {
                        max = score.getScore();
                        name = fr.getmName();
                    }
                }
            }
            return new CompareResult(name, max);
        }
    }

    /**
     * 匹配到人脸回调接口
     */
    public interface FaceMatchListener {

        void onMatch(float score, String name, Bitmap bmp);

        void onMatchDone();
    }

}
