package com.lamanchy.verygoodalarmclock;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public class SoundService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    MediaPlayer mediaPlayer;
    private File usedSong;
    private File toBeUsedSong; // toBeUsedOrNotToBeUsed?

    @Override
    public void onCreate() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        usedSong = new File(getFilesDir(), getString(R.string.used_mp3));
        toBeUsedSong = new File(getFilesDir(), getString(R.string.to_be_used)); // toBeUsedOrNotToBeUsed?
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        runSong();
        return START_STICKY;
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

    private void runSong() {
        if (toBeUsedSong.exists()) {
            usedSong.delete();
            toBeUsedSong.renameTo(usedSong);
        }
        try {
            mediaPlayer.setDataSource(usedSong.getPath());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.prepare();
        } catch (IOException e) {
            int resID=getResources().getIdentifier("all_my_tears.mp3", "raw", getPackageName());
            mediaPlayer = MediaPlayer.create(this,resID);
        }
        mediaPlayer.start();
    }
}
