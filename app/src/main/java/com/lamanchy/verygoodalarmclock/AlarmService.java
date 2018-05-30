package com.lamanchy.verygoodalarmclock;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

public class AlarmService extends IntentService {
    private CustomPreferences morninPreferences;
    private CustomPreferences eveninPreferences;
    private AlarmManager alarmManager;
    private SongManager songManager;

    public AlarmService() {
        super("Alarm");
        setIntentRedelivery(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        morninPreferences = new CustomPreferences(this, Enums.MORNIN_PREFIX);
        eveninPreferences = new CustomPreferences(this, Enums.EVENIN_PREFIX);
        songManager = new SongManager(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case Enums.RESET_ACTION:
                resetAlarms();
                break;
            case Enums.MORNIN_PREFIX:
                runAlarm(morninPreferences);
                break;
            case Enums.EVENIN_PREFIX:
                runAlarm(eveninPreferences);
                break;
        }
    }

    private void resetAlarms() {
        setAlarmFor(morninPreferences);
        setAlarmFor(eveninPreferences);
        downloadSong();
    }

    private void runAlarm(CustomPreferences preferences) {
        if (preferences.getEnabled(Enums.ONE_TIME_OFF)) {
            preferences.setEnabled(Enums.ONE_TIME_OFF, false);
        } else {
            if (preferences.getEnabled(Enums.ONE_TIME_ALARM)) {
                preferences.setEnabled(Enums.ONE_TIME_ALARM, false);
            }

            // if next regular time would be sooner then 3 hours, skip it
            Long nextAlarmTime = songManager.getNextAlarmTime(preferences, false);
            if (nextAlarmTime != null && nextAlarmTime < System.currentTimeMillis() + 3 * 60 * 60 * 1000) {
                preferences.setEnabled(Enums.ONE_TIME_OFF, true);
            }


            Intent intent = new Intent(this, SoundService.class);
            intent.setAction(preferences.getPrefix());
            startService(intent);
        }


        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        } finally {
            resetAlarms();
        }
    }


    private void setAlarmFor(CustomPreferences preferences) {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(preferences.getPrefix());
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        // cancel intent if exists and recreate it if needed
        if (pendingIntent != null) {
            pendingIntent.cancel();
        }

        Log.i("alarmtime", "setting alarm " + preferences.getPrefix());
        Long absoluteAlarmTimeMillis = songManager.getNextAlarmTime(preferences, false);

        // alarms are off, do noting
        if (absoluteAlarmTimeMillis == null) {
            return;
        }

        pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, absoluteAlarmTimeMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, absoluteAlarmTimeMillis, pendingIntent);
        }

    }

    private void downloadSong() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(Enums.RESET_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        // cancel intent if exists and recreate later it if needed
        if (pendingIntent != null) {
            pendingIntent.cancel();
        }

        if (songManager.isDownloaded()) return; // song is downloaded

        // setup reset in future
        pendingIntent = PendingIntent.getService(this, 0, intent, 0); // 3 hours
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3 * 60 * 60 * 1000, pendingIntent);
        // "set" is used, no need to be exact
    }

}
