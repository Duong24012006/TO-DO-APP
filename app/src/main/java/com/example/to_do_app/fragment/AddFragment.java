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
    private String currentFilterCategory = "";

    // 🔹 SharedPreferences để lưu trạng thái lọc
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

        // 🔹 Khởi tạo SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 🔹 Khôi phục bộ lọc nếu có lưu trước đó
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
        filterOptionsMap.put("Giờ ngủ", Arrays.asList("4 giờ", "6 giờ", "8 giờ"));
        filterOptionsMap.put("Học tập", Arrays.asList("4 giờ", "6 giờ", "8 giờ"));
        filterOptionsMap.put("Giải trí", Arrays.asList("30 phút", "60 phút", "90 phút"));
        filterOptionsMap.put("Thể thao", Arrays.asList("30 phút", "60 phút", "90 phút"));
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

                // 🔹 Lưu lại bộ lọc vào SharedPreferences
                sharedPreferences.edit()
                        .putString(KEY_CATEGORY, currentFilterCategory)
                        .putString(KEY_OPTION, selectedOption)
                        .apply();

                filterOptionsContainer.setVisibility(View.GONE);
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một tùy chọn", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetFilter.setOnClickListener(v -> {
            resetFilter();

            // 🔹 Xóa dữ liệu lưu trữ
            sharedPreferences.edit().clear().apply();

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
        String tagToFilter;
        switch (category) {
            case "Học tập":
                tagToFilter = "#HocTap";
                break;
            case "Thể thao":
                tagToFilter = "#TheThao";
                break;
            case "Giải trí":
                tagToFilter = "#GiaiTri";
                break;
            case "Giờ ngủ":
                tagToFilter = "#SinhVien";
                break;
            default:
                tagToFilter = "";
                break;
        }

        if (tagToFilter.isEmpty()) {
            adapter.updateList(allTemplates);
            return;
        }

        String finalTagToFilter = tagToFilter;
        List<ScheduleTemplate> result = allTemplates.stream()
                .filter(template -> template.getTags().contains(finalTagToFilter))
                .collect(Collectors.toList());

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
                Arrays.asList("#HocTap", "#SinhVien")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch trình thể thao",
                "Tối ưu hóa thời gian tập luyện và phục hồi để đạt hiệu suất cao nhất trong thể thao.",
                Arrays.asList("#TheThao", "#CoHoi")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch trình giải trí cuối tuần",
                "Dành thời gian để thư giãn, giải trí và nạp lại năng lượng sau một tuần làm việc căng thẳng.",
                Arrays.asList("#GiaiTri", "#CuoiTuan")
        ));
        allTemplates.add(new ScheduleTemplate(
                "Lịch trình ăn uống lành mạnh",
                "Thiết lập một chế độ ăn uống cân bằng và khoa học để cải thiện sức khỏe và vóc dáng.",
                Arrays.asList("#AnUong", "#SucKhoe", "#SinhVien")
        ));
    }
}
