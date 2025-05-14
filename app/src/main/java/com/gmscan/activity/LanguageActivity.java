package com.gmscan.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.gmscan.R;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class LanguageActivity extends AppCompatActivity {
    private LanguageAdapter adapter;
    private List<LanguageItem> languageItems;
    private static final String PREF_NAME = "LanguagePrefs";
    private static final String PREF_SELECTED_LANGUAGE = "selected_language";

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_language);

        // Initialize ListView
        ListView languageListView = findViewById(R.id.languageListView);

        // Setup back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Initialize data
        initializeLanguageData();

        // Restore saved selection
        restoreSelectedLanguage();

        // Create and set adapter
        adapter = new LanguageAdapter(this, languageItems);
        languageListView.setAdapter(adapter);

        // Set item click listener
        languageListView.setOnItemClickListener((parent, view, position, id) -> {
            LanguageItem item = adapter.getItem(position);
            if (item != null && !item.isHeader()) {
                // Update previously selected item
                LanguageItem previousSelected = findSelectedItem();
                if (previousSelected != null) {
                    previousSelected.setSelected(false);
                }

                // Update newly selected item
                item.setSelected(true);

                // Save selection
                saveSelectedLanguage(item.getName());

                // Notify adapter to refresh the list
                adapter.notifyDataSetChanged();

                // Notify user (replace with actual language change implementation)
                Toast.makeText(this, getString(R.string.language_changed, item.getName()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private LanguageItem findSelectedItem() {
        for (LanguageItem item : languageItems) {
            if (item.isSelected()) {
                return item;
            }
        }
        return null;
    }

    private void saveSelectedLanguage(String language) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_SELECTED_LANGUAGE, language);
        editor.apply();
    }

    private void restoreSelectedLanguage() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedLanguage = preferences.getString(PREF_SELECTED_LANGUAGE, getString(R.string.english_us));

        // Find the saved language in the list and select it
        boolean foundSaved = false;
        for (int i = 0; i < languageItems.size(); i++) {
            LanguageItem item = languageItems.get(i);
            if (!item.isHeader() && item.getName().equals(savedLanguage)) {
                item.setSelected(true);
                foundSaved = true;
            } else {
                item.setSelected(false);
            }
        }

        // If saved language not found in list, default to English (US)
        if (!foundSaved) {
            for (int i = 0; i < languageItems.size(); i++) {
                LanguageItem item = languageItems.get(i);
                if (!item.isHeader() && item.getName().equals(getString(R.string.english_us))) {
                    item.setSelected(true);
                    break;
                }
            }
        }
    }

    private void initializeLanguageData() {
        languageItems = new ArrayList<>();

        // Suggested section
        languageItems.add(new LanguageItem(getString(R.string.suggested), true, false));
        languageItems.add(new LanguageItem(getString(R.string.english_us), false, false));
        languageItems.add(new LanguageItem(getString(R.string.english_uk), false, false));

        // Language section
        languageItems.add(new LanguageItem(getString(R.string.language), true, false));
        languageItems.add(new LanguageItem(getString(R.string.spanish), false, false));
        languageItems.add(new LanguageItem(getString(R.string.french), false, false));
    }

    // Language item model class
    public static class LanguageItem {
        private final String name;
        boolean isHeader;
        private boolean isSelected;

        public LanguageItem(String name, boolean isHeader, boolean isSelected) {
            this.name = name;
            this.isHeader = isHeader;
            this.isSelected = isSelected;
        }

        public String getName() {
            return name;
        }

        public boolean isHeader() {
            return isHeader;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }

    // Custom adapter for the language list
    public static class LanguageAdapter extends BaseAdapter {
        private final Context context;
        private final List<LanguageItem> items;
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;

        public LanguageAdapter(Context context, List<LanguageItem> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public LanguageItem getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2; // Header and normal item
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @Override
        public boolean isEnabled(int position) {
            return !items.get(position).isHeader(); // Headers are not clickable
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LanguageItem item = getItem(position);

            if (item.isHeader()) {
                // Header view
                HeaderViewHolder headerHolder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.layout_header_language, parent, false);
                    headerHolder = new HeaderViewHolder();
                    headerHolder.headerTextView = convertView.findViewById(R.id.headerTextView);
                    convertView.setTag(headerHolder);
                } else {
                    headerHolder = (HeaderViewHolder) convertView.getTag();
                }
                headerHolder.headerTextView.setText(item.getName());
            } else {
                // Language item view
                ItemViewHolder itemHolder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.layout_list_language, parent, false);
                    itemHolder = new ItemViewHolder();
                    itemHolder.languageNameTextView = convertView.findViewById(R.id.languageNameTextView);
                    itemHolder.checkImageView = convertView.findViewById(R.id.checkImageView);
                    convertView.setTag(itemHolder);
                } else {
                    itemHolder = (ItemViewHolder) convertView.getTag();
                }

                itemHolder.languageNameTextView.setText(item.getName());
                itemHolder.checkImageView.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
            }

            return convertView;
        }

        // View holder pattern
        private static class HeaderViewHolder {
            TextView headerTextView;
        }

        private static class ItemViewHolder {
            TextView languageNameTextView;
            ImageView checkImageView;
        }
    }
}