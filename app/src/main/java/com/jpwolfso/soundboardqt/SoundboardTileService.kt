package com.jpwolfso.soundboardqt

import android.content.Context
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.os.Handler
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import java.io.File

open class SoundboardTileService() : TileService() {

    open lateinit var file: File
    open lateinit var myKey: String
    private lateinit var tile: Tile
    private var audioAttributes: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
    private var soundPool: SoundPool? = SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(1).build()

    override fun onStartListening() {
        super.onStartListening()
        tile = qsTile

        when (file.exists()) {
            false -> updateTile(tile, 0)
            true -> updateTile(tile, 1)
        }

    }

    override fun onClick() {
        super.onClick()

            when (file.exists()) {
                true -> {
                    soundPool!!.load(file.path, 1)
                    updateTile(tile, 2)
                    soundPool!!.setOnLoadCompleteListener { soundPool, sampleId, _ ->
                        soundPool!!.play(sampleId, 1F, 1F, 1, 0, 1F)
                    }

                    var mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever!!.setDataSource(file.path)
                    var length: Long = mediaMetadataRetriever!!.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
                    Handler().postDelayed({
                        updateTile(tile, 1)
                    }, length)


                }
                false -> {
                    updateTile(tile, 0)
                    Toast.makeText(this, getString(R.string.toast_tile_help), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateTile(tile: Tile, mode: Int) {
        when (mode) {
            0 -> { tile.state = Tile.STATE_INACTIVE // initial state, no button sound
                tile.label = "Soundboard QT"
            }
            1 -> { tile.icon = Icon.createWithResource(this, R.drawable.ic_start) // button sound is present and not currently playing
                   tile.state = Tile.STATE_ACTIVE
                   tile.label = getSharedPreferences("buttons", Context.MODE_PRIVATE).getString(myKey, "")
            }
            2 -> { tile.icon = Icon.createWithResource(this, R.drawable.ic_stop)  // button sound is present and currently playing
                   tile.state = Tile.STATE_UNAVAILABLE
            }
        }
        tile.updateTile()
    }
}