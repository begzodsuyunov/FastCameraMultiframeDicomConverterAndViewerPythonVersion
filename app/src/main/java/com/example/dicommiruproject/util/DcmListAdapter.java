package com.example.dicommiruproject.util;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dicommiruproject.DiagnoseActivity;
import com.example.dicommiruproject.R;

import java.text.DecimalFormat;
import java.util.List;

public class DcmListAdapter extends RecyclerView.Adapter<DcmListAdapter.ViewHolder> {
    private List<DcmFile> dcmFiles;
    private OnItemClickListener onItemClickListener;

    public DcmListAdapter(List<DcmFile> dcmFiles, OnItemClickListener onItemClickListener) {

        this.dcmFiles = dcmFiles;
        this.onItemClickListener = onItemClickListener;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dcm_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DcmFile dcmFile = dcmFiles.get(position);

        holder.fileNameTextView.setText(dcmFile.getFileName());
        holder.fileSizeTextView.setText("File Size: " + formatFileSize(dcmFile.getFileSize()));
        holder.creationTimeTextView.setText("Creation Time: " + dcmFile.getFormattedCreationTime());

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(dcmFile));

    }

    // Helper method to format file size
    private String formatFileSize(long fileSize) {
        double fileSizeInMB = bytesToMegabytes(fileSize);
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(fileSizeInMB) + " MB";
    }

    public static double bytesToMegabytes(long bytes) {
        return (double) bytes / (1024 * 1024);
    }
    @Override
    public int getItemCount() {
        return dcmFiles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView fileNameTextView;
        TextView fileSizeTextView;
        TextView creationTimeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewIcon);
            fileNameTextView = itemView.findViewById(R.id.textViewFileName);
            fileSizeTextView = itemView.findViewById(R.id.textViewFileSize);
            creationTimeTextView = itemView.findViewById(R.id.textViewFileCreationTime);
        }
    }
    public void updateData(List<DcmFile> newDcmFiles) {
        dcmFiles.clear();
        dcmFiles.addAll(newDcmFiles);
        notifyDataSetChanged();
    }
    public interface OnItemClickListener {
        void onItemClick(DcmFile dcmFile);
    }
}
