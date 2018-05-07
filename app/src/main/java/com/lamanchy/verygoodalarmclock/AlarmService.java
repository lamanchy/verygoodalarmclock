package com.lamanchy.verygoodalarmclock;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.GregorianCalendar;

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
            Long nextAlarmTime = getNextAlarmTime(preferences);
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
        Long absoluteAlarmTimeMillis = getNextAlarmTime(preferences);

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

    @Nullable
    private Long getNextAlarmTime(CustomPreferences preferences) {
        if (!preferences.getEnabled(Enums.REGULAR_ALARM)
                && !preferences.getEnabled(Enums.ONE_TIME_ALARM)) {
            return null;
        }

        String alarmToInvoke = preferences.getEnabled(Enums.ONE_TIME_ALARM)
                ? Enums.ONE_TIME_ALARM
                : Enums.REGULAR_ALARM;

        Long currentTime = System.currentTimeMillis()
                + new GregorianCalendar().getTimeZone().getRawOffset()
                + 1000L * 60 * 60; // one hour millis to get to UTC
        Long oneDayMillis = 1000L * 60 * 60 * 24;
        Long thisDayMillis = (currentTime / oneDayMillis) * oneDayMillis;
        Long currentDayMillis = currentTime % oneDayMillis;
        Long alarmDayMillis = preferences.getTime(alarmToInvoke) * 60 * 1000L;

        // if morning, subtract song length
        if (preferences.getPrefix().equals(Enums.MORNIN_PREFIX)) {
            alarmDayMillis -= songManager.getSongDuration();
        }

        // to be sure, that RESET doesn't start a bit before alarm,
        // cancels alarm, and then schedule next day alarm
        // (executed alarm blocks this thread for at least 1000ms
        // so it wont reexecute it)
        if (alarmDayMillis < currentDayMillis + 1000) {
            alarmDayMillis += oneDayMillis;
        }

        Long absoluteAlarmTimeMillis = thisDayMillis + alarmDayMillis
                - new GregorianCalendar().getTimeZone().getRawOffset()
                - 1000L * 60 * 60; // one hour millis to get to UTC;

        Long timeToAlarmMillis = absoluteAlarmTimeMillis - System.currentTimeMillis();
        int seconds = (int) (timeToAlarmMillis / 1000) % 60;
        int minutes = (int) ((timeToAlarmMillis / (1000 * 60)) % 60);
        int hours = (int) ((timeToAlarmMillis / (1000 * 60 * 60)) % 24);
        int days = (int) ((timeToAlarmMillis / (1000 * 60 * 60 * 24)));
        Log.i("AlarmService", String.format("Alarm in %d days, %d hours, %d minutes and %d seconds", days, hours, minutes, seconds));

        return absoluteAlarmTimeMillis;
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
