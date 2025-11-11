package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.example.to_do_app.data.ScheduleData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AddFragment — updated:
 *  - fix SharedPreferences init order (avoid NPE)
 *  - add item click handling on RecyclerView to open Layout6Activity and pass template data
 *  - robust click handling using RecyclerView.OnItemTouchListener + GestureDetector (no dependency on adapter API)
 */
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
    private Map<String, String> optionTagMap; // optional mapping from option text to tag (if available)
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

        // 🔹 Khởi tạo SharedPreferences BEFORE setupListeners so listeners can use it safely
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupListeners();
        setupRecyclerView();

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
        filterOptionsMap.put("Học tập", Arrays.asList("2 giờ", "4 giờ", "6 giờ"));
        filterOptionsMap.put("Giải trí", Arrays.asList("30 phút", "60 phút", "90 phút"));
        filterOptionsMap.put("Thể thao", Arrays.asList("30 phút", "60 phút", "90 phút"));

        // Map category -> main tag used to identify templates of that category
        categoryTagMap = new HashMap<>();
        categoryTagMap.put("Học tập", "#HocTap");
        categoryTagMap.put("Thể thao", "#TheThao");
        categoryTagMap.put("Giải trí", "#GiaiTri");
        categoryTagMap.put("Giờ ngủ", "#GioNgu"); // fixed tag for sleep

        optionTagMap = new HashMap<>();
        optionTagMap.put("Giờ ngủ:4 giờ", "#4h_sleep");
        optionTagMap.put("Giờ ngủ:6 giờ", "#6h_sleep");
        optionTagMap.put("Giờ ngủ:8 giờ", "#8h_sleep");

        optionTagMap.put("Học tập:2 giờ", "#2h_study");
        optionTagMap.put("Học tập:4 giờ", "#4h_study");
        optionTagMap.put("Học tập:6 giờ", "#6h_study");

        optionTagMap.put("Giải trí:30 phút", "#30m_relax");
        optionTagMap.put("Giải trí:60 phút", "#60m_relax");
        optionTagMap.put("Giải trí:90 phút", "#90m_relax");

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

        // Add item click handling without depending on adapter's API.
        final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent e) { return true; }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && gestureDetector.onTouchEvent(e)) {
                    int position = rv.getChildAdapterPosition(child);
                    if (position != RecyclerView.NO_POSITION && position < currentlyDisplayedTemplates.size()) {
                        ScheduleTemplate template = currentlyDisplayedTemplates.get(position);
                        openLayout6WithTemplate(template);
                        return true;
                    }
                }
                return false;
            }

            @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) { /* no-op */ }

            @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { /* no-op */ }
        });
    }

    /**
     * Open Layout6Activity with template details passed as Intent extras.
     * Keys used:
     *  - Layout6Activity.EXTRA_TEMPLATE_TITLE
     *  - Layout6Activity.EXTRA_TEMPLATE_DESCRIPTION
     *  - "EXTRA_TEMPLATE_TAGS" (ArrayList<String>) — Layout6Activity expects this key in existing code
     */
    private void openLayout6WithTemplate(ScheduleTemplate template) {
        if (template == null) return;
        Intent intent = new Intent(getContext(), Layout6Activity.class);
        intent.putExtra(Layout6Activity.EXTRA_TEMPLATE_TITLE, template.getTitle());
        intent.putExtra(Layout6Activity.EXTRA_TEMPLATE_DESCRIPTION, template.getDescription());
        ArrayList<String> tags = new ArrayList<>();
        if (template.getTags() != null) tags.addAll(template.getTags());
        intent.putStringArrayListExtra("EXTRA_TEMPLATE_TAGS", tags);
        startActivity(intent);
    }

    private void toggleFilterOptions(String category) {
        // If the same filter is clicked again while visible, hide it.
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

        // update currentlyDisplayedTemplates so click handling references correct list
        currentlyDisplayedTemplates.clear();
        currentlyDisplayedTemplates.addAll(result);

        adapter.updateList(result);

        Toast.makeText(getContext(), "Lọc theo: " + category + " - " + option, Toast.LENGTH_SHORT).show();
    }

    private void resetFilter() {
        currentlyDisplayedTemplates.clear();
        currentlyDisplayedTemplates.addAll(allTemplates);
        adapter.updateList(allTemplates);
        Toast.makeText(getContext(), "Đã xóa bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void loadSampleData() {
        allTemplates = new ArrayList<>();
        // Chỉ cần gọi một dòng duy nhất để lấy toàn bộ danh sách template
        allTemplates.addAll(ScheduleData.getSampleTemplates());
    }
}