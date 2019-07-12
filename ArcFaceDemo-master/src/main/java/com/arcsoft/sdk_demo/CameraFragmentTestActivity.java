package com.arcsoft.sdk_demo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.arcsoft.facerecognition.AFR_FSDKFace;

public class CameraFragmentTestActivity extends AppCompatActivity implements FRTask.OnFaceDetectedListener {

    private FrameLayout flContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_fragment_test);
        flContainer = findViewById(R.id.flContainer);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.flContainer, CameraFragment.createInstance(1))
                .commit();
    }

    @Override
    public void onFaceDetected(AFR_FSDKFace face, Bitmap faceBitmap) {
        //handle face data here
    }
}
