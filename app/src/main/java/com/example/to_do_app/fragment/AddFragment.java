package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleTemplateAdapter;
import com.example.to_do_app.data.ScheduleData;
import com.example.to_do_app.model.ScheduleTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleTemplateAdapter adapter;
    private List<ScheduleTemplate> allTemplates;
    private List<ScheduleTemplate> currentlyDisplayedTemplates;

    private LinearLayout filterOptionsContainer;
    private RadioGroup radioGroupFilterOptions;
    private Button btnApplyFilter, btnResetFilter;

    // Data for filters
    private Map<Integer, String> filterCategoryMap; // Maps Button ID to Category Name
    private Map<String, List<String>> filterOptionsMap; // Maps Category Name to List of Options
    private Map<String, String> optionTagMap; // Maps "Category:Option" to a specific tag

    // New state management
    private Map<String, String> appliedFilters; // Stores currently applied filters, e.g., {"Giờ ngủ": "6 giờ"}
    private String currentCategoryForEditing = ""; // Which category panel is currently open

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FilterStatePrefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeFilterData();
        loadFiltersFromPrefs(); // Load saved filters
        setupListeners();
        setupRecyclerView();
        applyAllFilters(); // Apply loaded filters on startup
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        filterOptionsContainer = view.findViewById(R.id.filter_options_container);
        radioGroupFilterOptions = view.findViewById(R.id.radio_group_filter_options);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnResetFilter = view.findViewById(R.id.btn_reset_filter);
    }

    private void initializeFilterData() {
        loadSampleData();
        currentlyDisplayedTemplates = new ArrayList<>(allTemplates);
        appliedFilters = new HashMap<>();
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        filterCategoryMap = new HashMap<>();
        filterCategoryMap.put(R.id.btn_filter_sleep, "Giờ ngủ");
        filterCategoryMap.put(R.id.btn_filter_study, "Học tập");
        filterCategoryMap.put(R.id.btn_filter_entertainment, "Giải trí");
        filterCategoryMap.put(R.id.btn_filter_sports, "Thể thao");

        filterOptionsMap = new HashMap<>();
        filterOptionsMap.put("Giờ ngủ", Arrays.asList("4 giờ", "6 giờ", "8 giờ"));
        filterOptionsMap.put("Học tập", Arrays.asList("4 giờ", "6 giờ", "8 giờ"));
        filterOptionsMap.put("Giải trí", Arrays.asList("30 phút", "60 phút", "90 phút"));
        filterOptionsMap.put("Thể thao", Arrays.asList("30 phút", "60 phút", "90 phút"));

        optionTagMap = new HashMap<>();
        optionTagMap.put("Giờ ngủ:4 giờ", "4h_sleep");
        optionTagMap.put("Giờ ngủ:6 giờ", "6h_sleep");
        optionTagMap.put("Giờ ngủ:8 giờ", "8h_sleep");
        optionTagMap.put("Học tập:4 giờ", "4h_study");
        optionTagMap.put("Học tập:6 giờ", "6h_study");
        optionTagMap.put("Học tập:8 giờ", "8h_study");
        optionTagMap.put("Giải trí:30 phút", "30m_relax");
        optionTagMap.put("Giải trí:60 phút", "60m_relax");
        optionTagMap.put("Giải trí:90 phút", "90m_relax");
        optionTagMap.put("Thể thao:30 phút", "30m_sport");
        optionTagMap.put("Thể thao:60 phút", "60m_sport");
        optionTagMap.put("Thể thao:90 phút", "90m_sport");
    }

    private void setupListeners() {
        View.OnClickListener filterButtonClickListener = v -> {
            String category = filterCategoryMap.get(v.getId());
            if (category != null) {
                toggleFilterOptions(category);
            }
        };

        requireView().findViewById(R.id.btn_filter_sleep).setOnClickListener(filterButtonClickListener);
        requireView().findViewById(R.id.btn_filter_study).setOnClickListener(filterButtonClickListener);
        requireView().findViewById(R.id.btn_filter_entertainment).setOnClickListener(filterButtonClickListener);
        requireView().findViewById(R.id.btn_filter_sports).setOnClickListener(filterButtonClickListener);

        btnApplyFilter.setOnClickListener(v -> {
            int selectedId = radioGroupFilterOptions.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = requireView().findViewById(selectedId);
                String selectedOption = selectedRadioButton.getText().toString();

                // Save the selected filter
                appliedFilters.put(currentCategoryForEditing, selectedOption);
                saveFilterState(currentCategoryForEditing, selectedOption);

                // Apply all filters and update UI
                applyAllFilters();
                filterOptionsContainer.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Đã áp dụng: " + selectedOption, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetFilter.setOnClickListener(v -> {
            // Remove the filter for the current category
            appliedFilters.remove(currentCategoryForEditing);
            clearFilterState(currentCategoryForEditing);

            // Apply remaining filters and update UI
            applyAllFilters();
            filterOptionsContainer.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Đã xóa bộ lọc cho " + currentCategoryForEditing, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new ScheduleTemplateAdapter(getContext(), currentlyDisplayedTemplates);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void toggleFilterOptions(String category) {
        // If the same filter is clicked again while visible, hide it.
        if (filterOptionsContainer.getVisibility() == View.VISIBLE && category.equals(currentCategoryForEditing)) {
            filterOptionsContainer.setVisibility(View.GONE);
            return;
        }

        currentCategoryForEditing = category;
        radioGroupFilterOptions.clearCheck();
        radioGroupFilterOptions.removeAllViews();

        List<String> options = filterOptionsMap.get(category);
        String previouslySelectedOption = appliedFilters.get(category);

        if (options != null) {
            for (String option : options) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(option);
                radioButton.setTextSize(16f);
                radioButton.setPadding(32, 32, 32, 32);
                if (option.equals(previouslySelectedOption)) {
                    radioButton.setChecked(true); // Restore checked state
                }
                radioGroupFilterOptions.addView(radioButton);
            }
        }
        filterOptionsContainer.setVisibility(View.VISIBLE);
    }

    private void applyAllFilters() {
        List<ScheduleTemplate> filteredList = new ArrayList<>(allTemplates);

        // Sequentially apply each filter from the map
        for (Map.Entry<String, String> entry : appliedFilters.entrySet()) {
            String category = entry.getKey();
            String option = entry.getValue();
            String optionKey = category + ":" + option;
            String optionTag = optionTagMap.get(optionKey);

            if (optionTag != null && !optionTag.isEmpty()) {
                final String finalTag = optionTag;
                filteredList = filteredList.stream()
                        .filter(template -> template.getTags().contains(finalTag))
                        .collect(Collectors.toList());
            }
        }

        adapter.updateList(filteredList);
    }

    // --- SharedPreferences Logic ---

    private void saveFilterState(String category, String option) {
        sharedPreferences.edit().putString(category, option).apply();
    }

    private void clearFilterState(String category) {
        sharedPreferences.edit().remove(category).apply();
    }

    private void loadFiltersFromPrefs() {
        for (String category : filterOptionsMap.keySet()) {
            String savedOption = sharedPreferences.getString(category, null);
            if (savedOption != null) {
                appliedFilters.put(category, savedOption);
            }
        }
    }

    // --- Data Loading ---
    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        allTemplates.addAll(ScheduleData.getSampleTemplates());
    }
}
