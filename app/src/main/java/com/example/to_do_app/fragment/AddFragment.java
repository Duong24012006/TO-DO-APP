package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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
import com.google.android.material.textfield.TextInputEditText;

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
    private TextInputEditText searchEditText;
    private TextView tvNoResults;

    // Filter Data
    private Map<Integer, String> filterCategoryMap;
    private Map<String, List<String>> filterOptionsMap;
    private Map<String, String> optionTagMap;
    private Map<String, String> appliedFilters;

    private String currentCategoryForEditing = "";

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
        loadFiltersFromPrefs();
        setupListeners();
        setupRecyclerView();
        applyAllFilters();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        filterOptionsContainer = view.findViewById(R.id.filter_options_container);
        radioGroupFilterOptions = view.findViewById(R.id.radio_group_filter_options);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnResetFilter = view.findViewById(R.id.btn_reset_filter);
        searchEditText = view.findViewById(R.id.search_edit_text);
        tvNoResults = view.findViewById(R.id.tv_no_results);
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
            if (category != null) toggleFilterOptions(category);
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

                appliedFilters.put(currentCategoryForEditing, selectedOption);
                saveFilterState(currentCategoryForEditing, selectedOption);

                applyAllFilters();
                filterOptionsContainer.setVisibility(View.GONE);
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetFilter.setOnClickListener(v -> {
            appliedFilters.remove(currentCategoryForEditing);
            clearFilterState(currentCategoryForEditing);

            applyAllFilters();
            filterOptionsContainer.setVisibility(View.GONE);
        });

        // 🔎 SEARCH LISTENER
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                applyAllFilters();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ScheduleTemplateAdapter(getContext(), currentlyDisplayedTemplates);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void toggleFilterOptions(String category) {
        if (filterOptionsContainer.getVisibility() == View.VISIBLE &&
                category.equals(currentCategoryForEditing)) {
            filterOptionsContainer.setVisibility(View.GONE);
            return;
        }

        currentCategoryForEditing = category;
        radioGroupFilterOptions.clearCheck();
        radioGroupFilterOptions.removeAllViews();

        List<String> options = filterOptionsMap.get(category);
        String selectedPreviously = appliedFilters.get(category);

        if (options != null) {
            for (String option : options) {
                RadioButton rb = new RadioButton(getContext());
                rb.setText(option);
                rb.setTextSize(16f);
                rb.setPadding(32, 32, 32, 32);

                if (option.equals(selectedPreviously)) rb.setChecked(true);

                radioGroupFilterOptions.addView(rb);
            }
        }

        filterOptionsContainer.setVisibility(View.VISIBLE);
    }

    private void applyAllFilters() {
        List<ScheduleTemplate> filteredList = new ArrayList<>(allTemplates);

        // Apply category filters
        for (Map.Entry<String, String> entry : appliedFilters.entrySet()) {
            String category = entry.getKey();
            String option = entry.getValue();
            String optionTag = optionTagMap.get(category + ":" + option);

            if (optionTag != null) {
                filteredList = filteredList.stream()
                        .filter(t -> t.getTags().contains(optionTag))
                        .collect(Collectors.toList());
            }
        }

        // Apply search filter
        String keyword = searchEditText.getText().toString().trim().toLowerCase();

        if (!keyword.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(keyword)
                            || t.getDescription().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // Update UI
        adapter.updateList(filteredList);
        tvNoResults.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

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

    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        allTemplates.addAll(ScheduleData.getSampleTemplates());
    }
}
