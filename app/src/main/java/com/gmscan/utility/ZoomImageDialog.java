package com.gmscan.utility;

import android.app.Dialog;
import android.content.Context;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.gmscan.R;
import com.jsibbold.zoomage.ZoomageView;

public class ZoomImageDialog {

    public static void show(Context context, @Nullable String imageUrl) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_zoom_image);

        ZoomageView zoomImageView = dialog.findViewById(R.id.zoomImageView);
        ImageButton closeButton = dialog.findViewById(R.id.closeButton);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

        ImageUtil.load(context, imageUrl, zoomImageView, progressBar);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
