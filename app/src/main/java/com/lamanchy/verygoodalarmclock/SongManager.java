package com.lamanchy.verygoodalarmclock;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class SongManager {
    private class Song {
        File name;
        File song;

        private Song(Context context, String type) {
            song = new File(context.getFilesDir(), type + ".mp3");
            name = new File(context.getFilesDir(), type + ".name");
        }

        Boolean exists() {
            return song.exists() && name.exists();
        }

        @NonNull
        private Long getDuration() {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(song.getPath());
            String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            try {
                return Long.parseLong(duration);
            } catch (NumberFormatException ignore) {
                return defaultDurationMillis;
            }
        }

        @NonNull
        private String getName() {
            String result = getNameFromFile(name);
            if (result == null) {
                Log.e("SongManager", "Shit fuck can't read from local file?");
                return "Name error";
            }
            return result;
        }

        @NonNull
        private String getPath() {
            return song.getPath();
        }

        private void switchTo(Song other) {
            other.set(song, getName());
            clean();
        }

        private Boolean set(File file, @NonNull String newName) {
            if (name.exists() && !name.delete()) return false;
            if (song.exists() && !song.delete()) return false;
            if (!writeNameToFile(name, newName)) {
                clean();
                return false;
            }
            if (!file.renameTo(song)) {
                clean();
                return false;
            }
            Log.i("SongManager", "Setting of song " + newName + " successful");
            return true;
        }

        private void clean() {
            if (name.exists() && !name.delete()) {
                Log.e("SongManager", "Fuck this shit if delete in clean does not work");
            }
            if (song.exists() && !song.delete()) {
                Log.e("SongManager", "Fuck this shit if delete in clean does not work");
            }
        }
    }

    private Context context;
    private Song usedSong;
    private Song toBeUsedSong; // toBeUsedOrNotToBeUsed?
    private Long defaultDurationMillis = 2 * 60 * 1000L; // default song is 2 minutes long

    public SongManager(Context context) {
        this.context = context;
        usedSong = new Song(context, "used_song");
        toBeUsedSong = new Song(context, "to_be_used_song");
    }

    @Nullable
    private Song getSong() {
        return toBeUsedSong.exists() ? toBeUsedSong : (
                usedSong.exists() ? usedSong : null
        );
    }

    public Long getSongDuration() {
        Song song = getSong();
        if (song == null) return defaultDurationMillis;
        return song.getDuration();
    }

    public String getSongName() {
        Song song = getSong();
        if (song == null) return context.getString(R.string.default_song_name);
        return song.getName();
    }

    public String getSongPath() throws IOException {
        Song song = getSong();
        if (song == null) throw new IOException("Song does not exist");
        return song.getPath();
    }

    public void switchSongs() {
        if (toBeUsedSong.exists()){
            toBeUsedSong.switchTo(usedSong);
        }
    }

    public Boolean isDownloaded() {
        if (toBeUsedSong.exists()) {
            Log.i("SongManager", "Song already downloaded");
            return true;
        }

        // download it
        InputStream input = null;
        FileOutputStream output = null;
        File file = new File(context.getFilesDir(), "tmp.mp3");
        Boolean allOk = true;
        String name = null;
        try {
            URL url = new URL("http://mocdobrahudba.lomic.cz/random_song/");
            URLConnection urlConnection = url.openConnection();
            name = urlConnection.getHeaderField("Content-Disposition")
                    .substring("attachment; filename=".length());

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
        if (!allOk || name == null) {
            Log.w("SongManager", "Downloading error");
            return false;
        }

        return toBeUsedSong.set(file, name);
    }

    @Nullable
    private String getNameFromFile(File file) {
        String result = null;
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream))
        ) {
            result = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private Boolean writeNameToFile(File file, String name) {
        try (
                FileOutputStream output = new FileOutputStream(file);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output))
        ) {
            writer.write(name);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
