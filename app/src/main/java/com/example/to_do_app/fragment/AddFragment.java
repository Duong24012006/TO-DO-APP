package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleTemplateAdapter;
import com.example.to_do_app.model.ScheduleTemplate;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AddFragment wired to add_fragment.xml (search, preset chips, filter buttons, filter options area, apply/reset, recycler).
 *
 * Behavior:
 * - Search by title/description/tags (search_edit_text)
 * - Preset chips quick filters (preset_chips)
 * - Filter buttons open filter_options_container
 * - Filter options are rendered as checkboxes dynamically (multi-select) inside the filter_options_container
 * - Selecting an option applies the filter immediately (no need to press "Áp dụng")
 * - Reset clears filters and persisted selection
 * - Filters persisted to SharedPreferences
 */
public class AddFragment extends Fragment {

    private RecyclerView recyclerView;
    private ScheduleTemplateAdapter adapter;
    private List<ScheduleTemplate> allTemplates; // full list
    private List<ScheduleTemplate> currentlyDisplayedTemplates; // current shown list

    private TextInputEditText searchEditText;
    private ChipGroup presetChips;

    private LinearLayout filterOptionsContainer;
    private android.widget.Button btnApplyFilter, btnResetFilter;

    private Map<Integer, String> filterCategoryMap;
    private Map<String, List<String>> filterOptionsMap;
    private Map<String, String> categoryTagMap;
    private Map<String, String> optionTagMap;

    // dynamic checkbox container placed into filterOptionsContainer
    private LinearLayout optionsCheckboxContainer;

    // Selected filter state
    private final Set<String> selectedCategories = new HashSet<>();
    private final Map<String, Set<String>> selectedOptions = new HashMap<>();
    private String currentlyShowingCategory = null;

    // persistence
    private static final String PREFS_FILTERS = "add_fragment_filters";
    private static final String PREF_KEY_CATEGORIES = "selected_categories";
    private static final String PREF_KEY_OPTIONS = "selected_options";
    private SharedPreferences prefs;

    public AddFragment() { /* required empty constructor */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefs = requireContext().getSharedPreferences(PREFS_FILTERS, Context.MODE_PRIVATE);

        // bind views
        recyclerView = view.findViewById(R.id.recyclerView);
        searchEditText = view.findViewById(R.id.search_edit_text);
        presetChips = view.findViewById(R.id.preset_chips);
        filterOptionsContainer = view.findViewById(R.id.filter_options_container);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnResetFilter = view.findViewById(R.id.btn_reset_filter);

        // dynamic checkbox container
        optionsCheckboxContainer = new LinearLayout(requireContext());
        optionsCheckboxContainer.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(requireContext());
        sv.addView(optionsCheckboxContainer);
        if (filterOptionsContainer != null) {
            filterOptionsContainer.removeAllViews();
            filterOptionsContainer.addView(sv);
            filterOptionsContainer.setVisibility(View.GONE);
        }

        initializeFilterData();
        loadSampleData();
        restoreFiltersFromPrefs(); // restore before rendering

        currentlyDisplayedTemplates = new ArrayList<>(allTemplates);
        adapter = new ScheduleTemplateAdapter(requireContext(), currentlyDisplayedTemplates);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        setupSearch();
        setupPresetChips();
        setupFilterButtons();
        setupApplyReset();

        // apply restored filters immediately if present
        if (!selectedCategories.isEmpty() || selectedOptions.values().stream().anyMatch(s -> !s.isEmpty())) {
            applyFiltersAndShowResults();
        } else {
            adapter.updateList(allTemplates);
        }
    }

    private void initializeFilterData() {
        // categories mapped to button IDs present in add_fragment.xml
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

        categoryTagMap = new HashMap<>();
        categoryTagMap.put("Học tập", "#HocTap");
        categoryTagMap.put("Thể thao", "#TheThao");
        categoryTagMap.put("Giải trí", "#GiaiTri");
        categoryTagMap.put("Giờ ngủ", "#GioNgu");

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

        // initialize empty selection sets for every category key we know about
        for (String c : filterOptionsMap.keySet()) selectedOptions.put(c, new HashSet<>());
    }

