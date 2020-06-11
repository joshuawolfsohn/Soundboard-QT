package com.jpwolfso.soundboardqt.Buttons

import android.os.Environment
import com.jpwolfso.soundboardqt.SoundboardTileService
import java.io.File

class Button2() : SoundboardTileService() {
    override var file: File = File(Environment.getDataDirectory(), "/data/com.jpwolfso.soundboardqt/files/recording2")
    override var myKey: String = "button2"
}