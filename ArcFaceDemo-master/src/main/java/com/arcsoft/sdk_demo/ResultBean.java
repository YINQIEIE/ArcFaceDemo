package com.arcsoft.sdk_demo;

import android.graphics.Bitmap;

public class ResultBean {
    String name;
    String percent;
    Bitmap bitmap;

    public ResultBean(String name, String percent, Bitmap bitmap) {
        this.name = name;
        this.percent = percent;
        this.bitmap = bitmap;
    }
}
