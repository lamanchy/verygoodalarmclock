package com.lamanchy.verygoodalarmclock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.GregorianCalendar;

public class AlarmService extends IntentService {
    private CustomPreferences morninPreferences;
    private CustomPreferences eveninPreferences;
    private AlarmManager alarmManager;

    private Long songDurationMillis = 2 * 60 * 1000L; // default song is 2 minutes long
    private File usedSong;
    private File toBeUsedSong; // toBeUsedOrNotToBeUsed?

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

        usedSong = new File(getFilesDir(), getString(R.string.used_mp3));
        toBeUsedSong = new File(getFilesDir(), getString(R.string.to_be_used_mp3)); // toBeUsedOrNotToBeUsed?
        getSongDuration();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            Log.i("service", "No intent? How that can be?"); // serious question
            return;
        } else if (intent.getAction() == null) {
            Log.i("service", "Intent with no action.");
            return;
        }
        Log.i("alarmtime", "intent come");

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
        if (morninPreferences.getEnabled(Enums.ONE_TIME_OFF)) {
            morninPreferences.setEnabled(Enums.ONE_TIME_OFF, false);
        } else {
            if (morninPreferences.getEnabled(Enums.ONE_TIME_ALARM)) {
                morninPreferences.setEnabled(Enums.ONE_TIME_ALARM, false);
            }

            // if next regular time would be sooner then 3 hours, skip it
            Long nextAlarmTime = getNextAlarmTime(preferences);
            if (nextAlarmTime != null && nextAlarmTime < System.currentTimeMillis() + 3*60*60*1000) {
                morninPreferences.setEnabled(Enums.ONE_TIME_OFF, true);
            }


            Intent intent = new Intent(this, SoundService.class);
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
            alarmDayMillis -= songDurationMillis;
        }

        // to be sure, that RESET doesn't start a bit before alarm,
        // cancels alarm, and then schedule next day alarm
        // (executed alarm blocks this thread for at least 1000ms
        // so it wont reexecute it)
        if (alarmDayMillis < currentDayMillis + 1000) {
            alarmDayMillis += oneDayMillis;
        }

        // if one off is turned on => only regular can be turned on => next one is day after
//        if (preferences.getEnabled(Enums.ONE_TIME_OFF)) {
//            alarmDayMillis += oneDayMillis;
//        }
//      ALARM IS SET, BUT IT DOESN'T SOUND

        Long absoluteAlarmTimeMillis = thisDayMillis + alarmDayMillis
                - new GregorianCalendar().getTimeZone().getRawOffset()
                - 1000L * 60 * 60; // one hour millis to get to UTC;

        Long timeToAlarmMillis = absoluteAlarmTimeMillis - System.currentTimeMillis();
        int seconds = (int) (timeToAlarmMillis / 1000) % 60;
        int minutes = (int) ((timeToAlarmMillis / (1000 * 60)) % 60);
        int hours = (int) ((timeToAlarmMillis / (1000 * 60 * 60)) % 24);
        int days = (int) ((timeToAlarmMillis / (1000 * 60 * 60 * 24)));
        Log.i("alarmtime", String.format("Alarm in %d days, %d hours, %d minutes and %d seconds", days, hours, minutes, seconds));

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

        if (toBeUsedSong.exists()) return; // song is downloaded

        // download it
        InputStream input = null;
        FileOutputStream output = null;
        File file = new File(getFilesDir(), "tmp.mp3");
        Boolean allOk = true;
        try {
            // TODO serve songs randomly on some url
            URL url = new URL("http://mocdobrahudba.lomic.cz/media/uploads/videos/Ane%20Brun%20-%20All%20My%20Tears.mp3");
            URLConnection urlConnection = url.openConnection();

            input = urlConnection.getInputStream();
            output = new FileOutputStream(file);
            byte[] buffer = new byte[1024]; // Adjust if you want
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            allOk = false;
            e.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
            } catch (IOException e) {
                allOk = false;
                e.printStackTrace();
            }
        }
        if (allOk) {
            // atomic operation to be sure fie is ok
            file.renameTo(toBeUsedSong);
        }

        // something went wrong and file does not exists
        if (!toBeUsedSong.exists()) {
            // setup reset in future
            pendingIntent = PendingIntent.getService(this, 0, intent, 0); // 3 hours
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3 * 60 * 60 * 1000, pendingIntent);
            // "set" is used, no need to be exact
        } else {
            // recompute song duration, since it is computed only on create
            getSongDuration();
        }
    }

    private void getSongDuration() {
        File song = toBeUsedSong.exists() ? toBeUsedSong : (
                usedSong.exists() ? usedSong : null
        );
        if (song == null) return; // keep default value

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(song.getPath());
        String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try {
            songDurationMillis = Long.parseLong(duration);
        } catch (NumberFormatException ignore) {
            // keep the default value if error
        }
    }
}
