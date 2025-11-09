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

        // Khôi phục bộ lọc đã lưu
        String savedCategory = sharedPreferences.getString(KEY_CATEGORY, null);
        String savedOption = sharedPreferences.getString(KEY_OPTION, null);

        if (savedCategory != null && savedOption != null) {
            currentFilterCategory = savedCategory;
            applyFilter(savedCategory, savedOption);
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
        filterOptionsMap.put("Giờ ngủ", Arrays.asList("4 giờ", "6 giờ", "12 giờ"));
        filterOptionsMap.put("Học tập", Arrays.asList("2 giờ", "4 giờ", "6 giờ"));
        filterOptionsMap.put("Giải trí", Arrays.asList("30 phút", "60 phút", "90 phút"));
        filterOptionsMap.put("Thể thao", Arrays.asList("30 phút", "60 phút", "90 phút"));

        categoryTagMap = new HashMap<>();
        categoryTagMap.put("Học tập", "#HocTap");
        categoryTagMap.put("Thể thao", "#TheThao");
        categoryTagMap.put("Giải trí", "#GiaiTri");
        categoryTagMap.put("Giờ ngủ", "#GioNgu");

        optionTagMap = new HashMap<>();
        optionTagMap.put("Giờ ngủ:4 giờ", "#4h_sleep");
        optionTagMap.put("Giờ ngủ:6 giờ", "#6h_sleep");
        optionTagMap.put("Giờ ngủ:12 giờ", "#12h_sleep");

        optionTagMap.put("Học tập:2 giờ", "#2h_study");
        optionTagMap.put("Học tập:4 giờ", "#4h_study");
        optionTagMap.put("Học tập:6 giờ", "#6h_study");

        optionTagMap.put("Giải trí:30 phút", "#30m_fun");
        optionTagMap.put("Giải trí:60 phút", "#60m_fun");
        optionTagMap.put("Giải trí:90 phút", "#90m_fun");

        optionTagMap.put("Thể thao:30 phút", "#30m_sport");
        optionTagMap.put("Thể thao:60 phút", "#60m_sport");
        optionTagMap.put("Thể thao:90 phút", "#90m_sport");
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

        // Áp dụng bộ lọc
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

                // ✅ Nếu là “Giờ ngủ” thì chỉ hiển thị lại lựa chọn được chọn
                if (currentFilterCategory.equals("Giờ ngủ")) {
                    radioGroupFilterOptions.removeAllViews();

                    RadioButton rb = new RadioButton(getContext());
                    rb.setText(selectedOption);
                    rb.setChecked(true);
                    rb.setTextSize(16f);
                    rb.setPadding(32, 32, 32, 32);
                    radioGroupFilterOptions.addView(rb);
                }

            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        // Thiết lập lại
        btnResetFilter.setOnClickListener(v -> {
            if (currentFilterCategory.equals("Giờ ngủ")) {
                showFullSleepOptions(); // Hiện lại đủ 3 lựa chọn
            }
            radioGroupFilterOptions.clearCheck();
            resetFilter();
            sharedPreferences.edit().clear().apply();
            Toast.makeText(getContext(), "Đã thiết lập lại bộ lọc", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new ScheduleTemplateAdapter(getContext(), currentlyDisplayedTemplates);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void toggleFilterOptions(String category) {
        // Nếu click lại cùng danh mục → ẩn
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
                RadioButton rb = new RadioButton(getContext());
                rb.setText(option);
                rb.setTextSize(16f);
                rb.setPadding(32, 32, 32, 32);
                radioGroupFilterOptions.addView(rb);
            }
        }

        filterOptionsContainer.setVisibility(View.VISIBLE);
    }

    private void showFullSleepOptions() {
        radioGroupFilterOptions.removeAllViews();
        List<String> sleepOptions = Arrays.asList("4 giờ", "6 giờ", "12 giờ");
        for (String opt : sleepOptions) {
            RadioButton rb = new RadioButton(getContext());
            rb.setText(opt);
            rb.setTextSize(16f);
            rb.setPadding(32, 32, 32, 32);
            radioGroupFilterOptions.addView(rb);
        }
    }

    private void applyFilter(String category, String option) {
        String categoryTag = categoryTagMap.getOrDefault(category, "");
        String optionKey = category + ":" + option;
        String optionTag = optionTagMap.getOrDefault(optionKey, "");

        List<ScheduleTemplate> result = new ArrayList<>(allTemplates);

        if (!categoryTag.isEmpty() && !optionTag.isEmpty()) {
            final String ct = categoryTag;
            final String ot = optionTag;
            result = allTemplates.stream()
                    .filter(t -> t.getTags().contains(ct) && t.getTags().contains(ot))
                    .collect(Collectors.toList());
        } else if (!categoryTag.isEmpty()) {
            final String ct = categoryTag;
            result = allTemplates.stream()
                    .filter(t -> t.getTags().contains(ct))
                    .collect(Collectors.toList());
        } else {
            final String optLower = option.toLowerCase();
            result = allTemplates.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(optLower) ||
                            t.getDescription().toLowerCase().contains(optLower))
                    .collect(Collectors.toList());
        }

        adapter.updateList(result);
    }

    private void resetFilter() {
        adapter.updateList(allTemplates);
    }

    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        allTemplates.add(new ScheduleTemplate(
                "Lịch học cho sinh viên",
                "Tối ưu thời gian học tập.",
                Arrays.asList("#HocTap", "#4h_study")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch thể thao buổi sáng",
                "Rèn luyện sức khỏe hiệu quả.",
                Arrays.asList("#TheThao", "#60m_sport")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch giải trí cuối tuần",
                "Dành thời gian thư giãn và giải trí.",
                Arrays.asList("#GiaiTri", "#60m_fun")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch ngủ sâu",
                "Giúp cơ thể phục hồi sau ngày làm việc.",
                Arrays.asList("#GioNgu", "#4h_sleep")
        ));
    }
}
