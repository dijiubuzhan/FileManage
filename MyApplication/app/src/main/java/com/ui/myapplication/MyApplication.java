package com.ui.myapplication;


import android.app.Application;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
           CrashHandler.getInstance().init(this);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
