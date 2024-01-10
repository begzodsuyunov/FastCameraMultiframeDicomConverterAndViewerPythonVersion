package com.example.dicommiruproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    ImageButton diagnose;
    ImageButton camera;
    ImageButton dicomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
        }

        diagnose = findViewById(R.id.diagnose);
        camera = findViewById(R.id.cameraOpen);
        dicomList = findViewById(R.id.listDicom);

        diagnose.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), DiagnoseActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);

        });

        camera.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);

        });


        dicomList.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), FileListActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);

        });
    }
}