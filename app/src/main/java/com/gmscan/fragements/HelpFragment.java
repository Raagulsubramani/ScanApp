package com.gmscan.fragements;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gmscan.R;
import com.gmscan.adapter.FaqAdapter;
import com.gmscan.model.faq.FaqModel;
import com.gmscan.utility.ChipUtils;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * HelpFragment displays a list of FAQs and allows filtering based on category.
 */
public class HelpFragment extends Fragment {
    private FaqAdapter faqAdapter;
    private List<FaqModel> faqList;
    private List<FaqModel> originalFaqList;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private ChipGroup chipGroup;
    private View includeNoSearch;

    // Chip labels representing FAQ categories
    private final List<String> chipLabels = Arrays.asList("All", "General", "Scan", "Account", "Services", "Export");

    public static HelpFragment newInstance() {
        return new HelpFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
         recyclerView = view.findViewById(R.id.recyclerView);
        searchView = view.findViewById(R.id.searchView);
        View includeAppBar = view.findViewById(R.id.includeAppBar);
        TextView txtTitle = includeAppBar.findViewById(R.id.txtTitle);
        txtTitle.setText(getString(R.string.help_center));
        chipGroup = view.findViewById(R.id.chipGroup);
        includeNoSearch = view.findViewById(R.id.includeNoSearch);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize FAQ list
        initializeFaqList();

        // Setup adapter
        faqAdapter = new FaqAdapter(faqList);
        recyclerView.setAdapter(faqAdapter);

        // Enable search functionality
        setupSearchView();

        // Add category chips dynamically
        setupChips();
    }

    /**
     * Initializes the FAQ list with predefined data.
     */
    private void initializeFaqList() {
        faqList = new ArrayList<>();
        originalFaqList = new ArrayList<>();


        addFaqItem("What is GM Scan?", "GM Scan is a document scanning application that helps you digitize and manage your documents efficiently.", "General");
        addFaqItem("Is the GMScan App Free?", "The basic version of GMScan is free. However, some advanced features may require a premium subscription.", "Services");
        addFaqItem("How do I export to PDF?", "To export a scan to PDF, open the scanned document and tap the 'Export' button, then select PDF as the export format.", "Export");
        addFaqItem("How can I Sign Out from GM Scan?", "Go to the Profile section and select 'Sign Out' from the menu options.", "Account");
        addFaqItem("How to close GMScan Account?", "To close your account, go to Settings > Account > Delete Account. Please note that this action is irreversible.", "Account");
        addFaqItem("How to scan documents properly?", "Ensure good lighting, place the document on a flat surface, align it within the camera frame, and maintain a steady hand.", "Scan");  }

    /**
     * Adds a new FAQ item to the list.
     *
     * @param question The FAQ question.
     * @param answer   The FAQ answer.
     * @param category The FAQ category.
     */
    private void addFaqItem(String question, String answer, String category) {
        FaqModel faqItem = new FaqModel(question, answer, category);
        faqList.add(faqItem);
        originalFaqList.add(faqItem);
    }


    /**
     * Initializes the ChipGroup using the ChipUtils helper method.
     * It handles filtering scan history based on the selected chip.
     */
    private void setupChips() {
        ChipUtils.setupChipGroup(requireContext(), chipGroup, chipLabels, selectedText -> {
            Toast.makeText(getActivity(), "Selected: " + selectedText, Toast.LENGTH_SHORT).show();
            filterFaqByCategory(selectedText);
        });
    }
    /**
     * Filters FAQ list based on the selected category.
     *
     * @param category The selected category.
     */
    private void filterFaqByCategory(String category) {
        List<FaqModel> filteredList = new ArrayList<>();
        if (category.equals("All")) filteredList.addAll(originalFaqList);
        else for (FaqModel faq : originalFaqList)
            if (faq.category().equalsIgnoreCase(category)) filteredList.add(faq);
        faqAdapter.updateList(filteredList);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterFaqs(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFaqs(newText);
                return true;
            }
        });
    }

    private void filterFaqs(String query) {
        List<FaqModel> filteredList = new ArrayList<>();

        for (FaqModel faq : originalFaqList) {
            if (faq.question().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(faq);
            }
        }

        if(!filteredList.isEmpty()){
            includeNoSearch.setVisibility(GONE);
            recyclerView.setVisibility(VISIBLE);
        }else{
            includeNoSearch.setVisibility(VISIBLE);
            recyclerView.setVisibility(GONE);
        }

        faqAdapter.updateList(filteredList);
    }
}