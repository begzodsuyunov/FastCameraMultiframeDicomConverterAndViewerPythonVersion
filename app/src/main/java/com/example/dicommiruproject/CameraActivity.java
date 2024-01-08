package com.example.dicommiruproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, android.hardware.Camera.PreviewCallback {

    private static final int PERMISSION_REQUEST_CODE = 1;

    final Handler handlerFrame = new Handler(Looper.getMainLooper());

    private android.hardware.Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private int frameCount = 0;
    private boolean shouldStopCapturingFrames = false;

    private static final int CAPTURE_DURATION = 1000;  // Duration in milliseconds
    private HandlerThread frameCaptureThread;
    private Handler frameCaptureHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        if (checkCameraHardware()) {
            camera = android.hardware.Camera.open();
        } else {
            Toast.makeText(this, "No camera found", Toast.LENGTH_SHORT).show();
            finish();
        }

        Button startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFrames();
            }
        });

        Button patientInfoButton = findViewById(R.id.patientInfoBtn);
        patientInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show patient info dialog
                showUpdateDialog();

            }
        });
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }


//        mergeDicomBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mergeDicomFiles();
//            }
//        });
    }

    // ...

    private void mergeDicomFiles() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed with file operations
            Python py = Python.getInstance();
            PyObject pyo = py.getModule("yolo_module");


            // Update the directory path to point to the correct folder
            String directoryPath = "/storage/emulated/0/Download/DCMFiles/";
            File directory = new File(directoryPath);

            // Check if the directory exists, if not, create it
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    Log.d("CameraApp", "Directory created: " + directoryPath);
                } else {
                    Log.e("CameraApp", "Failed to create directory: " + directoryPath);
                    showToast("Failed to create directory for DICOM files.");
                    return;
                }
            }


            // Update the directory path to point to the correct folder
            PyObject obj = pyo.callAttr("create_multiframe_dicom",
                    "/storage/emulated/0/Download/DicomJpg/",  // Correct directory path
                    directoryPath);

            if (Objects.requireNonNull(obj).toJava(String.class).equals("Success")) {
                showToast("DICOM files merged successfully.");
            } else {
                showToast("Error merging DICOM files.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with file operations
                mergeDicomFiles();
            } else {
                // Permission denied, show a message or handle accordingly
                showToast("Permission denied. Cannot perform file operations.");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    private boolean checkCameraHardware() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void captureFrames() {
        frameCount = 0; // Reset frame count
        shouldStopCapturingFrames = false; // Reset the flag
// Create a new HandlerThread for frame capturing
        frameCaptureThread = new HandlerThread("FrameCaptureThread");
        frameCaptureThread.start();

        // Get the handler from the new thread
        frameCaptureHandler = new Handler(frameCaptureThread.getLooper());
        // Schedule the first frame capture immediately
// Schedule the first frame capture immediately
        frameCaptureHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                captureFrameAndScheduleNext();
            }
        }, 0);

        // Schedule a task to stop capturing frames after 1000ms
        // Schedule a task to stop capturing frames after 1000ms
        frameCaptureHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopCapturingFrames();
            }
        }, 900);
    }
    private void stopFrameCaptureThread() {
        shouldStopCapturingFrames = true;

        // Cleanup and stop the frame capture thread
        if (frameCaptureHandler != null) {
            frameCaptureHandler.removeCallbacksAndMessages(null);
        }

        if (frameCaptureThread != null) {
            frameCaptureThread.quitSafely();
            try {
                // Ensure that the thread is properly terminated before moving forward
                frameCaptureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void captureFrameAndScheduleNext() {
        // Check the flag before capturing the frame
        if (!shouldStopCapturingFrames) {
            // Capture frame
            camera.setOneShotPreviewCallback(new android.hardware.Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
                    saveFrame(data);
                }
            });

            // Schedule the next frame capture with a consistent delay of 45ms
            frameCaptureHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    captureFrameAndScheduleNext();
                }
            }, 65);
        }
    }

    private void stopCapturingFrames() {
        // Stop capturing frames or perform any necessary cleanup
        shouldStopCapturingFrames = true;

        // Cleanup and stop the frame capture thread
        if (frameCaptureHandler != null) {
            frameCaptureHandler.removeCallbacksAndMessages(null);
            frameCaptureHandler.getLooper().quitSafely();
            mergeDicomFiles();
        }

        if (frameCaptureThread != null) {
            try {
                // Ensure that the thread is properly terminated before moving forward
                frameCaptureThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }



    private void saveFrame(byte[] data) {
        // Save frame to a file in the specified location
        File mediaStorageDirJpg = new File(Environment.getExternalStorageDirectory(), "/Download/DicomJpg");
        if (!mediaStorageDirJpg.exists()) {
            mediaStorageDirJpg.mkdirs();
        }

        String frameFilePath = mediaStorageDirJpg.getPath() + File.separator + System.currentTimeMillis() + ".jpg";
        File frameFile = new File(frameFilePath);

        try {
            // Convert YUV NV21 to RGB
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] jpegData = out.toByteArray();

            // Save the JPEG data to the file
            FileOutputStream fos = new FileOutputStream(frameFile);
            fos.write(jpegData);
            fos.close();

            Log.d("CameraApp", "Frame saved: " + frameFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
// Check frame size
//        int frameWidth = camera.getParameters().getPreviewSize().width;
//        int frameHeight = camera.getParameters().getPreviewSize().height;
//        Log.d("CameraApp", "Frame Size: " + frameWidth + "x" + frameHeight);
//
//        // Increment frame count
//        frameCount++;
//
//        // Log the frame count whenever you need it
//        Log.d("CameraApp", "Frame Count: " + frameCount);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        android.hardware.Camera.Parameters parameters = camera.getParameters();
        camera.setDisplayOrientation(90); // Adjust the orientation as needed
        parameters.setPreviewFormat(ImageFormat.NV21);  // or other supported format
        parameters.setPreviewSize(1280, 720);  // Set the resolution to Full HD
        camera.setParameters(parameters);

        camera.startPreview();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.startPreview();
        camera.setPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    private void updatePatientInfo(String patientName, String patientAge, String patientId, String patientSex,
                                   String formattedDob, String patientAddress, String institutionName,
                                   String manufacturer, String manufacturerModelName, String referringPhysicianName,
                                   String formattedStudyDate, String studyDescription, String studyID,
                                   String formattedSeriesDate) {
        Python py = Python.getInstance();
        PyObject pyo = py.getModule("yolo_module"); // Replace with your Python module name

        // Call your Python function to update patient information
        PyObject resultUpdate = pyo.callAttr("update_patient_info", patientName, patientAge, patientId, patientSex,
                formattedDob, patientAddress, institutionName, manufacturer, manufacturerModelName,
                referringPhysicianName, formattedStudyDate, studyDescription, studyID, formattedSeriesDate);

        // Get the result message from the update function
        String resultMessageUpdate = resultUpdate.toJava(String.class);

        // Show a toast message based on the result of updating patient information
        showToast(resultMessageUpdate);
    }

    private void showUpdateDialog() {
        // Create a dialog with two EditText fields
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Patient Information");

        // Set up the layout for the dialog
        View view = getLayoutInflater().inflate(R.layout.dialog_patient_info, null);
        builder.setView(view);

        EditText patientNameEditText = view.findViewById(R.id.editTextPatientName);
        EditText patientIdEditText = view.findViewById(R.id.editTextPatientId);
        EditText patientSexEditText = view.findViewById(R.id.editTextPatientSex);
        EditText patientAgeEditText = view.findViewById(R.id.editTextPatientAge);
        EditText patientDobEditText = view.findViewById(R.id.editTextPatientDob);
        EditText patientAddressEditText = view.findViewById(R.id.editTextPatientAddress);
        EditText institutionNameEditText = view.findViewById(R.id.editTextInstitutionName);
        EditText manufacturerEditText = view.findViewById(R.id.editTextManufacturer);
        EditText manufacturerModelNameEditText = view.findViewById(R.id.editTextManufacturerModelName);
        EditText referringPhysicianNameEditText = view.findViewById(R.id.editTextReferringPhysicianName);
        EditText studyDateEditText = view.findViewById(R.id.editTextStudyDate);
        EditText studyDescriptionEditText = view.findViewById(R.id.editTextStudyDescription);
        EditText studyIDEditText = view.findViewById(R.id.editTextStudyID);
        EditText seriesDateEditText = view.findViewById(R.id.editTextSeriesDate);


        // Set up the positive button to update patient information
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get the values from the EditText fields
                String updatedPatientName = patientNameEditText.getText().toString();
                String updatedPatientId = patientIdEditText.getText().toString();
                String updatedPatientSex = patientSexEditText.getText().toString();
                String updatedPatientAge = patientAgeEditText.getText().toString();
                String updatedPatientDob = patientDobEditText.getText().toString();
                String updatedPatientAddress = patientAddressEditText.getText().toString();
                String updatedInstitutionName = institutionNameEditText.getText().toString();
                String updatedManufacturer = manufacturerEditText.getText().toString();
                String updatedManufacturerModelName = manufacturerModelNameEditText.getText().toString();
                String updatedReferringPhysicianName = referringPhysicianNameEditText.getText().toString();
                String updatedStudyDate = studyDateEditText.getText().toString();
                String updatedStudyDescription = studyDescriptionEditText.getText().toString();
                String updatedStudyID = studyIDEditText.getText().toString();
                String updatedSeriesDate = seriesDateEditText.getText().toString();


                updatePatientInfo(updatedPatientName, updatedPatientAge, updatedPatientId, updatedPatientSex, updatedPatientDob, updatedPatientAddress, updatedInstitutionName,
                        updatedManufacturer, updatedManufacturerModelName, updatedReferringPhysicianName,
                        updatedStudyDate, updatedStudyDescription, updatedStudyID, updatedSeriesDate);
            }
        });

        // Set up the negative button to cancel the dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}