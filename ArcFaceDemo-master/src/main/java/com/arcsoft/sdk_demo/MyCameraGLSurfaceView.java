package com.arcsoft.sdk_demo;

import android.content.Context;
import android.util.AttributeSet;

import com.guo.android_extend.widget.CameraGLSurfaceView;

/**
 * 只考虑竖屏
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
            height = (int) (previewWidth * ((double) width / (double) previewHeight));
        }
        setMeasuredDimension(width, height);
//        setMeasuredDimension(height, width);
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

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
