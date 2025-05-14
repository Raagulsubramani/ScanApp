package com.gmscan.adapter;

import static com.gmscan.utility.CustomBottomSheetDialog.VIEW_MORE;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.gmscan.R;
import com.gmscan.model.getAllDocuments.GetAllDocumentsResponseItem;
import com.gmscan.model.getAllDocuments.ScannedDocument;
import com.gmscan.utility.CustomBottomSheetDialog;
import com.gmscan.utility.ImageUtil;
import com.gmscan.utility.ShareUtils;
import com.makeramen.roundedimageview.RoundedImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter for displaying scan history in a RecyclerView.
 * This adapter groups scanned documents by date and displays them accordingly.
 */
public class FaviouriteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_DOCUMENT = 1;
    private static final String UNKNOWN_DATE = "Unknown Date";

    private final List<Object> groupedItems;
    public List<GetAllDocumentsResponseItem> originalDocuments;
    private final Context context;

    // Interface for delete click listener
    public interface OnDeleteClickListener {
        void onDeleteClick(String documentId);
    }

    // Interface for favorite click listener
    public interface OnFavoriteClickListener {
        void onFavoriteClick(String documentId, boolean isFavorite);
    }




    public interface OnDocumentClickListener {
        void onDocumentClick(GetAllDocumentsResponseItem document);
    }
    private OnDeleteClickListener onDeleteClickListener;
    private final OnDocumentClickListener  onDocumentClickListener ;
    private OnFavoriteClickListener onFavoriteClickListener;
    private OnSaveClickListener onSaveClickListener;
    private OnRenameClickListener onRenameClickListener;

    public interface OnSaveClickListener {
        void onSave(GetAllDocumentsResponseItem document);
    }

    public interface OnRenameClickListener {
        void onRename(GetAllDocumentsResponseItem document);
    }

    public void setOnSaveClickListener(OnSaveClickListener listener) {
        this.onSaveClickListener = listener;
    }

    public void setOnRenameClickListener(OnRenameClickListener listener) {
        this.onRenameClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    // Method to set the favorite listener
    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.onFavoriteClickListener = listener;
    }

    /**
     * Constructor for initializing the scan history adapter.
     *
     * @param context Context for accessing resources.
     * @param items   List of items (can be a list of ScannedDocument objects or a pre-processed list).
     */
    public FaviouriteAdapter(Context context, List<?> items,OnDocumentClickListener onDocumentClickListener) {
        this.context = context;
        this.onDocumentClickListener = onDocumentClickListener;

        // Check if the list is already processed (contains String headers)
        boolean isPreProcessed = false;
        for (Object item : items) {
            if (item instanceof String) {
                isPreProcessed = true;
                break;
            }
        }

        if (isPreProcessed) {
            // The list is already grouped with date headers
            this.groupedItems = new ArrayList<>(items);

            // Extract only the ScannedDocument objects for the original list
            this.originalDocuments = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof GetAllDocumentsResponseItem) {
                    this.originalDocuments.add((GetAllDocumentsResponseItem) item);
                }
            }
        } else {
            // The list contains only ScannedDocument objects
            List<GetAllDocumentsResponseItem> documents = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof GetAllDocumentsResponseItem) {
                    documents.add((GetAllDocumentsResponseItem) item);
                }
            }

            this.originalDocuments = new ArrayList<>(documents);
            this.groupedItems = groupByDate(documents);
        }
    }

    /**
     * Groups scanned documents by date to organize the RecyclerView.
     */
    private List<Object> groupByDate(List<GetAllDocumentsResponseItem> scanHistoryList) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        Map<String, List<GetAllDocumentsResponseItem>> groupedMap = new LinkedHashMap<>();

        for (GetAllDocumentsResponseItem doc : scanHistoryList) {
            String formattedDate;

            try {
                if (doc.getCreatedAt() != null) {
                    Date date = inputFormat.parse(doc.getCreatedAt());
                    formattedDate = outputFormat.format(Objects.requireNonNull(date));
                } else {
                    formattedDate = UNKNOWN_DATE;
                }
            } catch (Exception e) {
                formattedDate = UNKNOWN_DATE; // Fallback if parsing fails
            }

            groupedMap.computeIfAbsent(formattedDate, k -> new ArrayList<>()).add(doc);
        }

        List<Object> groupedList = new ArrayList<>();
        for (Map.Entry<String, List<GetAllDocumentsResponseItem>> entry : groupedMap.entrySet()) {
            groupedList.add(entry.getKey());
            groupedList.addAll(entry.getValue());
        }
        return groupedList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.row_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.row_history_item, parent, false);
            return new ScanHistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = groupedItems.get(position);

        if (holder instanceof DateHeaderViewHolder) {
            bindDateHeader((DateHeaderViewHolder) holder, (String) item);
        } else if (holder instanceof ScanHistoryViewHolder) {
            bindDocument((ScanHistoryViewHolder) holder, (GetAllDocumentsResponseItem) item, position);
        }
    }

    /**
     * Binds a date header to the view holder.
     */
    private void bindDateHeader(DateHeaderViewHolder holder, String date) {
        holder.dateTextView.setText(date);
    }

    /**
     * Binds a scanned document to the view holder and sets click listeners.
     */
    private void bindDocument(ScanHistoryViewHolder holder, GetAllDocumentsResponseItem document, int position) {
        holder.nameTextView.setText(document.getDocumentName());
        holder.scanTypeTextView.setText(document.getScanType());

        holder.imgLike.setImageResource(document.isFavorite() ? R.drawable.ic_heart_selected : R.drawable.ic_heart_unselect);
        holder.imgLike.setOnClickListener(v -> setFavourite(document, position));
        ImageUtil.load(context,document.getFileUrl(),holder.imgPhoto,holder.imageProgress);

        holder.linearView.setOnClickListener(v -> {
            if (onDocumentClickListener != null) {
                onDocumentClickListener.onDocumentClick(document); // send it to activity
            }
        });

        // Add direct delete click listener for testing
        holder.iconsLayout.setOnClickListener(v -> {
            Log.d("DeleteDebug", "Direct delete clicked for document: " + document.getId());
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(document.getId());
            }
        });

        // Add click listener for the more options button
        holder.imgMore.setOnClickListener(view -> {
            FragmentActivity activity = (FragmentActivity) context;

            ScannedDocument scannedDocument = new ScannedDocument();
            scannedDocument.setScanType(document.getScanType());
            scannedDocument.setName(document.getDocumentName());
            scannedDocument.setFileUrl(document.getFileUrl());

            CustomBottomSheetDialog bottomSheet = CustomBottomSheetDialog.newInstance(VIEW_MORE,scannedDocument);
            bottomSheet.setOnMoreButtonClickListener(new CustomBottomSheetDialog.OnViewMoreButtonClickListener() {
                @Override
                public void onRenameClick() {
                    if (onRenameClickListener != null) {
                        onRenameClickListener.onRename(document);
                    }
                }
                @Override
                public void onShareClick() {
                    ShareUtils.shareDocumentUrl(context, scannedDocument.getFileUrl(), scannedDocument.getScanType());
                }
                @Override
                public void onSaveClick() {
                    if (onSaveClickListener != null) {
                        onSaveClickListener.onSave(document);
                    }
                }
                @Override
                public void onDeleteClick() {
                    if (onDeleteClickListener != null) {
                        onDeleteClickListener.onDeleteClick(document.getId());
                    }
                }
            });
            bottomSheet.show(activity.getSupportFragmentManager(), "MoreBottomSheet");
        });
    }
    /**
     * Toggles the like state of the document and updates the UI.
     */
    private void setFavourite(GetAllDocumentsResponseItem document, int position) {
        boolean newFavoriteStatus = !document.isFavorite();
        document.setFavorite(newFavoriteStatus);
        notifyItemChanged(position);

        // Notify the listener about the change if it exists
        if (onFavoriteClickListener != null) {
            onFavoriteClickListener.onFavoriteClick(document.getId(), newFavoriteStatus);
        }
    }

    @Override
    public int getItemCount() {
        return groupedItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (groupedItems.get(position) instanceof String) ? VIEW_TYPE_DATE_HEADER : VIEW_TYPE_DOCUMENT;
    }

    /**
     * ViewHolder for scanned documents.
     */
    public static class ScanHistoryViewHolder extends RecyclerView.ViewHolder {
        public final TextView nameTextView;
        public final TextView scanTypeTextView;
        public final ImageView imgLike;
        public final ImageView imgMore;
        public final LinearLayout iconsLayout;
        public final LinearLayout linearView;
        public final RoundedImageView imgPhoto;
        public final ProgressBar imageProgress;

        public ScanHistoryViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.nameTextView);
            scanTypeTextView = view.findViewById(R.id.scanTypeTextView);
            linearView = view.findViewById(R.id.linearView);
            imgLike = view.findViewById(R.id.imgLike);
            imgMore = view.findViewById(R.id.imgMore);
            iconsLayout = view.findViewById(R.id.icons_layout);
            imgPhoto = view.findViewById(R.id.imgPhoto);
            imageProgress = view.findViewById(R.id.imageProgress);
        }
    }

    /**
     * ViewHolder for date headers.
     */
    public static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        public final TextView dateTextView;

        public DateHeaderViewHolder(View view) {
            super(view);
            dateTextView = view.findViewById(R.id.dateTextView);
        }
    }
}