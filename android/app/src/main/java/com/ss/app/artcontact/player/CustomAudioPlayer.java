package com.ss.app.artcontact.player;

import android.media.MediaPlayer;

import java.io.IOException;

public class CustomAudioPlayer extends MediaPlayer {

    public void load(String path) {
        try {
            setDataSource(path);
            prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
        }
    }

}
