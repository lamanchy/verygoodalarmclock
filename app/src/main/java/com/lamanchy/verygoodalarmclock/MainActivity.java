package com.lamanchy.verygoodalarmclock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    CustomPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (fuckDoesFuckManufacturerFuckFuckFuckWithFuckFuckingFuckSwipeFuckQuestionFuckMarkFuck()
                && (getIntent().getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) == 0) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(getIntent().getFlags());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            finish();
            startActivity(intent);
        }

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
                updateService();
            }
        };


        if (BeautyManager.getWidth(this) < 600 || fuckDoesFuckManufacturerFuckFuckFuckWithFuckFuckingFuckSwipeFuckQuestionFuckMarkFuck()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

//        I'm gonna keep it here, it's the only test I used, such rarity! :D
        // testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest
//        new CustomPreferences(this, Enums.MORNIN_PREFIX).setTime(Enums.ONE_TIME_ALARM, (int) ((System.currentTimeMillis() / 1000 / 60) % (24 * 60)) + 1 + 2 * 60);
//        new CustomPreferences(this, Enums.MORNIN_PREFIX).setTime(Enums.REGULAR_ALARM, (int) ((System.currentTimeMillis() / 1000 / 60) % (24 * 60)) + 2 + 2 * 60);
//        new CustomPreferences(this, Enums.MORNIN_PREFIX).setEnabled(Enums.ONE_TIME_ALARM, true);
//        new CustomPreferences(this, Enums.MORNIN_PREFIX).setEnabled(Enums.REGULAR_ALARM, true);
//        new CustomPreferences(this, Enums.MORNIN_PREFIX).setEnabled(Enums.ONE_TIME_OFF, false);
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
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (fuckDoesFuckManufacturerFuckFuckFuckWithFuckFuckingFuckSwipeFuckQuestionFuckMarkFuck()) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
        }
    }

    private boolean fuckDoesFuckManufacturerFuckFuckFuckWithFuckFuckingFuckSwipeFuckQuestionFuckMarkFuck() {
        return (Build.MANUFACTURER.equals("LENOVO")); // LENOVO, for now
    }

}