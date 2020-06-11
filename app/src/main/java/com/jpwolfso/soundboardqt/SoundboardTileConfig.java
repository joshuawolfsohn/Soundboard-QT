package com.jpwolfso.soundboardqt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.documentfile.provider.DocumentFile;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Intent.ACTION_CREATE_DOCUMENT;
import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.EXTRA_TITLE;

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

        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveRecord);

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

        switch (grantResults[0]) {
                case (PackageManager.PERMISSION_GRANTED): {
                    if (requestCode == 1) {
                        exportSound();
                    } else if (requestCode == 2) {
                        importSound();
                    }
                    break;
                }
                case (PackageManager.PERMISSION_DENIED): {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false);
                    if (requestCode == 0) {
                        dialog.setMessage("Soundboard Quick Tile requires permission to record audio")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
                                    }
                                });
                    } else if (requestCode == 1) {
                        dialog.setMessage("Soundboard Quick Tile requires this permission to export this button sound to your device storage")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                    }
                                });
                    } else if (requestCode == 2) {
                        dialog.setMessage("Soundboard Quick Tile requires this permission to import a sound from your device storage")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                                    }
                                });
                    }
                    dialog.create();
                    dialog.show();
                    break;
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
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mediaRecorder.setOutputFile(tempfile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
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
            editText.setVisibility(View.VISIBLE);

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
            Toast.makeText(context, "Button saved successfully", Toast.LENGTH_SHORT).show();

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
            } case (R.id.soundimport): {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                } else {
                    importSound();
                }
                break;
            } case (R.id.soundexport): {
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
        new AlertDialog.Builder(context)
                .setTitle("Please read")
                .setMessage("On the following screen, please browse to the save location and verify the filename ends with the .m4a file extension.")
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ACTION_CREATE_DOCUMENT);
                        intent.setType("audio/*");
                        intent.putExtra(EXTRA_TITLE, getSharedPreferences("buttons", MODE_PRIVATE).getString("button" + buttonversion, "Button " + buttonversion) + ".m4a");
                        startActivityForResult(intent, 1);
                    }
                }).create().show();
    }

    public void importSound() {

        new AlertDialog.Builder(context)
                .setTitle("Please read")
                .setMessage("On the following screen, please browse and open the file to import.")
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ACTION_GET_CONTENT);
                        intent.setType("audio/*");
                        startActivityForResult(intent, 0);
                    }
                }).create().show();

    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {

                try {
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    Files.copy(inputStream,tempfile.toPath(),StandardCopyOption.REPLACE_EXISTING);
                    inputStream.close();

                    recordButton.setVisibility(View.GONE);
                    editText.setVisibility(View.VISIBLE);
                    saveButton.setVisibility(View.VISIBLE);
                    status.setText("Enter a button title and click the Save button to finish importing the selected file");


                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to import button sound :(", Toast.LENGTH_SHORT).show();
                }

            } else if (requestCode == 1) {

                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
                    Files.copy(file.toPath(), outputStream);
                    outputStream.close();
                    Toast.makeText(context, "Button sound exported successfully", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to export button sound :(", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }
}