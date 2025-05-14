package com.gmscan.utility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.gmscan.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class LoaderHelper {
    private static Dialog loadingDialog;
    private static WeakReference<Activity> activityWeakRef;

    public static void showLoader(Context context, boolean showLoader) {
        if (!(context instanceof Activity)) return;

        Activity activity = (Activity) context;
        if (activity.isFinishing() || activity.isDestroyed()) return;

        activityWeakRef = new WeakReference<>(activity);

        try {
            if (showLoader) {
                if (loadingDialog == null || !loadingDialog.isShowing()) {
                    loadingDialog = new Dialog(context);
                    @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_loader, null);
                    loadingDialog.setContentView(dialogView);
                    Objects.requireNonNull(loadingDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
                    loadingDialog.setCancelable(false);
                    loadingDialog.setCanceledOnTouchOutside(false);

                    ImageView gifLoader = dialogView.findViewById(R.id.gifLoader);
                    Glide.with(context)
                            .asGif()
                            .load(R.drawable.loader)
                            .into(gifLoader);

                    loadingDialog.show();
                }
            } else {
                hideLoader();
            }
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideLoader() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            Activity activity = activityWeakRef != null ? activityWeakRef.get() : null;
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                loadingDialog.dismiss();
            }
            loadingDialog = null;
            activityWeakRef = null;
        }
    }
}
