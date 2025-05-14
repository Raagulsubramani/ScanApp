package com.gmscan;

import android.app.Application;
import com.gmscan.utility.PreferenceManager;

public class GmScanApplication extends Application {
    public static PreferenceManager preferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
    }
    public static PreferenceManager getPreferenceManager() {
        return preferenceManager;
    }
}