    // ----------------- UI setup helpers -----------------

    private void setupSearch() {
        if (searchEditText == null) return;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s == null ? "" : s.toString().trim().toLowerCase();
                if (q.isEmpty()) {
                    applyFiltersAndShowResults();
                    return;
                }
                List<ScheduleTemplate> filtered = allTemplates.stream()
                        .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(q))
                                || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q))
                                || (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(q))))
                        .collect(Collectors.toList());
                updateResults(filtered);
            }
        });
    }

    private void setupPresetChips() {
        if (presetChips == null) return;
        presetChips.removeAllViews();
        for (ScheduleTemplate t : allTemplates) {
            Chip c = new Chip(requireContext());
            c.setText(t.getTitle());
            c.setCheckable(false);
            c.setOnClickListener(v -> {
                if (searchEditText != null) {
                    searchEditText.setText(t.getTitle());
                    searchEditText.setSelection(t.getTitle().length());
                }
                updateResults(Arrays.asList(t));
            });
            presetChips.addView(c);
        }
    }

    private void setupFilterButtons() {
        View root = requireView();
        if (root == null) return;
        View.OnClickListener filterBtnListener = v -> {
            String category = filterCategoryMap.get(v.getId());
            if (category == null) return;
            showOptionsForCategory(category);
        };
        if (root.findViewById(R.id.btn_filter_sleep) != null) root.findViewById(R.id.btn_filter_sleep).setOnClickListener(filterBtnListener);
        if (root.findViewById(R.id.btn_filter_study) != null) root.findViewById(R.id.btn_filter_study).setOnClickListener(filterBtnListener);
        if (root.findViewById(R.id.btn_filter_entertainment) != null) root.findViewById(R.id.btn_filter_entertainment).setOnClickListener(filterBtnListener);
        if (root.findViewById(R.id.btn_filter_sports) != null) root.findViewById(R.id.btn_filter_sports).setOnClickListener(filterBtnListener);
    }

    private void setupApplyReset() {
        if (btnApplyFilter != null) {
            btnApplyFilter.setOnClickListener(v -> {
                applyFiltersAndShowResults();
                if (filterOptionsContainer != null) filterOptionsContainer.setVisibility(View.GONE);
            });
        }
        if (btnResetFilter != null) {
            btnResetFilter.setOnClickListener(v -> {
                resetFilters();
                if (filterOptionsContainer != null) filterOptionsContainer.setVisibility(View.GONE);
            });
        }
    }

    // ----------------- Filter UI & logic -----------------

    private void showOptionsForCategory(String category) {
        if (filterOptionsContainer == null) return;

        // toggle close if same category shown
        if (filterOptionsContainer.getVisibility() == View.VISIBLE && category.equals(currentlyShowingCategory)) {
            filterOptionsContainer.setVisibility(View.GONE);
            currentlyShowingCategory = null;
            return;
        }

        currentlyShowingCategory = category;
        optionsCheckboxContainer.removeAllViews();

        // header
        TextView header = new TextView(requireContext());
        header.setText("Tùy chọn: " + category);
        header.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(8));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        optionsCheckboxContainer.addView(header);

        List<String> options = filterOptionsMap.get(category);
        if (options != null) {
            for (String opt : options) {
                CheckBox cb = new CheckBox(requireContext());
                cb.setText(opt);
                cb.setChecked(selectedOptions.getOrDefault(category, new HashSet<>()).contains(opt));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Set<String> set = selectedOptions.get(category);
                    if (set == null) {
                        set = new HashSet<>();
                        selectedOptions.put(category, set);
                    }
                    if (isChecked) {
                        set.add(opt);
                        selectedCategories.add(category);
                    } else {
                        set.remove(opt);
                        if (set.isEmpty()) selectedCategories.remove(category);
                    }
                    persistFiltersToPrefs();
                    // Apply immediately when user toggles a checkbox
                    applyFiltersAndShowResults();
                });
                optionsCheckboxContainer.addView(cb);
            }
        }

        // category toggle row
        CheckBox catToggle = new CheckBox(requireContext());
        catToggle.setText("Chọn danh mục: " + category);
        catToggle.setChecked(selectedCategories.contains(category));
        catToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedCategories.add(category);
            else {
                selectedCategories.remove(category);
                Set<String> set = selectedOptions.get(category);
                if (set != null) {
                    set.clear();
                    // uncheck checkboxes (skip header)
                    for (int i = 1; i < optionsCheckboxContainer.getChildCount(); i++) {
                        View child = optionsCheckboxContainer.getChildAt(i);
                        if (child instanceof CheckBox) ((CheckBox) child).setChecked(false);
                    }
                }
            }
            persistFiltersToPrefs();
            applyFiltersAndShowResults();
        });
        optionsCheckboxContainer.addView(catToggle, 0);

        filterOptionsContainer.setVisibility(View.VISIBLE);
    }

    private void applyFiltersAndShowResults() {
        boolean anyCategory = !selectedCategories.isEmpty();
        boolean anyOption = selectedOptions.values().stream().anyMatch(s -> s != null && !s.isEmpty());
        if (!anyCategory && !anyOption) {
            adapter.updateList(allTemplates);
            Toast.makeText(requireContext(), "Hiển thị tất cả mẫu", Toast.LENGTH_SHORT).show();
            persistFiltersToPrefs();
            return;
        }

        List<ScheduleTemplate> result = new ArrayList<>();
        for (ScheduleTemplate t : allTemplates) {
            boolean ok = true;

            // For each selected category ensure template matches category and at least one chosen option (if any)
            for (String cat : selectedCategories) {
                String requiredTag = categoryTagMap.getOrDefault(cat, "");
                boolean catMatched = false;
                if (!requiredTag.isEmpty() && t.getTags() != null && t.getTags().contains(requiredTag)) catMatched = true;
                else if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(cat.toLowerCase()))
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(cat.toLowerCase()))
                        || (t.getTags() != null && t.getTags().stream().anyMatch(tt -> tt.toLowerCase().contains(cat.toLowerCase())))) catMatched = true;

                if (!catMatched) { ok = false; break; }

                Set<String> opts = selectedOptions.getOrDefault(cat, new HashSet<>());
                if (!opts.isEmpty()) {
                    boolean optOk = false;
                    for (String opt : opts) {
                        String key = cat + ":" + opt;
                        String optTag = optionTagMap.getOrDefault(key, "");
                        if (!optTag.isEmpty() && t.getTags() != null && t.getTags().contains(optTag)) { optOk = true; break; }
                        else {
                            String ol = opt.toLowerCase();
                            if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(ol))
                                    || (t.getDescription() != null && t.getDescription().toLowerCase().contains(ol))
                                    || (t.getTags() != null && t.getTags().stream().anyMatch(tt -> tt.toLowerCase().contains(ol)))) {
                                optOk = true; break;
                            }
                        }
                    }
                    if (!optOk) { ok = false; break; }
                }
            }

            // Option-only case (no categories selected)
            if (selectedCategories.isEmpty() && anyOption && ok) {
                for (Map.Entry<String, Set<String>> e : selectedOptions.entrySet()) {
                    Set<String> group = e.getValue();
                    if (group == null || group.isEmpty()) continue;
                    boolean groupMatched = false;
                    for (String opt : group) {
                        String key = e.getKey() + ":" + opt;
                        String optTag = optionTagMap.getOrDefault(key, "");
                        if (!optTag.isEmpty() && t.getTags() != null && t.getTags().contains(optTag)) { groupMatched = true; break; }
                        else {
                            String ol = opt.toLowerCase();
                            if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(ol))
                                    || (t.getDescription() != null && t.getDescription().toLowerCase().contains(ol))
                                    || (t.getTags() != null && t.getTags().stream().anyMatch(tt -> tt.toLowerCase().contains(ol)))) {
                                groupMatched = true; break;
                            }
                        }
                    }
                    if (!groupMatched) { ok = false; break; }
                }
            }

            if (ok) result.add(t);
        }

        adapter.updateList(result);
        String summary = buildFilterSummary(selectedCategories, selectedOptions);
        Toast.makeText(requireContext(), "Bộ lọc: " + summary + " — tìm thấy " + result.size() + " mẫu", Toast.LENGTH_SHORT).show();
        persistFiltersToPrefs();
    }

    private String buildFilterSummary(Set<String> cats, Map<String, Set<String>> opts) {
        if ((cats == null || cats.isEmpty()) && (opts == null || opts.isEmpty())) return "Không có";
        StringBuilder sb = new StringBuilder();
        for (String c : cats) {
            sb.append(c);
            Set<String> o = opts.get(c);
            if (o != null && !o.isEmpty()) sb.append("(").append(String.join(",", o)).append(")");
            sb.append("; ");
        }
        for (Map.Entry<String, Set<String>> e : opts.entrySet()) {
            if (cats.contains(e.getKey())) continue;
            if (e.getValue() != null && !e.getValue().isEmpty()) sb.append(e.getKey()).append("(").append(String.join(",", e.getValue())).append("); ");
        }
        String res = sb.toString().trim();
        if (res.endsWith(";")) res = res.substring(0, res.length() - 1);
        if (res.isEmpty()) res = "Không có";
        return res;
    }

    private void resetFilters() {
        selectedCategories.clear();
        for (Set<String> s : selectedOptions.values()) s.clear();
        adapter.updateList(allTemplates);
        Toast.makeText(requireContext(), "Đã xóa bộ lọc", Toast.LENGTH_SHORT).show();
        persistFiltersToPrefs();
    }

    private void persistFiltersToPrefs() {
        String cats = String.join(",", selectedCategories);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> e : selectedOptions.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            sb.append(escape(e.getKey())).append("|")
                    .append(e.getValue().stream().map(this::escape).collect(Collectors.joining(",")))
                    .append(";;");
        }
        prefs.edit().putString(PREF_KEY_CATEGORIES, cats).putString(PREF_KEY_OPTIONS, sb.toString()).apply();
    }

    private void restoreFiltersFromPrefs() {
        String cats = prefs.getString(PREF_KEY_CATEGORIES, "");
        if (!TextUtils.isEmpty(cats)) {
            String[] parts = cats.split(",");
            for (String p : parts) if (!p.trim().isEmpty()) selectedCategories.add(p.trim());
        }
        String opts = prefs.getString(PREF_KEY_OPTIONS, "");
        if (!TextUtils.isEmpty(opts)) {
            String[] groups = opts.split(";;");
            for (String g : groups) {
                if (g.trim().isEmpty()) continue;
                String[] kv = g.split("\\|", 2);
                if (kv.length < 2) continue;
                String cat = unescape(kv[0]);
                String[] os = kv[1].split(",");
                Set<String> set = selectedOptions.get(cat);
                if (set == null) { set = new HashSet<>(); selectedOptions.put(cat, set); }
                for (String o : os) {
                    String u = unescape(o);
                    if (!u.trim().isEmpty()) set.add(u.trim());
                }
                if (!set.isEmpty()) selectedCategories.add(cat);
            }
        }
    }

    private String escape(String s) {
        return s.replace("|", "%7C").replace(",", "%2C").replace(";;", "%3B%3B");
    }
    private String unescape(String s) {
        return s.replace("%7C", "|").replace("%2C", ",").replace("%3B%3B", ";;");
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

    private void updateResults(List<ScheduleTemplate> list) {
        currentlyDisplayedTemplates = new ArrayList<>(list);
        adapter.updateList(currentlyDisplayedTemplates);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}