package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
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

/**
 * Improved AddFragment:
 * - Keeps filters in-memory during app session, but clears persisted filters automatically when the fragment stops
 *   so that when the user "exits" the app and restarts it, no filters remain applied.
 *
 * Behavior change implemented: added lifecycle hooks (onStop/onDestroyView) that clear the saved filter SharedPreferences
 * and reset in-memory applied filters so filters do not persist across app exit.
 */
public class AddFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleTemplateAdapter adapter;
    private List<ScheduleTemplate> allTemplates;
    private List<ScheduleTemplate> currentlyDisplayedTemplates;

    private LinearLayout filterOptionsContainer;
    private RadioGroup radioGroupFilterOptions;
    private Button btnApplyFilter, btnResetFilter;

    // Buttons that open categories (so we can update their visual state)
    private Button btnFilterSleep, btnFilterStudy, btnFilterEntertainment, btnFilterSports;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeFilterData();
        loadFiltersFromPrefs(); // Load saved filters for current session
        setupListeners();
        setupRecyclerView();
        applyAllFilters(); // Apply loaded filters on startup
        updateFilterButtonsState();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clear persisted filters when fragment stops (so filters won't persist across app exit)
        clearPersistedFilters();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Also clear persisted filters and UI state when view is destroyed
        clearPersistedFilters();
    }

    /**
     * Clear only the filter SharedPreferences (PREFS_NAME) used by this fragment.
     * Keeps other SharedPreferences in the app untouched.
     * Also resets in-memory applied filters and UI state.
     */
    private void clearPersistedFilters() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().apply();
        }
        if (appliedFilters != null) appliedFilters.clear();
        // reset UI filter state
        updateFilterButtonsState();
        // re-apply filters (no filters => show all)
        applyAllFilters();
        // hide options panel
        if (filterOptionsContainer != null) filterOptionsContainer.setVisibility(View.GONE);
        currentCategoryForEditing = "";
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        filterOptionsContainer = view.findViewById(R.id.filter_options_container);
        radioGroupFilterOptions = view.findViewById(R.id.radio_group_filter_options);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnResetFilter = view.findViewById(R.id.btn_reset_filter);

        // Grab the category buttons so we can update style when filters active
        btnFilterSleep = view.findViewById(R.id.btn_filter_sleep);
        btnFilterStudy = view.findViewById(R.id.btn_filter_study);
        btnFilterEntertainment = view.findViewById(R.id.btn_filter_entertainment);
        btnFilterSports = view.findViewById(R.id.btn_filter_sports);
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

        btnFilterSleep.setOnClickListener(filterButtonClickListener);
        btnFilterStudy.setOnClickListener(filterButtonClickListener);
        btnFilterEntertainment.setOnClickListener(filterButtonClickListener);
        btnFilterSports.setOnClickListener(filterButtonClickListener);

        btnApplyFilter.setOnClickListener(v -> {
            int selectedId = radioGroupFilterOptions.getCheckedRadioButtonId();
            if (selectedId != -1) {
                // Ensure we don't save when no category is selected (defensive)
                if (currentCategoryForEditing == null || currentCategoryForEditing.isEmpty()) {
                    Toast.makeText(getContext(), "Không có danh mục bộ lọc nào đang mở.", Toast.LENGTH_SHORT).show();
                    return;
                }

                RadioButton selectedRadioButton = radioGroupFilterOptions.findViewById(selectedId);
                if (selectedRadioButton == null) {
                    Toast.makeText(getContext(), "Lỗi khi đọc tùy chọn đã chọn.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedOption = selectedRadioButton.getText().toString();

                // Save the selected filter in-memory
                appliedFilters.put(currentCategoryForEditing, selectedOption);

                // Persist it only for the running session. (It will be cleared onStop/onDestroyView)
                saveFilterState(currentCategoryForEditing, selectedOption);

                // Apply all filters and update UI
                applyAllFilters();
                filterOptionsContainer.setVisibility(View.GONE);
                updateFilterButtonsState();

                Toast.makeText(getContext(), "Đã áp dụng: " + selectedOption, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        // Reset only the current category on short click; long click clears all filters
        btnResetFilter.setOnClickListener(v -> {
            if (currentCategoryForEditing == null || currentCategoryForEditing.isEmpty()) {
                Toast.makeText(getContext(), "Không có danh mục bộ lọc nào đang mở.", Toast.LENGTH_SHORT).show();
                return;
            }
            appliedFilters.remove(currentCategoryForEditing);
            clearFilterState(currentCategoryForEditing);

            applyAllFilters();
            filterOptionsContainer.setVisibility(View.GONE);
            updateFilterButtonsState();
            Toast.makeText(getContext(), "Đã xóa bộ lọc cho " + currentCategoryForEditing, Toast.LENGTH_SHORT).show();
        });

        btnResetFilter.setOnLongClickListener(v -> {
            // Clear all filters persisted and in-memory for current session
            appliedFilters.clear();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            applyAllFilters();
            filterOptionsContainer.setVisibility(View.GONE);
            updateFilterButtonsState();
            Toast.makeText(getContext(), "Đã xóa tất cả bộ lọc", Toast.LENGTH_SHORT).show();
            return true;
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
            currentCategoryForEditing = "";
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
                // ensure unique id so radioGroup.getCheckedRadioButtonId() works reliably
                radioButton.setId(View.generateViewId());
                radioButton.setText(option);
                radioButton.setTextSize(16f);
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                radioButton.setPadding(pad, pad, pad, pad);
                // center vertically a bit nicer if needed
                RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(8, 8, 8, 8);
                radioButton.setLayoutParams(lp);

                if (option.equals(previouslySelectedOption)) {
                    radioButton.setChecked(true); // Restore checked state
                }
                radioGroupFilterOptions.addView(radioButton);
            }
        }

        // Make sure container is visible now
        filterOptionsContainer.setVisibility(View.VISIBLE);
    }

    private void applyAllFilters() {
        // Start from all templates. Use defensive copy so original list untouched.
        List<ScheduleTemplate> filteredList = new ArrayList<>(allTemplates);

        // Sequentially apply each filter from the map (AND semantics)
        for (Map.Entry<String, String> entry : appliedFilters.entrySet()) {
            String category = entry.getKey();
            String option = entry.getValue();
            String optionKey = category + ":" + option;
            String optionTag = optionTagMap.get(optionKey);

            if (optionTag != null && !optionTag.isEmpty()) {
                final String finalTag = optionTag;
                filteredList = filteredList.stream()
                        .filter(template -> template.getTags() != null && template.getTags().contains(finalTag))
                        .collect(Collectors.toList());
            }
        }

        currentlyDisplayedTemplates = filteredList;
        // adapter should implement updateList(...) to set new data and notify
        adapter.updateList(filteredList);
    }

    // --- SharedPreferences Logic ---
    // Note: We still write/read PREFS_NAME during app session, but we clear it onStop so it won't persist between app runs.

    private void saveFilterState(String category, String option) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(category, option).apply();
        }
    }

    private void clearFilterState(String category) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().remove(category).apply();
        }
    }

    private void loadFiltersFromPrefs() {
        // load saved filters into appliedFilters (for current session only)
        for (String category : filterOptionsMap.keySet()) {
            String savedOption = sharedPreferences.getString(category, null);
            if (savedOption != null) {
                appliedFilters.put(category, savedOption);
            }
        }
    }


    private void updateFilterButtonsState() {
        updateSingleFilterButton(btnFilterSleep, "Giờ ngủ");
        updateSingleFilterButton(btnFilterStudy, "Học tập");
        updateSingleFilterButton(btnFilterEntertainment, "Giải trí");
        updateSingleFilterButton(btnFilterSports, "Thể thao");
    }

    private void updateSingleFilterButton(Button btn, String category) {
        if (btn == null) return;
        String applied = appliedFilters.get(category);
        if (applied != null && !applied.isEmpty()) {
            btn.setAlpha(1f);
            // show small suffix so user knows what's active
            btn.setText(category + " (" + applied + ")");
        } else {
            btn.setAlpha(0.65f);
            btn.setText(category);
        }
    }

    // --- Data Loading ---
    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        allTemplates.addAll(ScheduleData.getSampleTemplates());
    }
}