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
    private lateinit var buttonversion: String

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
        saveButton.setOnClickListener(saveRecord)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }

        val componentName = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)
        buttonversion = componentName.shortClassName.substring(15)
        file = File(filesDir, "recording$buttonversion")
        tempfile = File(filesDir, "temprecording$buttonversion")
        sharedPreferences = getSharedPreferences("buttons", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

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
                        .setNegativeButton("Exit") { _, _ -> finish() }
                        .setCancelable(false)
                when (requestCode) {
                    0 -> {
                        dialog.setMessage("Soundboard Quick Tile requires permission to record audio")
                                .setPositiveButton("OK") { _, _ -> requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0) }
                    }
                    1 -> {
                        dialog.setMessage("Soundboard Quick Tile requires this permission to export this button sound to your device storage")
                                .setPositiveButton("OK") { _, _ -> requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1) }
                    }
                    2 -> {
                        dialog.setMessage("Soundboard Quick Tile requires this permission to import a sound from your device storage")
                                .setPositiveButton("OK") { _, _ -> requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2) }
                    }
                }
                dialog.create()
                dialog.show()
            }
        }
    }

    private fun startRecord() {
        if (file.exists()) {
            file.delete()
        }
        editor.remove("button$buttonversion").commit()
        recordButton.setImageResource(R.drawable.ic_stop)
        status.text = "Recording... Click to end recording"
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
        status.text = "Enter a button title and click the Save button to save the quick tile, or click above to record again"
        recordButton.setOnClickListener() {startRecord()}
        saveButton.visibility = View.VISIBLE
        editText.visibility = View.VISIBLE
    }

    var saveRecord = View.OnClickListener {
        val buttonName = editText.text.toString()
        editor.putString("button$buttonversion", buttonName).commit()
        tempfile.renameTo(file)
        Toast.makeText(context, "Button saved successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

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
            .setTitle("Please read")
            .setMessage("On the following screen, please browse to the save location and verify the filename ends with the .m4a file extension.")
            .setPositiveButton("Proceed") { _, _ ->
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = "audio/*"
                intent.putExtra(Intent.EXTRA_TITLE, getSharedPreferences("buttons", Context.MODE_PRIVATE).getString("button$buttonversion", "Button $buttonversion") + ".m4a")
                startActivityForResult(intent, 1)
            }.create().show()

    private fun importDialog() = AlertDialog.Builder(context)
            .setTitle("Please read")
            .setMessage("On the following screen, please browse and open the file to import.")
            .setPositiveButton("Proceed") { _, _ ->
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
                        Toast.makeText(this,"Error: Imported file is too large",Toast.LENGTH_LONG).show()
                        return
                    }
                    FileOutputStream(tempfile).use { stream -> inputStream.copyTo(stream)}
                    inputStream.close()
                    recordButton.visibility = View.GONE
                    editText.visibility = View.VISIBLE
                    saveButton.visibility = View.VISIBLE
                    status.text = "Enter a button title and click the Save button to finish importing the selected file"
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to import button sound :(", Toast.LENGTH_SHORT).show()
                }
            } else if (requestCode == 1) {
                try {
                    val outputStream = contentResolver.openOutputStream(data!!.data!!)
                    FileInputStream(file).use { stream -> stream.copyTo(outputStream!!)}
                    outputStream!!.close()
                    Toast.makeText(context, "Button sound exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to export button sound :(", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}