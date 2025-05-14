package com.gmscan.utility;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.gmscan.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for dynamically setting up a ChipGroup with selectable chips.
 */
public class ChipUtils {

    /**
     * Sets up a ChipGroup dynamically with the provided list of chip labels.
     *
     * @param context       The context in which the ChipGroup is used.
     * @param chipGroup     The ChipGroup where the chips will be added.
     * @param chipLabels    The list of labels for the chips.
     * @param onChipSelected A callback function that is triggered when a chip is selected.
     */
    public static void setupChipGroup(Context context, ChipGroup chipGroup, List<String> chipLabels, Consumer<String> onChipSelected) {
        chipGroup.setSingleSelection(true);
        chipGroup.removeAllViews(); // Clear existing chips before adding new ones.

        // Loop through the chipLabels list and create chips dynamically.
        for (int i = 0; i < chipLabels.size(); i++) {
            Chip chip = new Chip(context);
            chip.setText(chipLabels.get(i));
            chip.setCheckable(true);
            chip.setChecked(i == 0); // Set the first chip as selected by default.
            chip.setCheckedIconVisible(false);
            chip.setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_selector));
            chip.setChipBackgroundColorResource(R.color.chip_selector);
            chip.setChipStrokeWidth(3);
            chip.setChipStrokeColorResource(R.color.selected_color);
            chipGroup.addView(chip);
        }

        // Set a listener to handle chip selection events.
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int selectedChipId = checkedIds.get(0);
            Chip selectedChip = group.findViewById(selectedChipId);

            if (selectedChip != null) {
                onChipSelected.accept(selectedChip.getText().toString());
            }
        });
    }
}
