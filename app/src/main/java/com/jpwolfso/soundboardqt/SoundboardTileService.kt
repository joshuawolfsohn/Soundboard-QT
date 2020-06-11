package com.jpwolfso.soundboardqt

import android.content.Context
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.net.Uri
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import java.io.File

open class SoundboardTileService() : TileService() {

    open lateinit var file: File
    open lateinit var myKey: String
    private lateinit var tile: Tile
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartListening() {
        super.onStartListening()
        tile = qsTile
        tile.label = getSharedPreferences("buttons", Context.MODE_PRIVATE).getString(myKey, "Soundboard")
        when (file.exists()) {
            true -> updateTile(tile, 3)
            false -> updateTile(tile, 2)
        }
    }

    override fun onClick() {
        super.onClick()

        when (file.exists()) {
            true -> {
                when (mediaPlayer) {
                    null -> {
                        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file))
                        mediaPlayer!!.setOnCompletionListener { updateTile(tile, 1) }
                    }
                }

                when (mediaPlayer!!.isPlaying) {
                    false -> {
                        updateTile(tile, 0)
                        mediaPlayer!!.start()
                    }
                    true -> updateTile(tile, 1)
                }
            }
            false -> {
                updateTile(tile, 2)
                Toast.makeText(this, "Please click and hold on the Soundboard tile to configure", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTile(tile: Tile, mode: Int) {
        when (mode) {
            0 -> tile.icon = Icon.createWithResource(this, R.drawable.ic_stop) // when starting sound, set icon to stop
            1 -> { tile.icon = Icon.createWithResource(this, R.drawable.ic_start)  // when stopping sound, set icon to start
                    mediaPlayer!!.reset()
                    mediaPlayer!!.release()
            }
            2 -> tile.state = Tile.STATE_INACTIVE // no sound file
            3 -> tile.state = Tile.STATE_ACTIVE // sound file present
        }
        tile.updateTile()
    }
}