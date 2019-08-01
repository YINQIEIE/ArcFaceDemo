package com.arcsoft.sdk_demo;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;

import com.guo.android_extend.widget.CameraGLSurfaceView;

/**
 * 横竖屏适配
 */
public class MyCameraGLSurfaceView extends CameraGLSurfaceView {

    private double mAspectRatio;
    private int previewWidth, previewHeight;

    public MyCameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyCameraGLSurfaceView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);

        if (mAspectRatio != 0) {
            int orientation = getContext().getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                height = (int) (previewWidth * ((double) width / (double) previewHeight));
            else
                width = (int) (previewWidth * ((double) height / (double) previewHeight));
//                height = (int) (previewHeight * ((double) width / (double) previewWidth));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void setAspectRatio(int width, int height) {
        previewWidth = width;
        previewHeight = height;
        double ratio = ((double) width / (double) height);
        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
    }
}

