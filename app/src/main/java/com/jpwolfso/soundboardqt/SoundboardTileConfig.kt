package com.jpwolfso.soundboardqt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class SoundboardTileConfig() : AppCompatActivity() {

    private lateinit var recordButton: AppCompatImageButton
    private lateinit var status: AppCompatTextView
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var context: Context
    private lateinit var editText: AppCompatEditText
    lateinit var saveButton: AppCompatButton
    private lateinit var tempfile: File
    private lateinit var file: File
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: Editor
    private lateinit var saveas:View

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        //configure layout variables and listeners
        recordButton = findViewById(R.id.imageButton)
        recordButton.setOnClickListener {startRecord()}
        status = findViewById(R.id.status)
        context = this
        editText = findViewById(R.id.editText)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                saveButton.isEnabled = s.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable) {}
        })
        saveButton = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {saveRecord()}

        tempfile = File(filesDir, "temprecording")
        sharedPreferences = getSharedPreferences("buttons", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        if (sharedPreferences.getBoolean("firstrun",true)) {
            welcomeDialog()
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }

        reinitDlg()
    }

    private fun welcomeDialog() = AlertDialog.Builder(context)
            .setTitle("Welcome!")
            .setMessage("To begin, please manually add the 4 sound buttons to your quick settings panel, then you can begin recording sounds!")
            .setNeutralButton(R.string.OK) { dialog, which -> editor.putBoolean("firstrun",false).commit()}
            .create().show()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (grantResults[0]) {
            PackageManager.PERMISSION_GRANTED -> {
                if (requestCode == 1) {
                    exportDialog()
                } else if (requestCode == 2) {
                    importDialog()
                }
            }
            PackageManager.PERMISSION_DENIED -> {
                val dialog = AlertDialog.Builder(this)
                        .setNegativeButton(getString(R.string.exit)) { _, _ -> finish() }
                        .setCancelable(false)
                when (requestCode) {
                    0 -> {
                        dialog.setMessage(getString(R.string.message_permission_audio))
                                .setPositiveButton("OK") { _, _ -> requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0) }
                    }
                    1 -> {
                        dialog.setMessage(getString(R.string.message_permission_storage_export))
                                .setPositiveButton("OK") { _, _ -> requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1) }
                    }
                    2 -> {
                        dialog.setMessage(getString(R.string.message_permission_storage_import))
                                .setPositiveButton("OK") { _, _ -> requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2) }
                    }
                }
                dialog.create()
                dialog.show()
            }
        }
    }

    private fun startRecord() {
        recordButton.setImageResource(R.drawable.ic_stop)
        status.text = getString(R.string.status_recording_in_progress)
        recordButton.setOnClickListener() {endRecord()}
        mediaRecorder = MediaRecorder()
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        mediaRecorder!!.setOutputFile(tempfile.absolutePath)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
        try {
            mediaRecorder!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mediaRecorder!!.start()
    }

    private fun endRecord () {
        mediaRecorder!!.stop()
        mediaRecorder!!.reset()
        mediaRecorder!!.release()
        mediaRecorder = null
        recordButton.setImageResource(R.drawable.ic_record)
        status.text = getString(R.string.status_recording_complete)
        recordButton.setOnClickListener() {startRecord()}
        saveButton.visibility = View.VISIBLE
        editText.visibility = View.VISIBLE
    }

    private fun reinitDlg() {
        saveas = layoutInflater.inflate(R.layout.dialog_saveas,null)
        saveas.findViewById<RadioButton>(R.id.radioButton1).text = sharedPreferences.getString("button1", R.string.sound_1.toString())
        saveas.findViewById<RadioButton>(R.id.radioButton2).text = sharedPreferences.getString("button2", R.string.sound_2.toString())
        saveas.findViewById<RadioButton>(R.id.radioButton3).text = sharedPreferences.getString("button3", R.string.sound_3.toString())
        saveas.findViewById<RadioButton>(R.id.radioButton4).text = sharedPreferences.getString("button4", R.string.sound_4.toString())
    }

    private fun saveRecord() = AlertDialog.Builder(context)
            .setTitle(getString(R.string.title_select_button_write))
            .setView(saveas)
            .setOnDismissListener {reinitDlg()}
            .setPositiveButton(getString(R.string.confirm)) { _, _->
                val buttonName = editText.text.toString()
                when (saveas.findViewById<RadioGroup>(R.id.radioGroup).checkedRadioButtonId) {
                    R.id.radioButton1 -> {
                        editor.putString("button1", buttonName).commit()
                        file = File(filesDir, "recording1")
                    }
                    R.id.radioButton2 -> {
                        editor.putString("button2", buttonName).commit()
                        file = File(filesDir, "recording2")
                    }
                    R.id.radioButton3 -> {
                        editor.putString("button3", buttonName).commit()
                        file = File(filesDir, "recording3")
                    }
                    R.id.radioButton4 -> {
                        editor.putString("button4", buttonName).commit()
                        file = File(filesDir, "recording4")
                    }
                }
                tempfile.renameTo(file)
                Toast.makeText(context, getString(R.string.toast_button_saved_success), Toast.LENGTH_SHORT).show()
                finish()
            }
            .create().show()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_overflow, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.appinfo -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            R.id.soundimport -> {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
                } else {
                    importDialog()
                }
            }
            R.id.soundexport -> {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                } else {
                    exportDialog()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun exportDialog() = AlertDialog.Builder(context)
            .setTitle(getString(R.string.title_select_button_export))
            .setMessage(getString(R.string.message_export_help))
            .setView(saveas)
            .setOnDismissListener {reinitDlg()}
            .setPositiveButton(getString(R.string.proceed)) { _, _ ->
                when (saveas.findViewById<RadioGroup>(R.id.radioGroup).checkedRadioButtonId) {
                    R.id.radioButton1 -> { file = File(filesDir, "recording1") }
                    R.id.radioButton2 -> { file = File(filesDir, "recording2") }
                    R.id.radioButton3 -> { file = File(filesDir, "recording3") }
                    R.id.radioButton4 -> { file = File(filesDir, "recording4") }
                }
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = "audio/*"
                startActivityForResult(intent, 1)
            }.create().show()

    private fun importDialog() = AlertDialog.Builder(context)
            .setTitle(getString(R.string.title_please_read))
            .setMessage(getString(R.string.message_help_import))
            .setPositiveButton(getString(R.string.proceed)) { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "audio/*"
                startActivityForResult(intent, 0)
            }.create().show()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 0) {
                try {
                    val inputStream = contentResolver.openInputStream(data!!.data!!)
                    if (inputStream!!.available() >= 1000000) { // SoundPool does not work with files greater than 1 MB
                        Toast.makeText(this,getString(R.string.toast_error_large_file),Toast.LENGTH_LONG).show()
                        return
                    }
                    FileOutputStream(tempfile).use { stream -> inputStream.copyTo(stream)}
                    inputStream.close()
                    recordButton.visibility = View.GONE
                    editText.visibility = View.VISIBLE
                    saveButton.visibility = View.VISIBLE
                    status.text = getString(R.string.status_import_recording)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, getString(R.string.toast_error_import_failed), Toast.LENGTH_SHORT).show()
                }
            } else if (requestCode == 1) {
                try {
                    val outputStream = contentResolver.openOutputStream(data!!.data!!)
                    FileInputStream(file).use { stream -> stream.copyTo(outputStream!!)}
                    outputStream!!.close()
                    Toast.makeText(context, getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}