package com.example.dicommiruproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;

import com.example.dicommiruproject.util.DcmFile;
import com.example.dicommiruproject.util.DcmListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity implements DcmListAdapter.OnItemClickListener{
    private RecyclerView recyclerView;

    private DcmListAdapter dcmListAdapter;
    private FileObserver fileObserver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        recyclerView = findViewById(R.id.recyclerViewDcmFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list
        dcmListAdapter = new DcmListAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(dcmListAdapter);

        // Initialize and start the FileObserver in the directory
        String directoryPath = "/storage/emulated/0/Download/DCMFiles/";
        fileObserver = new FileObserver(directoryPath) {
            @Override
            public void onEvent(int event, String path) {
                // Check if a new file is created (CREATE event)
                if (event == FileObserver.CREATE) {
                    // Notify the adapter that a new file is created
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dcmListAdapter.updateData(getDcmFiles());
                            // Assuming you have a method updateData in your adapter
                            // that updates the dataset and calls notifyDataSetChanged()
                        }
                    });
                }
            }
        };
        fileObserver.startWatching();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the FileObserver when the activity is destroyed
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
    }

    // Add your other methods here...

    // Include your getDcmFiles method here...

    private List<DcmFile> getDcmFiles() {
        List<DcmFile> dcmFiles = new ArrayList<>();
        File directory = new File(Environment.getExternalStorageDirectory(), "/Download/DCMFiles");

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".dcm")) {
                        long fileSize = file.length();  // Get the file size
                        long creationTime = file.lastModified();  // Get the file creation time
                        Uri fileUri = Uri.fromFile(file);

                        dcmFiles.add(new DcmFile(file.getName(), fileSize, creationTime, fileUri));
                    }
                }
            }
        }

        return dcmFiles;
    }
    private void initRecyclerView() {
        // Assuming getDcmFiles() returns the initial list of DcmFiles
        List<DcmFile> initialDcmFiles = getDcmFiles();

        // Assuming DcmListAdapter takes the initial list in its constructor
        dcmListAdapter = new DcmListAdapter(initialDcmFiles, this);

        // Assuming you have a RecyclerView with the ID "recyclerView" in your layout
        RecyclerView recyclerView = findViewById(R.id.recyclerViewDcmFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(dcmListAdapter);
    }
    @Override
    protected void onStart() {
        super.onStart();
        initRecyclerView();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Remove the transition animation
        overridePendingTransition(0, 0);
    }
    @Override
    public void onItemClick(DcmFile dcmFile) {
        // Handle item click, open the DiagnoseActivity with the selected DICOM file
        Intent intent = new Intent(FileListActivity.this, DiagnoseActivity.class);
        // Pass information about the selected DICOM file to DiagnoseActivity
        // For example, you can pass the file path or any other relevant information.
        intent.putExtra("selected_dcm_file", dcmFile);
        startActivity(intent);
    }


}