package com.example.to_do_app.fragment;

import android.content.Intent;
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
import com.example.to_do_app.activitys.Layout6Activity;
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
    private List<ScheduleTemplate> allTemplates; // Full list
    private List<ScheduleTemplate> currentlyDisplayedTemplates; // List hiển thị

    private LinearLayout filterOptionsContainer;
    private RadioGroup radioGroupFilterOptions;
    private Button btnApplyFilter, btnResetFilter;

    private Map<Integer, String> filterCategoryMap;
    private Map<String, List<String>> filterOptionsMap;
    private Map<String, String> categoryTagMap;
    private Map<String, String> optionTagMap; // optional mapping from option text to tag (if available)
    private String currentFilterCategory = "";

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
        filterOptionsMap.put("Học tập", Arrays.asList("2 giờ", "4 giờ", "6 giờ"));
        filterOptionsMap.put("Giải trí", Arrays.asList("30 phút", "60 phút", "90 phút"));
        filterOptionsMap.put("Thể thao", Arrays.asList("30 phút", "60 phút", "90 phút"));

        // Map category -> main tag used to identify templates of that category
        categoryTagMap = new HashMap<>();
        categoryTagMap.put("Học tập", "#HocTap");
        categoryTagMap.put("Thể thao", "#TheThao");
        categoryTagMap.put("Giải trí", "#GiaiTri");
        categoryTagMap.put("Giờ ngủ", "#GioNgu"); // fixed tag for sleep

        // Option tag map is optional: if you have tags for specific options (e.g., durations),
        // map them here so filter will be able to filter by both category and option.
        // If your templates don't include such tags, the option filter will try to match the option
        // text inside title or description as a fallback.
        optionTagMap = new HashMap<>();
        optionTagMap.put("Giờ ngủ:4 giờ", "#4h_sleep");
        optionTagMap.put("Giờ ngủ:6 giờ", "#6h_sleep");
        optionTagMap.put("Giờ ngủ:8 giờ", "#8h_sleep");

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

        btnApplyFilter.setOnClickListener(v -> {
            int selectedId = radioGroupFilterOptions.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = requireView().findViewById(selectedId);
                String selectedOption = selectedRadioButton.getText().toString();
                applyFilter(currentFilterCategory, selectedOption);
                filterOptionsContainer.setVisibility(View.GONE);
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetFilter.setOnClickListener(v -> {
            resetFilter();
            filterOptionsContainer.setVisibility(View.GONE);
        });
    }

    private void setupRecyclerView() {
        adapter = new ScheduleTemplateAdapter(getContext(), currentlyDisplayedTemplates);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void toggleFilterOptions(String category) {
        if (filterOptionsContainer.getVisibility() == View.VISIBLE && category.equals(currentFilterCategory)) {
            filterOptionsContainer.setVisibility(View.GONE);
            return;
        }

        currentFilterCategory = category;
        List<String> options = filterOptionsMap.get(category);
        radioGroupFilterOptions.clearCheck();
        radioGroupFilterOptions.removeAllViews();

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
        // 1) find the category tag (if any)
        String categoryTag = categoryTagMap.getOrDefault(category, "");

        // 2) find the option tag (if any)
        String optionKey = category + ":" + option; // consistent key used in optionTagMap
        String optionTag = optionTagMap.getOrDefault(optionKey, "");


        List<ScheduleTemplate> result = new ArrayList<>(allTemplates);

        if (!categoryTag.isEmpty() && !optionTag.isEmpty()) {
            final String ct = categoryTag;
            final String ot = optionTag;
            result = allTemplates.stream()
                    .filter(template -> template.getTags().contains(ct) && template.getTags().contains(ot))
                    .collect(Collectors.toList());
        } else if (!categoryTag.isEmpty()) {
            final String ct = categoryTag;
            result = allTemplates.stream()
                    .filter(template -> template.getTags().contains(ct))
                    .collect(Collectors.toList());
        } else if (!optionTag.isEmpty()) {
            final String ot = optionTag;
            result = allTemplates.stream()
                    .filter(template -> template.getTags().contains(ot))
                    .collect(Collectors.toList());
        } else {
            // Fallback: if there is no tag mapping available for this option/category,
            // attempt to match the option text inside title or description AND match category if possible.
            final String optLower = option.toLowerCase();
            if (!categoryTag.isEmpty()) {
                final String ct = categoryTag;
                result = allTemplates.stream()
                        .filter(template -> template.getTags().contains(ct) &&
                                (template.getTitle().toLowerCase().contains(optLower) ||
                                        template.getDescription().toLowerCase().contains(optLower)))
                        .collect(Collectors.toList());
            } else {
                result = allTemplates.stream()
                        .filter(template -> template.getTitle().toLowerCase().contains(optLower) ||
                                template.getDescription().toLowerCase().contains(optLower) ||
                                template.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(optLower)))
                        .collect(Collectors.toList());
            }
        }

        adapter.updateList(result);

        Toast.makeText(getContext(), "Lọc theo: " + category + " - " + option, Toast.LENGTH_SHORT).show();
    }

    private void resetFilter() {
        adapter.updateList(allTemplates);
        Toast.makeText(getContext(), "Đã xóa bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        allTemplates.add(new ScheduleTemplate(
                "Lịch học cho sinh viên",
                "Template này dành cho sinh viên muốn tối đa hóa thời gian học tập nhưng với mức ngủ tối thiểu",
                Arrays.asList("#HocTap", "#SinhVien", "#4h_study")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch trình thể thao",
                "Tối ưu hóa thời gian tập luyện và phục hồi để đạt hiệu suất cao nhất trong thể thao.",
                Arrays.asList("#TheThao", "#CoHoi", "#60m_sport")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch trình giải trí cuối tuần",
                "Dành thời gian để thư giãn, giải trí và nạp lại năng lượng sau một tuần làm việc căng thẳng.",
                Arrays.asList("#GiaiTri", "#CuoiTuan", "#60m_fun")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch trình ăn uống lành mạnh",
                "Thiết lập một chế độ ăn uống cân bằng và khoa học để cải thiện sức khỏe và vóc dáng.",
                Arrays.asList("#AnUong", "#SucKhoe", "#SinhVien")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch ngủ sâu",
                "Tập trung vào chất lượng giấc ngủ để cải thiện năng lượng ngày tiếp theo.",
                Arrays.asList("#GioNgu", "#8h_sleep")
        ));
    }
}