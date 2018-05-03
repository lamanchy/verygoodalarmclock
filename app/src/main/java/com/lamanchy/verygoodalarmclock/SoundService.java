package com.lamanchy.verygoodalarmclock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.Objects;

public class SoundService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    MediaPlayer mediaPlayer;
    SongManager songManager;
    String action;

    @Override
    public void onCreate() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        songManager = new SongManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            Log.i("SoundService", "Empty intent (or none)");
        } else if (!intent.getAction().equals(Enums.STOP_ACTION)) {
            action = intent.getAction();
            putIntoForeground();
            if (!mediaPlayer.isPlaying()) {
                runSong();
            }
        } else {
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mediaPlayer.reset();
        runSong();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopSelf();
    }

    private void putIntoForeground() {
        Intent intent = new Intent(this, SoundService.class);
        intent.setAction(Enums.STOP_ACTION);
        PendingIntent pendingIntent =
                PendingIntent.getService(this, 0, intent, 0);

        Notification notification =
                new Notification.Builder(this)
                        .setContentTitle(getNotificationTitle())
                        .setContentText(songManager.getSongName())
                        .setSmallIcon(R.drawable.alarm_icon)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(1, notification);
    }

    private void runSong() {
        songManager.switchSongs();
        try {
            mediaPlayer.setDataSource(songManager.getSongPath());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.prepare();
        } catch (IOException e) { // backup song
            mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this, R.raw.all_my_tears);
        }
        mediaPlayer.start();
    }

    public String getNotificationTitle() {
        return Objects.equals(action, Enums.MORNIN_PREFIX)
                ? getString(R.string.mornin_message)
                : getString(R.string.evenin_message);
    }
}
