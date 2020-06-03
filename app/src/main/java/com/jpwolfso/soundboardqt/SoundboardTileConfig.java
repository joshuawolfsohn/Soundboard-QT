package com.jpwolfso.soundboardqt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class SoundboardTileConfig extends AppCompatActivity {

    AppCompatImageButton recordButton;
    TextView status;
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    Context context;
    EditText editText;
    Button saveButton;
    File tempfile;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        recordButton = findViewById(R.id.imageButton);
        recordButton.setOnClickListener(startRecord);
        status = findViewById(R.id.status);
        context = this;
        editText = findViewById(R.id.editText);
        saveButton = findViewById(R.id.saveButton);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},0);
        }

        file = new File(getFilesDir(),"recording");
        tempfile = new File(getFilesDir(),"temprecording");

        if (file.exists()) {
            file.delete();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            switch (grantResults[0]) {
                case (PackageManager.PERMISSION_GRANTED): {
                    break;
                } case (PackageManager.PERMISSION_DENIED): {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Soundboard Quick Tile requires permission to record audio")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},0);
                                }
                            })
                            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .create();
                    dialog.show();
                    break;

                }
            }
        }
    }

    View.OnClickListener startRecord = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            recordButton.setImageResource(R.drawable.ic_stop);
            status.setText("Recording... Click to end recording");
            recordButton.setOnClickListener(endRecord);

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(tempfile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaRecorder.start();

        }
    };

    View.OnClickListener endRecord = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            recordButton.setImageResource(R.drawable.ic_record);
            status.setText("Enter a button title and click the Save button to save the quick tile, or click above to record again");
            recordButton.setOnClickListener(startRecord);
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setOnClickListener(saveRecord);
            editText.setVisibility(View.VISIBLE);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0) {
                        saveButton.setEnabled(true);
                    } else {
                        saveButton.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });



        }
    };

    View.OnClickListener saveRecord = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            SharedPreferences sharedPreferences = getSharedPreferences("buttons",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String buttonName = editText.getText().toString();

            if (buttonName != null) {
                editor.putString("button1", buttonName).commit();
            }

            tempfile.renameTo(file);

            finish();
        }
    };
}