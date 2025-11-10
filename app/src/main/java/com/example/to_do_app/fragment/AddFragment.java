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
    private List<ScheduleTemplate> allTemplates; // Full list
    private List<ScheduleTemplate> currentlyDisplayedTemplates; // List hiển thị

    private LinearLayout filterOptionsContainer;
    private RadioGroup radioGroupFilterOptions;
    private Button btnApplyFilter, btnResetFilter;

    // Data for filters
    private Map<Integer, String> filterCategoryMap;
    private Map<String, List<String>> filterOptionsMap;
    private Map<String, String> categoryTagMap;
    private Map<String, String> optionTagMap;
    private String currentFilterCategory = "";

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FilterPrefs";
    private static final String KEY_CATEGORY = "filter_category";
    private static final String KEY_OPTION = "filter_option";

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
        setupListeners();
        setupRecyclerView();

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String savedCategory = sharedPreferences.getString(KEY_CATEGORY, null);
        String savedOption = sharedPreferences.getString(KEY_OPTION, null);

        if (savedCategory != null && savedOption != null) {
            currentFilterCategory = savedCategory;
            applyFilter(savedCategory, savedOption);

            if (currentFilterCategory.equals("Giờ ngủ")) {
                filterOptionsContainer.setVisibility(View.VISIBLE);
                radioGroupFilterOptions.removeAllViews();
                RadioButton rb = new RadioButton(getContext());
                rb.setText(savedOption);
                rb.setChecked(true);
                rb.setTextSize(16f);
                rb.setPadding(32, 32, 32, 32);
                radioGroupFilterOptions.addView(rb);
            }
        }
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

        categoryTagMap = new HashMap<>();
        categoryTagMap.put("Học tập", "HocTap");
        categoryTagMap.put("Thể thao", "TheThao");
        categoryTagMap.put("Giải trí", "GiaiTri");
        categoryTagMap.put("Giờ ngủ", "GioNgu");

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
                applyFilter(currentFilterCategory, selectedOption);

                sharedPreferences.edit()
                        .putString(KEY_CATEGORY, currentFilterCategory)
                        .putString(KEY_OPTION, selectedOption)
                        .apply();

                Toast.makeText(getContext(), "Đã áp dụng: " + selectedOption, Toast.LENGTH_SHORT).show();

                if (currentFilterCategory.equals("Giờ ngủ")) {
                    radioGroupFilterOptions.removeAllViews();
                    RadioButton rb = new RadioButton(getContext());
                    rb.setText(selectedOption);
                    rb.setChecked(true);
                    rb.setTextSize(16f);
                    rb.setPadding(32, 32, 32, 32);
                    radioGroupFilterOptions.addView(rb);
                } else {
                    filterOptionsContainer.setVisibility(View.GONE);
                }

            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetFilter.setOnClickListener(v -> {
            if (currentFilterCategory.equals("Giờ ngủ")) {
                showFullSleepOptions();
            }

            radioGroupFilterOptions.clearCheck();
            resetFilter();
            sharedPreferences.edit().clear().apply();
            filterOptionsContainer.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Đã thiết lập lại bộ lọc", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new ScheduleTemplateAdapter(getContext(), currentlyDisplayedTemplates);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void showFullSleepOptions() {
        radioGroupFilterOptions.removeAllViews();
        List<String> sleepOptions = filterOptionsMap.get("Giờ ngủ");
        if (sleepOptions != null) {
            for (String opt : sleepOptions) {
                RadioButton rb = new RadioButton(getContext());
                rb.setText(opt);
                rb.setTextSize(16f);
                rb.setPadding(32, 32, 32, 32);
                radioGroupFilterOptions.addView(rb);
            }
        }
    }

    private void toggleFilterOptions(String category) {
        if (filterOptionsContainer.getVisibility() == View.VISIBLE && category.equals(currentFilterCategory)) {
            filterOptionsContainer.setVisibility(View.GONE);
            return;
        }

        currentFilterCategory = category;
        radioGroupFilterOptions.clearCheck();
        radioGroupFilterOptions.removeAllViews();

        List<String> options = filterOptionsMap.get(category);
        if (options != null) {
            for (String option : options) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(option);
                radioButton.setTextSize(16f);
                radioButton.setPadding(32, 32, 32, 32);
                radioGroupFilterOptions.addView(radioButton);
            }
        }
        filterOptionsContainer.setVisibility(View.VISIBLE);
    }

    private void applyFilter(String category, String option) {
        String categoryTag = categoryTagMap.getOrDefault(category, "");
        String optionKey = category + ":" + option;
        String optionTag = optionTagMap.getOrDefault(optionKey, "");

        List<ScheduleTemplate> result;

        // Sửa lỗi: Ưu tiên lọc theo tag tùy chọn cụ thể trước
        if (!optionTag.isEmpty()) {
            result = allTemplates.stream()
                    .filter(template -> template.getTags().contains(optionTag))
                    .collect(Collectors.toList());
        } 
        // Nếu không có tag tùy chọn, thử lọc theo tag danh mục chung
        else if (!categoryTag.isEmpty()) {
            result = allTemplates.stream()
                    .filter(template -> template.getTags().contains(categoryTag))
                    .collect(Collectors.toList());
        } 
        // Nếu không có tag nào, tìm kiếm theo văn bản
        else {
            final String optLower = option.toLowerCase();
            result = allTemplates.stream()
                    .filter(template -> template.getTitle().toLowerCase().contains(optLower) ||
                            template.getDescription().toLowerCase().contains(optLower))
                    .collect(Collectors.toList());
        }

        adapter.updateList(result);
    }

    private void resetFilter() {
        adapter.updateList(allTemplates);
    }

    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        allTemplates.addAll(ScheduleData.getSampleTemplates());
    }
}
