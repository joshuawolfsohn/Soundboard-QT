package com.jpwolfso.soundboardqt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.rtp.AudioStream;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SoundboardTileConfig extends AppCompatActivity {

    AppCompatImageButton recordButton;
    TextView status;
    MediaRecorder mediaRecorder;
    Context context;
    EditText editText;
    Button saveButton;
    File tempfile;
    File file;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String buttonversion;

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

        ComponentName componentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        buttonversion = componentName.getShortClassName().substring(15);

        setTitle("Soundboard Config - Button " + buttonversion);

        file = new File(getFilesDir(),"recording" + buttonversion);
        tempfile = new File(getFilesDir(),"temprecording" + buttonversion);

        sharedPreferences = getSharedPreferences("buttons",MODE_PRIVATE);
        editor = sharedPreferences.edit();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0) {
            switch (grantResults[0]) {
                case (PackageManager.PERMISSION_GRANTED): {
                    break;
                }
                case (PackageManager.PERMISSION_DENIED): {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Soundboard Quick Tile requires permission to record audio")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
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
        } else if (requestCode == 1) {
            switch (grantResults[0]) {
                case (PackageManager.PERMISSION_GRANTED): {
                    exportSound();
                    break;
                }
                case (PackageManager.PERMISSION_DENIED): {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Soundboard Quick Tile requires permission to export this button sound to your device storage")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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

            if (file.exists()) {
                file.delete();
            }

            editor.remove("button" + buttonversion).commit();

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

            String buttonName = editText.getText().toString();

            if (buttonName != null) {
                editor.putString("button" + buttonversion, buttonName).commit();
            }

            tempfile.renameTo(file);

            finish();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_overflow, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.appinfo): {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                break;
            } case (R.id.export): {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    exportSound();
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void exportSound() {
        try {
            File exportfile = new File(Environment.getExternalStorageDirectory() + "/soundboardqt", "button" + buttonversion + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".3gpp");
            exportfile.getParentFile().mkdirs();
            exportfile.createNewFile();

            FileChannel input = new FileInputStream(file).getChannel();
            FileChannel output = new FileOutputStream(exportfile).getChannel();
            input.transferTo(0,input.size(),output);

            input.close();
            output.close();

            Toast.makeText(context, "Button " + buttonversion + " sound backed up to " + file.toString(), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(context, String.valueOf(e), Toast.LENGTH_SHORT).show();
        }
    }
}