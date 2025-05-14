package com.gmscan.utility;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gmscan.R;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Objects;

public class CustomBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_VIEW_TYPE = "view_type";
    private static final String OBJ_DOCUMENT = "doc_obj";
    private OnBottomSheetDismissListener onBottomSheetDismissListener;
    private OnViewSignOutButtonClickListener onViewSignOutButtonClickListener;
    private OnViewScannerButtonClickListener onViewScannerButtonClickListener;
    private OnViewMoreButtonClickListener onViewMoreButtonClickListener;
    private OnViewExportButtonClickListener onViewExportButtonClickListener;

    // Define different view types
    public static final int VIEW_SCANNER = 1;
    public static final int VIEW_SIGN_OUT = 2;
    public static final int VIEW_EXPORT = 3;
    public static final int VIEW_MORE = 4;

    public interface OnViewSignOutButtonClickListener {
        void onSignOutClick();
    }

    public interface OnViewScannerButtonClickListener {
        void onCameraClick();
        void onGalleryClick();
    }

    public interface OnViewMoreButtonClickListener {

        void onRenameClick();
        void onShareClick();
        void onSaveClick();
        void onDeleteClick();
    }

    public interface OnViewExportButtonClickListener {
        void onExportPNGClick();
        void onExportPDFClick();
    }

    public void setOnBottomSheetDismissListener(OnBottomSheetDismissListener listener) {
        this.onBottomSheetDismissListener = listener;
    }
    public void setOnViewSignOutButtonClickListener(OnViewSignOutButtonClickListener listener) {
        this.onViewSignOutButtonClickListener = listener;
    }
    public void setOnMoreButtonClickListener(OnViewMoreButtonClickListener listener) {
        this.onViewMoreButtonClickListener = listener;
    }
    public void setOnViewScannerButtonClickListener(OnViewScannerButtonClickListener listener) {
        this.onViewScannerButtonClickListener = listener;
    }
    public void setOnExportButtonClickListener(OnViewExportButtonClickListener listener) {
        this.onViewExportButtonClickListener = listener;
    }

    public static CustomBottomSheetDialog newInstance(int viewType) {
        CustomBottomSheetDialog fragment = new CustomBottomSheetDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_VIEW_TYPE, viewType);
        fragment.setArguments(args);
        return fragment;
    }

    public static CustomBottomSheetDialog newInstance(int viewType, ScannedDocument document) {
        CustomBottomSheetDialog fragment = new CustomBottomSheetDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_VIEW_TYPE, viewType);
        args.putSerializable(OBJ_DOCUMENT, document);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = (com.google.android.material.bottomsheet.BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int viewType = getArguments() != null ? getArguments().getInt(ARG_VIEW_TYPE, VIEW_SCANNER) : VIEW_SCANNER;
        View view;

        if (viewType == VIEW_SCANNER) {
            view = View.inflate(getContext(), R.layout.bottom_sheet_scanner, null);
            setupScannerView(view);
        } else if (viewType == VIEW_SIGN_OUT) {
            view = View.inflate(getContext(), R.layout.layout_signout_popup, null);
            setupSignOutView(view);
        } else if (viewType == VIEW_EXPORT) {
            view = View.inflate(getContext(), R.layout.layout_export_popup, null);
            setupExportView(view);
        } else if (viewType == VIEW_MORE) {
            view = View.inflate(getContext(), R.layout.layout_scan_histroy_popup, null);
            setupScanHistoryView(view);
        } else {
            view = View.inflate(getContext(), R.layout.bottom_sheet_scanner, null);
            setupScannerView(view);
        }

        dialog.setContentView(view);

        dialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(null);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        });

        return dialog;
    }

    private void setupScannerView(View view) {
        RelativeLayout btnCamera = view.findViewById(R.id.btnCamera);
        RelativeLayout btnGallery = view.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> {
            if (onViewScannerButtonClickListener != null) onViewScannerButtonClickListener.onCameraClick();
            dismiss();
        });

        btnGallery.setOnClickListener(v -> {
            if (onViewScannerButtonClickListener != null) onViewScannerButtonClickListener.onGalleryClick();
            dismiss();
        });
    }

    private void setupScanHistoryView(View view) {
        View includeView = view.findViewById(R.id.includeView);
        View renameOption = view.findViewById(R.id.option_rename);
        View shareOption = view.findViewById(R.id.option_share);
        View saveOption = view.findViewById(R.id.option_save);
        View deleteOption = view.findViewById(R.id.option_delete);

        TextView nameTextView = view.findViewById(R.id.nameTextView);
        TextView scanTypeTextView = view.findViewById(R.id.scanTypeTextView);

        final ScannedDocument scannedDocument = (ScannedDocument) requireArguments().getSerializable(OBJ_DOCUMENT);
        nameTextView.setText(Objects.requireNonNull(scannedDocument).getName());
        scanTypeTextView.setText(scannedDocument.getScanType());
        ImageUtil.load(view.getContext(),scannedDocument.getFileUrl(),includeView.findViewById(R.id.imgPhoto),includeView.findViewById(R.id.imageProgress));


        renameOption.setOnClickListener(v -> {
            if (onViewMoreButtonClickListener != null) onViewMoreButtonClickListener.onRenameClick();
            dismiss();
        });

        shareOption.setOnClickListener(v -> {
            if (onViewMoreButtonClickListener != null) onViewMoreButtonClickListener.onShareClick();
            dismiss();
        });

        saveOption.setOnClickListener(v -> {
            if (onViewMoreButtonClickListener != null) onViewMoreButtonClickListener.onSaveClick();
            dismiss();
        });

        deleteOption.setOnClickListener(v -> {
            if (onViewMoreButtonClickListener != null) onViewMoreButtonClickListener.onDeleteClick();
            dismiss();
        });
    }

    private void setupExportView(View view) {
        View btnClose = view.findViewById(R.id.btnClose);
        View btnExportPNG = view.findViewById(R.id.btnExportPNG);
        View btnExportPDF = view.findViewById(R.id.btnExportPDF);

        btnClose.setOnClickListener(v -> dismiss());

        btnExportPNG.setOnClickListener(v -> {
            if (onViewExportButtonClickListener != null) onViewExportButtonClickListener.onExportPNGClick();
            dismiss();
        });

        btnExportPDF.setOnClickListener(v -> {
            if (onViewExportButtonClickListener != null) onViewExportButtonClickListener.onExportPDFClick();
            dismiss();
        });
    }

    private void setupSignOutView(View view) {
        Button cancelButton = view.findViewById(R.id.btn_cancel);
        Button signOutButton = view.findViewById(R.id.btn_sign_out);

        cancelButton.setOnClickListener(v -> dismiss());
        signOutButton.setOnClickListener(view1 -> {
            if (onViewSignOutButtonClickListener != null) onViewSignOutButtonClickListener.onSignOutClick();
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onBottomSheetDismissListener != null) {
            onBottomSheetDismissListener.onBottomSheetDismissed();
        }
    }

    public interface OnBottomSheetDismissListener {
        void onBottomSheetDismissed();
    }
}