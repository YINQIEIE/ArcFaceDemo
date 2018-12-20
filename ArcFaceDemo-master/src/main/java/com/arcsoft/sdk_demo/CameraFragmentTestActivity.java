package com.arcsoft.sdk_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

public class CameraFragmentTestActivity extends AppCompatActivity {

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
}
