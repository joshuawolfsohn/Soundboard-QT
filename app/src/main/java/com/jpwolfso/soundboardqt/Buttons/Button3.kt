package com.jpwolfso.soundboardqt.Buttons

import android.os.Environment
import com.jpwolfso.soundboardqt.SoundboardTileService
import java.io.File

class Button3() : SoundboardTileService() {
    override var file: File = File(Environment.getDataDirectory(), "/data/com.jpwolfso.soundboardqt/files/recording3")
    override var myKey: String = "button3"
}