package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.StringRes;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.LibreData;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.PlusAsyncExecutor;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.VersionTracker;


import static com.eveningoutpost.dexdrip.utils.VersionFixer.disableUpdates;

import java.util.Locale;


/**
 * Created by Emma Black on 3/21/15.
 */

public class xdrip extends Application {

    private static final String TAG = "xdrip.java";
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public static PlusAsyncExecutor executor;
    private static boolean fabricInited = false;
    private static Boolean isRunningTestCache;
    private static Locale LOCALE;

    @Override
    public void onCreate() {
        xdrip.context = getApplicationContext();
        super.onCreate();
        try {
            if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_crashlytics", true)) {

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        ActiveAndroid.initialize(this);
        updateMigrations();
        DemiGod.isPresent();
        JoH.forceBatteryWhitelisting();
        executor = new PlusAsyncExecutor();
        VersionTracker.updateDevice();
        disableUpdates();

    }

    public static Context getLangContext(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Pref.getBooleanDefaultFalse("force_english")) {
                final String forced_language = Pref.getString("forced_language", "en");
                final Configuration config = context.getResources().getConfiguration();

                if (LOCALE == null) LOCALE = new Locale(forced_language);
                Locale.setDefault(LOCALE);
                config.setLocale(LOCALE);
                context = context.createConfigurationContext(config);
                //Log.d(TAG, "Sending language context for: " + LOCALE);
                return new ContextWrapper(context);
            } else {
                return context;
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception in getLangContext: " + e);
            return context;
        }
    }

    public static Context getAppContext() {
        return xdrip.context;
    }

    public static boolean checkAppContext(Context context) {
        if (getAppContext() == null) {
            xdrip.context = context;
            return false;
        } else {
            return true;
        }
    }

    private static void updateMigrations() {
        Sensor.InitDb(context);//ensure database has already been initialized
        BgReading.updateDB();
        LibreBlock.updateDB();
        LibreData.updateDB();
    }

    private static boolean isWear2OrAbove() {
        return Build.VERSION.SDK_INT > 23;
    }

    public static synchronized boolean isRunningTest() {
        android.util.Log.e(TAG, Build.MODEL);
        if (null == isRunningTestCache) {
            boolean test_framework;
            if ("robolectric".equals(Build.FINGERPRINT)) {
                isRunningTestCache = true;
            } else {
                try {
                    Class.forName("android.support.test.espresso.Espresso");
                    test_framework = true;
                } catch (ClassNotFoundException e) {
                    test_framework = false;
                }
                isRunningTestCache = test_framework;
            }
        }
        return isRunningTestCache;
    }

    public static String gs(@StringRes final int id) {
        return getAppContext().getString(id);
    }

    public static String gs(@StringRes final int id, String... args) {
        return getAppContext().getString(id, (Object[]) args);
    }
}
