package com.example.dicommiruproject.util;

import android.net.Uri;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DcmFile implements Serializable {
    private String fileName;
    private long fileSize;
    private long creationTime;
    private String fileUriString; // Use String to store the URI

    public DcmFile(String fileName, long fileSize, long creationTime, Uri fileUri) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.creationTime = creationTime;
        this.fileUriString = fileUri.toString(); // Convert Uri to String
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public Uri getUri() {
        return Uri.parse(fileUriString); // Convert back to Uri when needed
    }

    // Helper method to format the creation time as a String
    public String getFormattedCreationTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(creationTime));
    }
}