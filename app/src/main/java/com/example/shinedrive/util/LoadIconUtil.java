package com.example.shinedrive.util;

import android.view.View;

import com.wang.avi.AVLoadingIndicatorView;

public class LoadIconUtil {
    public static void openLoadIcon(AVLoadingIndicatorView avi){
        if (avi.getVisibility() != View.VISIBLE) {
            // 控件不可见
            // 在这里可以进行相应逻辑处理
            avi.show();
        }
    }
    public static void closeLoadIcon(AVLoadingIndicatorView avi){
        if (avi.getVisibility() == View.VISIBLE) {
            // 控件可见
            // 在这里可以进行相应逻辑处理
            avi.setVisibility(View.GONE);
        }
    }
}
