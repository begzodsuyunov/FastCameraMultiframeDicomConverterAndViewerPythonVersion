package com.example.dicommiruproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dicomhero.api.CodecFactory;
import com.dicomhero.api.ColorTransformsFactory;
import com.dicomhero.api.DataSet;
import com.dicomhero.api.DrawBitmap;
import com.dicomhero.api.Image;
import com.dicomhero.api.Memory;
import com.dicomhero.api.PatientName;
import com.dicomhero.api.PipeStream;
import com.dicomhero.api.StreamReader;
import com.dicomhero.api.TagId;
import com.dicomhero.api.TransformsChain;
import com.dicomhero.api.VOILUT;
import com.dicomhero.api.drawBitmapType_t;
import com.example.dicommiruproject.util.PushToDicomheroPipe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DiagnoseActivity extends AppCompatActivity {

    private ImageView mImageView; // Used to display the image
    private TextView mTextView;  // Used to display the patient name
    private int currentFrameIndex = 0; // Maintain the current frame index
    private Button btnNext;
    private Button btnPrevious;
    private DataSet loadDataSet; // Declare loadDataSet at the class level

    private TextView patientNameTextView;
    private TextView patientIdTextView;
    private TextView patientSexTextView;
    private TextView patientAgeTextView;
    private TextView patientDobTextView;
    private TextView studyDateTextView;
    private TextView studyDescTextView;
    private TextView seriesDateTextView;
    private TextView seriesTimeTextView;

    /*

    Called when the user clicks on "Load DICOM file"

     */
    public void loadDicomFileClicked(View view) {

        // Let's use the Android File dialog. It will return an answer in the future, which we
        // get via onActivityResult()
        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select a DICOM file"), 123);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // First thing: load the Imebra library
        System.loadLibrary("dicomhero6");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnose);

        // We will use the ImageView widget to display the DICOM image
        mImageView = findViewById(R.id.imageView);
        mTextView = findViewById(R.id.textView);

        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);


        patientNameTextView = findViewById(R.id.patientNameTextView);
        patientIdTextView = findViewById(R.id.patientIdTextView);
        patientSexTextView = findViewById(R.id.patientSexTextView);
        patientAgeTextView = findViewById(R.id.patientAgeTextView);
        patientDobTextView = findViewById(R.id.patientDobTextView);
        studyDateTextView = findViewById(R.id.studyDateTextView);
        studyDescTextView = findViewById(R.id.studyDescTextView);
        seriesDateTextView = findViewById(R.id.seriesDateTextView);


        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNextFrame();
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreviousFrame();
            }
        });



    }
    private void showNextFrame() {
        if (currentFrameIndex < 5 - 1) {
            currentFrameIndex++;
            updateImageView();
        }
    }

    private void showPreviousFrame() {
        if (currentFrameIndex > 0) {
            currentFrameIndex--;
            updateImageView();
        }
    }

    private void updateImageView() {
        // Retrieve the image for the current frame
        Image dicomImage = loadDataSet.getImageApplyModalityTransform(currentFrameIndex);

        // Use a DrawBitmap to build a stream of bytes that can be handled by the
        // Android Bitmap class.
        TransformsChain chain = new TransformsChain();

        if (ColorTransformsFactory.isMonochrome(dicomImage.getColorSpace())) {
            VOILUT voilut = new VOILUT(VOILUT.getOptimalVOI(dicomImage, 0, 0, dicomImage.getWidth(), dicomImage.getHeight()));
            chain.addTransform(voilut);
        }
        DrawBitmap drawBitmap = new DrawBitmap(chain);
        Memory memory = drawBitmap.getBitmap(dicomImage, drawBitmapType_t.drawBitmapRGBA, 4);

        // Build the Android Bitmap from the raw bytes returned by DrawBitmap.
        Bitmap renderBitmap = Bitmap.createBitmap((int) dicomImage.getWidth(), (int) dicomImage.getHeight(), Bitmap.Config.ARGB_8888);
        byte[] memoryByte = new byte[(int) memory.size()];
        memory.data(memoryByte);
        ByteBuffer byteBuffer = ByteBuffer.wrap(memoryByte);
        renderBitmap.copyPixelsFromBuffer(byteBuffer);

        // Update the image
        mImageView.setImageBitmap(renderBitmap);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Update the text with the patient name
        mTextView.setText(loadDataSet.getPatientName(new TagId(0x10, 0x10), 0, new PatientName("Undefined", "", "")).getAlphabeticRepresentation());
    }
    /*

    Here we get the response from the file selector. We use the returned URI to open an
    InputStream which we push to the DICOM codec through a PIPE.

    It would be simpler to just use a file name with the DICOM codec, but this is difficult
    to obtain from the file selector dialog and would not allow to load also files from external
    sources (e.g. the Google Drive).

     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            try {
                CodecFactory.setMaximumImageSize(8000, 8000);

                Uri selectedfile = data.getData();
                if (selectedfile == null) {
                    return;
                }
                InputStream stream = getContentResolver().openInputStream(selectedfile);
                PipeStream dicomheroPipe = new PipeStream(32000);
                Thread pushThread = new Thread(new PushToDicomheroPipe(dicomheroPipe, stream));
                pushThread.start();

                loadDataSet = CodecFactory.load(new StreamReader(dicomheroPipe.getStreamInput()));

                currentFrameIndex = 0;
                updateImageView();
                updateTextViews(); // Call the method to update TextViews


            } catch (IOException e) {
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage(e.getMessage());
                dlgAlert.setTitle("Error");
                dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss the dialog
                    }
                });
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
            }
        }
    }
    private void updateTextViews() {
        if (loadDataSet != null) {
            PatientName patientName = loadDataSet.getPatientName(new TagId(0x10, 0x10), 0, new PatientName("Undefined", "", ""));
            patientNameTextView.setText("PName: " + patientName.getAlphabeticRepresentation());

            String patientId = loadDataSet.getString(new TagId(0x10, 0x20), 0, "");
            patientIdTextView.setText("PID: " + patientId);

            String patientSex = loadDataSet.getString(new TagId(0x10, 0x40), 0, "");
            patientSexTextView.setText("PSex: " + patientSex);

            String patientAge = loadDataSet.getString(new TagId(0x10, 0x1010), 0, "");
            patientAgeTextView.setText("PAge: " + patientAge);

            String patientDob = loadDataSet.getString(new TagId(0x10, 0x30), 0, "");
            patientDobTextView.setText("PDOB: " + patientDob);

            String studyDate = loadDataSet.getString(new TagId(0x8, 0x20), 0, "");
            studyDateTextView.setText("SDate: " + studyDate);

            String studyDesc = loadDataSet.getString(new TagId(0x8, 0x1030), 0, "");
            studyDescTextView.setText("SDescription: " + studyDesc);

            String seriesDate = loadDataSet.getString(new TagId(0x8, 0x21), 0, "");
            seriesDateTextView.setText("SDate: " + seriesDate);
//
//            String seriesTime = loadDataSet.getString(new TagId(0x8, 0x31), 0, "");
//            seriesTimeTextView.setText("Series Time: " + seriesTime);
        }
    }
}