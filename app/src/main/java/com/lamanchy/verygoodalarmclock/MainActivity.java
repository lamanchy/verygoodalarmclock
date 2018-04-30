package com.lamanchy.verygoodalarmclock;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    CustomPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {       // Important, otherwise there'd be a new Fragment created with every orientation change
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.morning_fragment,
                                AlarmFragment.newInstance(Enums.MORNIN_PREFIX), // https://www.youtube.com/watch?v=la0eUKD9kNw
                                null)
                        .replace(R.id.evening_fragment,
                                AlarmFragment.newInstance(Enums.EVENIN_PREFIX), // https://www.youtube.com/watch?v=3dZUDvIXeXI
                                null)                             // https://www.youtube.com/watch?v=VDCa-OoDeUI
                        .commit();                                    // ( mornin' and evenin' songs :D )
            }
        }


        preferences = new CustomPreferences(this, null);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.i("activity updated", key);
                updateService();
            }
        };

        // testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest
        new CustomPreferences(this, Enums.MORNIN_PREFIX).setTime(Enums.ONE_TIME_ALARM, (int) ((System.currentTimeMillis() / 1000 / 60) % (24 * 60)) + 3 + 2 * 60);
        new CustomPreferences(this, Enums.MORNIN_PREFIX).setTime(Enums.REGULAR_ALARM, (int) ((System.currentTimeMillis() / 1000 / 60) % (24 * 60)) + 4 + 2 * 60);
        new CustomPreferences(this, Enums.MORNIN_PREFIX).setEnabled(Enums.ONE_TIME_ALARM, true);
        // testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest

        updateService();
    }

    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void updateService() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(Enums.RESET_ACTION);
        startService(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }
}