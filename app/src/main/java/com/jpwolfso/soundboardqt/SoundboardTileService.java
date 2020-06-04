package com.jpwolfso.soundboardqt;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class SoundboardTileService extends TileService {
    public SoundboardTileService() {
    }

    Tile tile;
    MediaPlayer mediaPlayer;
    File file;
    String buttonversion;

    @Override
    public void onTileAdded() {
        super.onTileAdded();

    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        buttonversion = this.getClass().getSimpleName().substring(6);

        tile = this.getQsTile();
        file = new File(getFilesDir(), "recording" + buttonversion);
        tile.setLabel(getSharedPreferences("buttons", MODE_PRIVATE).getString("button" + buttonversion, "Button " + buttonversion));


        if (file.exists()) {
            updateTile(tile, 3);
        } else {
            updateTile(tile, 2);
        }

    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();


        if (file.exists()) {

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file));
            }

            if (!mediaPlayer.isPlaying()) {
                updateTile(tile, 0);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        updateTile(tile, 1);
                    }
                });
            } else if (mediaPlayer.isPlaying()) {
                updateTile(tile, 1);
            }

        } else {
            updateTile(tile, 2);
            Toast.makeText(this, "Please click and hold on the Soundboard tile to configure", Toast.LENGTH_LONG).show();
        }
    }

    public void updateTile(Tile tile, int mode) {
        if (mode == 0) { // when starting sound, set icon to stop
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stop));
        } else if (mode == 1) { // when stopping sound, set icon to start
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_start));
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        } else if (mode == 2) { // no sound file
            tile.setState(Tile.STATE_INACTIVE);
        } else if (mode == 3) { // sound file present
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }

}