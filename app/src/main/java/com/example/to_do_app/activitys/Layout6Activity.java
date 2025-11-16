package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleItemAdapter;
import com.example.to_do_app.data.ScheduleData;
import com.example.to_do_app.model.DetailedSchedule;
import com.example.to_do_app.model.ScheduleItem;
import com.example.to_do_app.model.ScheduleTemplate;
import com.example.to_do_app.model.TimeSlot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Layout6Activity extends AppCompatActivity {

    private static final String TAG = "Layout6Activity";

    public static final String EXTRA_SCHEDULE_TEMPLATE = "EXTRA_SCHEDULE_TEMPLATE";
    public static final String EXTRA_HISTORY_KEY = "EXTRA_HISTORY_KEY";


    private ImageView btnBack;
    private android.widget.TextView tvTitleHeader;
    private Button btnApplySchedule;
    private RecyclerView scheduleRecyclerView;
    private ScheduleItemAdapter scheduleAdapter;
    private List<ScheduleItem> currentList;

    private LinearLayout day2, day3, day4, day5, day6, day7, dayCN;
    private View selectedDayView;
    private int selectedDay = 2; // default Thứ 2

    private FloatingActionButton fabAdd;

    private DatabaseReference schedulesRef;
    private DatabaseReference rootRef;
    private String userId;

    private SharedPreferences prefs;


    private static final String PREFS_NAME = "profile_overrides";
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String PROFILE_HISTORY_KEY = "profile_history";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";
    private static final String PREF_ACTIVE_SCHEDULE = "active_schedule_name";
    private Context ctx;

    private String editingHistoryKey = null;
    private DetailedSchedule currentTemplateDetails = null;

   // java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manhinh_lichduocchon);

        currentList = new ArrayList<>();
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        rootRef = FirebaseDatabase.getInstance().getReference();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userId = FirebaseAuth.getInstance().getUid();

        // bind views first so view references are not null
        bindViews();

        setupRecyclerView();

        View root = findViewById(R.id.root_manhinh_lichduocchon);
        if (root != null) {
            final int padL = root.getPaddingLeft();
            final int padT = root.getPaddingTop();
            final int padR = root.getPaddingRight();

            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(padL + systemBars.left, padT + systemBars.top, padR + systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        userId = profilePrefs.getString("profile_user_id", null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString("profile_user_id", userId).apply();
        }

        selectedDay = getIntent().getIntExtra("selected_day", selectedDay);
        editingHistoryKey = getIntent().getStringExtra(EXTRA_HISTORY_KEY);

        ScheduleTemplate template = (ScheduleTemplate) getIntent().getSerializableExtra(EXTRA_SCHEDULE_TEMPLATE);
        if (template != null) {
            String passedTitle = template.getTitle();
            if (passedTitle != null && !passedTitle.isEmpty() && tvTitleHeader != null) {
                tvTitleHeader.setText(passedTitle);
                currentTemplateDetails = getTemplateDetailsByTitle(passedTitle);
            }
        }

        setupDays();
        setupListeners();
        loadScheduleDataForDay(selectedDay);
    }


    private void setupRecyclerView() {
        if (currentList == null) currentList = new ArrayList<>();
        scheduleAdapter = new ScheduleItemAdapter(this, currentList, new ScheduleItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, ScheduleItem item) {
                if (item == null) return;
                String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
                if (fk.startsWith("builtin_")) {
                    // builtin items cannot be deleted — inform user and open edit for overrides
                    showEditDialog(position, item);
                } else {
                    // user-added / local / firebase items -> confirm deletion
                    confirmAndDeleteItem(position, item);
                    Toast.makeText(Layout6Activity.this, "Mục này là mặc định và không thể xóa.", Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onEditClick(int position, ScheduleItem item) {
                showEditDialog(position, item);
            }
        });
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleRecyclerView.setAdapter(scheduleAdapter);
    }





    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnApplySchedule = findViewById(R.id.btnApplySchedule);
        scheduleRecyclerView = findViewById(R.id.scheduleRecyclerView);
        fabAdd = findViewById(R.id.fabAddSlot);
        tvTitleHeader = findViewById(R.id.tvTitleHeader);
        day2 = findViewById(R.id.day2);
        day3 = findViewById(R.id.day3);
        day4 = findViewById(R.id.day4);
        day5 = findViewById(R.id.day5);
        day6 = findViewById(R.id.day6);
        day7 = findViewById(R.id.day7);
        dayCN = findViewById(R.id.dayCN);
    };



    private void setupDays() {
        View.OnClickListener dayClick = v -> {
            if (selectedDayView != null) selectedDayView.setSelected(false);
            v.setSelected(true);
            selectedDayView = v;

            int id = v.getId();
            if (id == R.id.day2) selectedDay = 2;
            else if (id == R.id.day3) selectedDay = 3;
            else if (id == R.id.day4) selectedDay = 4;
            else if (id == R.id.day5) selectedDay = 5;
            else if (id == R.id.day6) selectedDay = 6;
            else if (id == R.id.day7) selectedDay = 7;
            else if (id == R.id.dayCN) selectedDay = 8;

            loadScheduleDataForDay(selectedDay);
        };

        day2.setOnClickListener(dayClick);
        day3.setOnClickListener(dayClick);
        day4.setOnClickListener(dayClick);
        day5.setOnClickListener(dayClick);
        day6.setOnClickListener(dayClick);
        day7.setOnClickListener(dayClick);
        dayCN.setOnClickListener(dayClick);

        day2.setSelected(true);
        selectedDayView = day2;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApplySchedule.setOnClickListener(v -> showApplyDialog());
        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddDialogMode());
    }


    private void loadScheduleForDay(int dayOfWeek) {
        // 1) load hidden overrides saved in prefs for this day (keys saved by your app)
        List<ScheduleItem> overrides = ScheduleData.getOverridesFromPrefs(ctx, dayOfWeek);
        hiddenBuiltins.clear();
        for (ScheduleItem o : overrides) {
            if (o != null && o.getFirebaseKey() != null && !o.getFirebaseKey().isEmpty()) {
                hiddenBuiltins.add(o.getFirebaseKey());
            }
        }

        // 2) load local-only user items (if any)
        List<ScheduleItem> localUserItems = ScheduleData.getLocalUserItems(ctx, dayOfWeek);

        // 3) load user items from Firebase 'schedules' node (single read)
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("schedules");
        ref.get().addOnCompleteListener(task -> {
            List<ScheduleItem> loadedItems = new ArrayList<>();

            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snap = task.getResult();
                for (DataSnapshot child : snap.getChildren()) {
                    ScheduleItem it = child.getValue(ScheduleItem.class);
                    if (it == null) continue;
                    // ensure firebaseKey present on model
                    if (it.getFirebaseKey() == null || it.getFirebaseKey().trim().isEmpty()) {
                        it.setFirebaseKey(child.getKey());
                    }
                    // only include items for requested day (assuming dayOfWeek stored)
                    if (it.getDayOfWeek() == dayOfWeek) {
                        loadedItems.add(it);
                    }
                }
            } else {
                // Firebase read failed or empty; we continue with what we have
            }

            // 4) add local-only items
            if (localUserItems != null && !localUserItems.isEmpty()) {
                loadedItems.addAll(localUserItems);
            }

            // 5) add template (builtin) items for that day (converted to ScheduleItem)
            // use ScheduleData.scheduleItemsFromTemplateForDay which marks items builtin and sets FK
            List<ScheduleTemplate> templates = ScheduleData.getSampleTemplates();
            if (templates != null && !templates.isEmpty()) {
                for (ScheduleTemplate st : templates) {
                    if (!(st instanceof DetailedSchedule)) continue;
                    DetailedSchedule ds = (DetailedSchedule) st;
                    List<ScheduleItem> fromTemplate = ScheduleData.scheduleItemsFromTemplateForDay(ds, dayOfWeek);
                    if (fromTemplate != null && !fromTemplate.isEmpty()) {
                        loadedItems.addAll(fromTemplate);
                    }
                }
            }

            // 6) Build display list applying hiddenBuiltins filter
            List<ScheduleItem> shown = ScheduleData.buildDisplayListFromLoaded(loadedItems, hiddenBuiltins);

            // 7) update local list and adapter on UI thread
            runOnUiThread(() -> {
                currentList.clear();
                currentList.addAll(shown);

                try {
                    // Prefer adapter-specific updateList(...) if your ScheduleItemAdapter exposes it
                    boolean updated = false;
                    if (scheduleAdapter != null) {
                        try {
                            scheduleAdapter.updateList(currentList);
                            updated = true;
                        } catch (Exception ignored) {
                            // adapter doesn't offer updateList or it failed; fall back below
                        }
                    }

                    if (!updated) {
                        RecyclerView.Adapter rvAdapter = (scheduleRecyclerView != null) ? scheduleRecyclerView.getAdapter() : null;
                        if (rvAdapter instanceof androidx.recyclerview.widget.ListAdapter) {
                            // safe: rvAdapter declared as RecyclerView.Adapter at runtime
                            ((androidx.recyclerview.widget.ListAdapter) rvAdapter).submitList(new ArrayList<>(currentList));
                        } else if (rvAdapter != null) {
                            rvAdapter.notifyDataSetChanged();
                        }
                    }
                } catch (Exception ex) {
                    // Final fallback
                    if (scheduleRecyclerView != null && scheduleRecyclerView.getAdapter() != null) {
                        scheduleRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                }
            });
        });
    }


    // (Chỉ cập nhật method loadScheduleDataForDay; chèn vào class Layout6Activity của bạn)
    // Replace or add these methods inside your Layout6Activity class.

    // --- loadScheduleDataForDay (with added logging of firebaseKeys) ---
    private void loadScheduleDataForDay(int day) {
        if (currentTemplateDetails != null) {
            String dayKey = getDayKeyAsString(day);
            List<TimeSlot> timeSlots = currentTemplateDetails.getWeeklyActivities().get(dayKey);

            if (timeSlots != null) {
                List<ScheduleItem> items = new ArrayList<>();
                String titlePart = (currentTemplateDetails.getTitle() == null)
                        ? "template"
                        : currentTemplateDetails.getTitle().replaceAll("\\s+", "_");
                for (int i = 0; i < timeSlots.size(); i++) {
                    TimeSlot slot = timeSlots.get(i);
                    ScheduleItem si = new ScheduleItem(0, slot.getStartTime(), slot.getEndTime(), slot.getActivityName(), day);
                    // đánh dấu là builtin (không thể xóa)
                    si.setFirebaseKey("builtin_template_" + titlePart + "_" + day + "_" + i);
                    items.add(si);
                }
                currentList.clear();
                currentList.addAll(items);
                if (scheduleAdapter != null) {
                    scheduleAdapter.updateList(currentList);
                }

                // Log keys for debugging
                StringBuilder sb = new StringBuilder();
                for (ScheduleItem it : currentList) {
                    String fk = it.getFirebaseKey() == null ? "<null>" : it.getFirebaseKey();
                    sb.append(fk).append(" | ");
                }
                Log.d(TAG, "loadScheduleDataForDay (template) keys: " + sb.toString());
            } else {
                currentList.clear();
                if (scheduleAdapter != null) {
                    scheduleAdapter.updateList(currentList);
                }
                Toast.makeText(Layout6Activity.this, "Không có lịch trình cho ngày này trong template.", Toast.LENGTH_SHORT).show();
            }
            if (fabAdd != null) fabAdd.setEnabled(true);
            return;
        }

        if (currentList == null) currentList = new ArrayList<>();
        if (fabAdd != null) fabAdd.setEnabled(false);

        String dayNode = "day_" + day;
        schedulesRef.child(dayNode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ScheduleItem> firebaseItems = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ScheduleItem item = ds.getValue(ScheduleItem.class);
                    if (item != null) {
                        if (item.getFirebaseKey() == null || item.getFirebaseKey().isEmpty()) {
                            item.setFirebaseKey(ds.getKey());
                        }
                        firebaseItems.add(item);
                    }
                }

                List<ScheduleItem> itemsToShow = new ArrayList<>();

                // Get default items and mark them builtin if they don't already have a key
                List<ScheduleItem> defaultItems = getDefaultItemsForDay(day);
                for (int i = 0; i < defaultItems.size(); i++) {
                    ScheduleItem d = defaultItems.get(i);
                    if (d.getFirebaseKey() == null || d.getFirebaseKey().isEmpty()) {
                        d.setFirebaseKey("builtin_default_" + day + "_" + i);
                    }
                }
                itemsToShow.addAll(defaultItems);

                itemsToShow.addAll(firebaseItems);

                // overrides from prefs (may include override_... keys) - keep as-is
                itemsToShow.addAll(getOverridesFromPrefs(day));

                itemsToShow.sort((a, b) -> Integer.compare(
                        timeToMinutesOrMax(a.getStartTime()),
                        timeToMinutesOrMax(b.getStartTime())
                ));
                List<ScheduleItem> deduped = new ArrayList<>();
                for (ScheduleItem it : itemsToShow) {
                    boolean exists = false;
                    for (ScheduleItem d : deduped) {
                        if (equalsByTimeAndActivity(d, it)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) deduped.add(it);
                }

                currentList.clear();
                currentList.addAll(deduped);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

                // Debug log: list firebase keys loaded
                StringBuilder sb = new StringBuilder();
                for (ScheduleItem it : currentList) {
                    String fk = it.getFirebaseKey() == null ? "<null>" : it.getFirebaseKey();
                    sb.append(fk).append(" | ");
                }
                Log.d(TAG, "loadScheduleDataForDay keys: " + sb.toString());

                if (currentList.isEmpty()) {
                    Toast.makeText(Layout6Activity.this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
                }
                if (fabAdd != null) fabAdd.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Layout6Activity.this, "Lỗi tải lịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                if (fabAdd != null) fabAdd.setEnabled(true);
            }
        });
    }






    private void saveItem(ScheduleItem item, int position) {
        String key = item.getFirebaseKey();
        if (key != null && key.startsWith("builtin_")) {
            saveOverrideToPrefs(item.getDayOfWeek(), item);
            if (position != -1) currentList.set(position, item);
            Toast.makeText(this, "Sửa đổi đã được lưu vào hồ sơ.", Toast.LENGTH_SHORT).show();
        } else if (key != null && !key.startsWith("override_")) {
            schedulesRef.child("day_" + item.getDayOfWeek()).child(key).setValue(item);
        } else if (key != null && key.startsWith("override_")) {
            saveOverrideToPrefs(item.getDayOfWeek(), item);
        } else {
            saveSingleItemToFirebase(item, position);
        }
        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
    }

    private void showAddDialogMode() {
        ensureScheduleNamedThen(() -> {
            AlertDialog.Builder modeBuilder = new AlertDialog.Builder(this);
            modeBuilder.setTitle("Chọn cách thêm")
                    .setItems(new String[]{"Thêm vào khung giờ cố định", "Thêm hoạt động tùy ý (cho phép trùng)"}, (modeDialog, whichMode) -> {
                        View dialogView = LayoutInflater.from(this).inflate(R.layout.edit_schedule1, null);
                        EditText etStart = dialogView.findViewById(R.id.etStartTime);
                        EditText etEnd = dialogView.findViewById(R.id.etEndTime);
                        EditText etAct = dialogView.findViewById(R.id.etActivity);
                        etStart.setHint("06:00");
                        etEnd.setHint("07:00");

                        if (whichMode == 0) {
                            List<ScheduleItem> predefined = getDefaultItemsForDay(selectedDay);
                            List<String> slotLabels = new ArrayList<>();
                            for (ScheduleItem s : predefined) {
                                String label = s.getStartTime() + " - " + s.getEndTime() + " : " + s.getActivity();
                                slotLabels.add(label);
                            }
                            CharSequence[] choices = slotLabels.toArray(new CharSequence[0]);

                            new AlertDialog.Builder(this)
                                    .setTitle("Chọn khung giờ")
                                    .setItems(choices, (slotDialog, slotIndex) -> {
                                        ScheduleItem chosenSlot = predefined.get(slotIndex);
                                        boolean occupied = false;
                                        if (currentList != null) {
                                            for (ScheduleItem existing : currentList) {
                                                String es = existing.getStartTime() == null ? "" : existing.getStartTime();
                                                String ee = existing.getEndTime() == null ? "" : existing.getEndTime();
                                                if (es.equals(chosenSlot.getStartTime()) && ee.equals(chosenSlot.getEndTime())) {
                                                    occupied = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (occupied) {
                                            Toast.makeText(this, "Khung giờ này đã có hoạt động, không thể thêm.", Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        etStart.setText(chosenSlot.getStartTime());
                                        etEnd.setText(chosenSlot.getEndTime());
                                        etAct.setText(chosenSlot.getActivity());

                                        new AlertDialog.Builder(this)
                                                .setTitle("Xác nhận khung giờ")
                                                .setView(dialogView)
                                                .setPositiveButton("Thêm", (confirmDialog, confirmWhich) -> {
                                                    String newStart = etStart.getText().toString().trim();
                                                    String newEnd = etEnd.getText().toString().trim();
                                                    String newAct = etAct.getText().toString().trim();
                                                    if (newStart.isEmpty() || newEnd.isEmpty()) {
                                                        Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    if (timeToMinutes(newStart) < 0 || timeToMinutes(newEnd) < 0) {
                                                        Toast.makeText(this, "Định dạng thời gian không hợp lệ (HH:mm)", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    if (timeToMinutes(newEnd) <= timeToMinutes(newStart)) {
                                                        Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    boolean occupiedNow = false;
                                                    if (currentList != null) {
                                                        for (ScheduleItem existing : currentList) {
                                                            String es = existing.getStartTime() == null ? "" : existing.getStartTime();
                                                            String ee = existing.getEndTime() == null ? "" : existing.getEndTime();
                                                            if (es.equals(newStart) && ee.equals(newEnd)) {
                                                                occupiedNow = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (occupiedNow) {
                                                        Toast.makeText(this, "Khung giờ này đã có hoạt động, không thể thêm.", Toast.LENGTH_LONG).show();
                                                        return;
                                                    }
                                                    ScheduleItem newItem = new ScheduleItem(0, newStart, newEnd, newAct, selectedDay);
                                                    // mark as user/local so delete UI & logic allow removal until firebase assigns real key
                                                    newItem.setFirebaseKey("local_" + System.currentTimeMillis());

                                                    if (currentList == null) currentList = new ArrayList<>();
                                                    currentList.add(newItem);
                                                    currentList.sort((a, b) -> Integer.compare(
                                                            timeToMinutesOrMax(a.getStartTime()),
                                                            timeToMinutesOrMax(b.getStartTime())
                                                    ));
                                                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                                                    // save & persist
                                                    saveSingleItemToFirebase(newItem);
                                                    pushSingleActivityHistory(newItem, null);
                                                    addToCurrentTemplateAndSave(newItem);
                                                    Toast.makeText(this, "Đã thêm vào khung giờ cố định (lưu vào lịch '" + currentScheduleName + "')", Toast.LENGTH_SHORT).show();
                                                })
                                                .setNegativeButton("Hủy", null)
                                                .show();
                                    })
                                    .show();
                        } else {
                            etStart.setText("");
                            etEnd.setText("");
                            etAct.setText("");
                            new AlertDialog.Builder(this)
                                    .setTitle("Thêm hoạt động tùy ý (cho phép trùng)")
                                    .setView(dialogView)
                                    .setPositiveButton("Thêm", (freeDialog, freeWhich) -> {
                                        String newStart = etStart.getText().toString().trim();
                                        String newEnd = etEnd.getText().toString().trim();
                                        String newAct = etAct.getText().toString().trim();
                                        if (newStart.isEmpty() || newEnd.isEmpty()) {
                                            Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        if (timeToMinutes(newStart) < 0 || timeToMinutes(newEnd) < 0) {
                                            Toast.makeText(this, "Định dạng thời gian không hợp lệ (HH:mm)", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        if (timeToMinutes(newEnd) <= timeToMinutes(newStart)) {
                                            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        ScheduleItem newItem = new ScheduleItem(0, newStart, newEnd, newAct, selectedDay);
                                        // assign temporary local key so delete option appears and delete logic can handle it
                                        newItem.setFirebaseKey("local_" + System.currentTimeMillis());

                                        if (currentList == null) currentList = new ArrayList<>();
                                        currentList.add(newItem);
                                        currentList.sort((a, b) -> {
                                            String as = a.getStartTime() == null ? "" : a.getStartTime();
                                            String bs = b.getStartTime() == null ? "" : b.getStartTime();
                                            return as.compareTo(bs);
                                        });
                                        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                                        // save under per-user schedule path (isolation guaranteed)
                                        saveSingleItemToFirebase(newItem);
                                        pushSingleActivityHistory(newItem, null);
                                        addToCurrentTemplateAndSave(newItem);
                                        Toast.makeText(this, "Đã thêm hoạt động (cho phép trùng) — lưu vào lịch '" + currentScheduleName + "'", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }




    private void showApplyDialog() {
        String[] options = new String[]{
                "Hiển thị ở màn hình chính",
                "Chỉ lưu vào lịch sử",
                "Xóa",
                "Hủy"
        };

        new AlertDialog.Builder(this)
                .setTitle("Áp dụng lịch này")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Apply to home display
                            if (currentList != null) {
                                for (ScheduleItem it : currentList) {
                                    String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
                                    if (fk.isEmpty() || fk.startsWith("local_")) {
                                        saveSingleItemToFirebase(it, -1);
                                    }
                                }
                            }
                            saveScheduleToFirebase();
                            saveScheduleToProfileHistory(selectedDay);
                            if (currentTemplateDetails != null) {
                                ScheduleData.saveCustomTemplate(this, currentTemplateDetails);
                            }
                            saveAllWeekScheduleToHomeDisplay();
                            Toast.makeText(this, "Đã lưu toàn bộ lịch tuần và áp dụng", Toast.LENGTH_SHORT).show();
                            finish();
                            break;

                        case 1: // Save to history only
                            if (currentList != null) {
                                for (ScheduleItem it : currentList) {
                                    String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
                                    if (fk.isEmpty() || fk.startsWith("local_")) {
                                        saveSingleItemToFirebase(it, -1);
                                    }
                                }
                            }
                            saveScheduleToFirebase();
                            saveScheduleToProfileHistory(selectedDay);
                            Toast.makeText(this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                            break;

                        case 2: // Delete day schedule
                            new AlertDialog.Builder(this)
                                    .setTitle("Xóa lịch")
                                    .setMessage("Bạn chắc chưa ?")
                                    .setPositiveButton("Xóa", (confirm, w) -> {
                                        String dayNode = "day_" + selectedDay;
                                        DatabaseReference targetRef;
                                        if (currentScheduleName != null && !currentScheduleName.trim().isEmpty()) {
                                            targetRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
                                        } else {
                                            targetRef = schedulesRef.child(dayNode);
                                        }

                                        targetRef.removeValue()
                                                .addOnSuccessListener(aVoid -> {
                                                    // remove overrides and local items for this day from prefs
                                                    try {
                                                        prefs.edit()
                                                                .remove("overrides_day_" + selectedDay)
                                                                .remove(localKeyForDay(selectedDay))
                                                                .apply();
                                                    } catch (Exception ex) {
                                                        Log.w(TAG, "Failed clearing local prefs for day", ex);
                                                    }

                                                    if (currentList != null) {
                                                        currentList.clear();
                                                        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                                                    }
                                                    Toast.makeText(Layout6Activity.this, "Đã xóa toàn bộ lịch ngày này.", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w(TAG, "Failed to remove day node: " + e.getMessage(), e);
                                                    Toast.makeText(Layout6Activity.this, "Không thể xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                });
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                            break;

                        case 3: // Cancel
                        default:
                            // nothing
                            break;
                    }
                })
                .show();
    }
    // --- CẬP NHẬT showEditDialog: chỉ hiện "Xóa" nếu item không phải builtin ---
    // Updated showEditDialog: only show "Xóa" button when item is NOT builtin (áp cứng)
    private void showEditDialog(int position, ScheduleItem item) {
        if (item == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.edit_schedule1, null);
        EditText etStart = view.findViewById(R.id.etStartTime);
        EditText etEnd = view.findViewById(R.id.etEndTime);
        EditText etAct = view.findViewById(R.id.etActivity);

        etStart.setText(item.getStartTime());
        etEnd.setText(item.getEndTime());
        etAct.setText(item.getActivity());

        boolean isBuiltin = false;
        try {
            isBuiltin = item.isBuiltin(); // uses ScheduleItem flag
        } catch (Throwable ignored) {
            // fallback: consider ScheduleData.isBuiltin(...)
            isBuiltin = ScheduleData.isBuiltin(item);
        }

        builder.setView(view)
                .setTitle("Chỉnh sửa lịch trình")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newStart = etStart.getText().toString().trim();
                    String newEnd = etEnd.getText().toString().trim();
                    String newAct = etAct.getText().toString().trim();

                    if (newStart.isEmpty() || newEnd.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isOverlapping(newStart, newEnd, currentList, item)) {
                        Toast.makeText(this, "Thời gian mới trùng với mục khác.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    item.setStartTime(newStart);
                    item.setEndTime(newEnd);
                    item.setActivity(newAct);

                    saveItem(item, position);
                })
                .setNegativeButton("Hủy", null);

        // Only show delete when NOT builtin
        if (!isBuiltin) {
            builder.setNeutralButton("Xóa", (dialog, which) -> confirmAndDeleteItem(position, item));
        }

        builder.show();
    }



    private void showConfirmDeleteDialog(int position, ScheduleItem item) {
        if (item == null) return;

        // If builtin, block early
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        if (fk.startsWith("builtin_")) {
            Toast.makeText(this, "Mục này là mặc định và không thể xóa.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn chắc chưa , cho suy nghĩ lại nha ?")
                .setPositiveButton("Xóa", (conf, which) -> {
                    // perform deletion
                    performDeleteItem(position, item);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Thực hiện xóa thật sự: xử lý các trường hợp override_*, firebase key, local (rỗng/local_).
     * Cập nhật currentList và adapter tương ứng.
     */
    private void performDeleteItem(int position, ScheduleItem item) {
        if (item == null) return;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        int day = item.getDayOfWeek();

        // 1) builtin protection (extra safety)
        if (fk.startsWith("builtin_")) {
            Toast.makeText(this, "Không thể xóa mục mặc định.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2) override stored in prefs (key like override_...) -> remove from overrides in prefs
        if (fk.startsWith("override_")) {
            try {
                List<ScheduleItem> overrides = ScheduleData.getOverridesFromPrefs(this, day);
                boolean removed = false;
                for (int i = overrides.size() - 1; i >= 0; i--) {
                    ScheduleItem s = overrides.get(i);
                    String k = s.getFirebaseKey() == null ? "" : s.getFirebaseKey();
                    // match by firebaseKey if present, else by time+activity
                    if ((!k.isEmpty() && k.equals(fk)) ||
                            (equalsByTimeAndActivity(s, item))) {
                        overrides.remove(i);
                        removed = true;
                    }
                }
                ScheduleData.saveOverridesToPrefs(this, day, overrides);
                if (removed) {
                    // remove from currentList
                    removeItemFromCurrentList(position, item);
                    Toast.makeText(this, "Đã xóa override.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không tìm thấy override để xóa.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                Log.w(TAG, "Failed removing override from prefs", ex);
                Toast.makeText(this, "Xóa thất bại.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 3) local-only item (no firebase key or local_...) -> remove locally
        if (fk.isEmpty() || fk.startsWith("local_")) {
            // remove from any local storage if you keep local_items_day_<day>
            try {
                // remove from currentList
                removeItemFromCurrentList(position, item);

                // update local saved items if you use ScheduleData.saveLocalUserItems
                List<ScheduleItem> local = ScheduleData.getLocalUserItems(this, day);
                boolean removed = false;
                for (int i = local.size() - 1; i >= 0; i--) {
                    if (equalsByTimeAndActivity(local.get(i), item)) {
                        local.remove(i);
                        removed = true;
                    }
                }
                if (removed) ScheduleData.saveLocalUserItems(this, day, local);

                Toast.makeText(this, "Đã xóa.", Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Log.w(TAG, "Failed removing local item", ex);
                Toast.makeText(this, "Xóa thất bại.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 4) firebase-backed item with a real key -> remove from Firebase
        DatabaseReference targetDayRef;
        if (currentScheduleName != null && !currentScheduleName.trim().isEmpty()) {
            targetDayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child("day_" + day);
        } else {
            targetDayRef = schedulesRef.child("day_" + day);
        }

        targetDayRef.child(fk).removeValue()
                .addOnSuccessListener(aVoid -> {
                    removeItemFromCurrentList(position, item);
                    Toast.makeText(this, "Đã xóa hoạt động.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to remove item from firebase: " + e.getMessage(), e);
                    Toast.makeText(this, "Không thể xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // helper to remove item from currentList safely and update adapter
    private void removeItemFromCurrentList(int position, ScheduleItem item) {
        try {
            if (currentList == null) return;
            // prefer remove by position if valid and matches item
            if (position >= 0 && position < currentList.size()) {
                ScheduleItem atPos = currentList.get(position);
                if (equalsByTimeAndActivity(atPos, item) || Objects.equals(atPos.getFirebaseKey(), item.getFirebaseKey())) {
                    currentList.remove(position);
                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                    return;
                }
            }
            // fallback: find by matching key or time+activity
            for (int i = currentList.size() - 1; i >= 0; i--) {
                ScheduleItem s = currentList.get(i);
                String k = s.getFirebaseKey() == null ? "" : s.getFirebaseKey();
                if ((!k.isEmpty() && k.equals(item.getFirebaseKey())) || equalsByTimeAndActivity(s, item)) {
                    currentList.remove(i);
                }
            }
            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
        } catch (Exception ex) {
            Log.w(TAG, "removeItemFromCurrentList error", ex);
        }
    }
   // java
// Updated confirmAndDeleteItem: use item.isBuiltin() instead of checking firebaseKey prefix
   private void confirmAndDeleteItem(int position, ScheduleItem item) {
       if (item == null) return;

       String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
       // changed: check the model flag instead of relying on string prefix
       boolean isBuiltin = item.isBuiltin();

       if (!isBuiltin && !fk.isEmpty()) {
           new AlertDialog.Builder(this)
                   .setTitle("Xác nhận xóa")
                   .setMessage("Bạn chắc chưa , cho suy nghĩ lại đó ?")
                   .setPositiveButton("Xóa", (d, w) -> {
                       deleteFromFirebase(fk, () -> {
                           // update local list
                           int removedIndex = -1;
                           if (position >= 0 && position < currentList.size()) {
                               currentList.remove(position);
                               removedIndex = position;
                           } else {
                               int idx = currentList.indexOf(item);
                               if (idx != -1) {
                                   currentList.remove(idx);
                                   removedIndex = idx;
                               }
                           }

                           // Refresh adapter safely
                           if (scheduleAdapter != null) {
                               try {
                                   scheduleAdapter.updateList(currentList);
                               } catch (Exception ex) {
                                   if (removedIndex != -1) scheduleAdapter.notifyItemRemoved(removedIndex);
                                   else scheduleAdapter.notifyDataSetChanged();
                               }
                           } else if (scheduleRecyclerView != null && scheduleRecyclerView.getAdapter() != null) {
                               RecyclerView.Adapter a = scheduleRecyclerView.getAdapter();
                               if (removedIndex != -1) a.notifyItemRemoved(removedIndex);
                               else a.notifyDataSetChanged();
                           }

                           Toast.makeText(this, "Đã xóa.", Toast.LENGTH_SHORT).show();
                       });
                   })
                   .setNegativeButton("Hủy", null)
                   .show();
       } else if (!isBuiltin) {
           // Local item (not builtin, but no firebase key) — just remove locally
           new AlertDialog.Builder(this)
                   .setTitle("Xác nhận xóa")
                   .setMessage("Mục này chỉ tồn tại cục bộ. Bạn có muốn xóa không?")
                   .setPositiveButton("Xóa", (d, w) -> {
                       int removedIndex = -1;
                       if (position >= 0 && position < currentList.size()) {
                           currentList.remove(position);
                           removedIndex = position;
                       } else {
                           int idx = currentList.indexOf(item);
                           if (idx != -1) {
                               currentList.remove(idx);
                               removedIndex = idx;
                           }
                       }

                       if (scheduleAdapter != null) {
                           try {
                               scheduleAdapter.updateList(currentList);
                           } catch (Exception ex) {
                               if (removedIndex != -1) scheduleAdapter.notifyItemRemoved(removedIndex);
                               else scheduleAdapter.notifyDataSetChanged();
                           }
                       } else if (scheduleRecyclerView != null && scheduleRecyclerView.getAdapter() != null) {
                           RecyclerView.Adapter a = scheduleRecyclerView.getAdapter();
                           if (removedIndex != -1) a.notifyItemRemoved(removedIndex);
                           else a.notifyDataSetChanged();
                       }

                       Toast.makeText(this, "Đã xóa (cục bộ).", Toast.LENGTH_SHORT).show();
                   })
                   .setNegativeButton("Hủy", null)
                   .show();
       } else {
           // Builtin: không xóa trên backend mặc định — cho phép ẩn cục bộ / tạo override
           new AlertDialog.Builder(this)
                   .setTitle("Mục mặc định hệ thống")
                   .setMessage("Mục này là mặc định của hệ thống và không thể xóa trên server. Bạn muốn ẩn mục này trên thiết bị (xóa cục bộ) hoặc lưu override?")
                   .setPositiveButton("Xóa cục bộ", (d, w) -> {
                       int removedIndex = -1;
                       if (position >= 0 && position < currentList.size()) {
                           currentList.remove(position);
                           removedIndex = position;
                       } else {
                           int idx = currentList.indexOf(item);
                           if (idx != -1) {
                               currentList.remove(idx);
                               removedIndex = idx;
                           }
                       }

                       if (scheduleAdapter != null) {
                           try {
                               scheduleAdapter.updateList(currentList);
                           } catch (Exception ex) {
                               if (removedIndex != -1) scheduleAdapter.notifyItemRemoved(removedIndex);
                               else scheduleAdapter.notifyDataSetChanged();
                           }
                       } else if (scheduleRecyclerView != null && scheduleRecyclerView.getAdapter() != null) {
                           RecyclerView.Adapter a = scheduleRecyclerView.getAdapter();
                           if (removedIndex != -1) a.notifyItemRemoved(removedIndex);
                           else a.notifyDataSetChanged();
                       }

                       // TODO: nếu muốn ẩn vĩnh viễn: gọi hàm lưu override (SharedPreferences / Firebase overrides)
                       // ví dụ: saveHiddenBuiltinToFirebase(item.getFirebaseKey(), null);

                       Toast.makeText(this, "Đã ẩn mục (cục bộ).", Toast.LENGTH_SHORT).show();
                   })
                   .setNeutralButton("Tạo override", (d, w) -> {
                       // TODO: implement override flow: lưu key vào node overrides hoặc local DB
                       String builtinKey = item.getFirebaseKey();
                       if (builtinKey == null || builtinKey.isEmpty()) {
                           // nếu không có firebaseKey, bạn có thể tạo một id dựa trên nội dung
                           builtinKey = "builtin_" + System.currentTimeMillis();
                       }
                       saveHiddenBuiltinToFirebase(builtinKey, () -> {
                           // remove locally as well
                           int removedIndex = -1;
                           if (position >= 0 && position < currentList.size()) {
                               currentList.remove(position);
                               removedIndex = position;
                           } else {
                               int idx = currentList.indexOf(item);
                               if (idx != -1) {
                                   currentList.remove(idx);
                                   removedIndex = idx;
                               }
                           }

                           if (scheduleAdapter != null) {
                               try {
                                   scheduleAdapter.updateList(currentList);
                               } catch (Exception ex) {
                                   if (removedIndex != -1) scheduleAdapter.notifyItemRemoved(removedIndex);
                                   else scheduleAdapter.notifyDataSetChanged();
                               }
                           } else if (scheduleRecyclerView != null && scheduleRecyclerView.getAdapter() != null) {
                               RecyclerView.Adapter a = scheduleRecyclerView.getAdapter();
                               if (removedIndex != -1) a.notifyItemRemoved(removedIndex);
                               else a.notifyDataSetChanged();
                           }

                           Toast.makeText(this, "Đã lưu override và ẩn mục.", Toast.LENGTH_SHORT).show();
                       });
                   })
                   .setNegativeButton("Hủy", null)
                   .show();
       }
   }
   // java
   private void saveHiddenBuiltinToFirebase(String builtinKey, Runnable callback) {
       if (builtinKey == null || builtinKey.trim().isEmpty()) {
           if (callback != null) callback.run();
           return;
       }

       // Save to local prefs set `hidden_builtins`
       try {
           java.util.Set<String> existing = prefs.getStringSet("hidden_builtins", null);
           java.util.HashSet<String> newSet = new java.util.HashSet<>(
                   existing == null ? java.util.Collections.emptySet() : existing
           );
           if (!newSet.contains(builtinKey)) {
               newSet.add(builtinKey);
               prefs.edit().putStringSet("hidden_builtins", newSet).apply();
           }
       } catch (Exception ex) {
           Log.w(TAG, "saveHiddenBuiltinToFirebase: prefs save failed", ex);
       }

       // Ensure userId and rootRef exist
       if (userId == null || userId.isEmpty()) {
           userId = FirebaseAuth.getInstance().getUid();
       }
       if (rootRef == null) {
           rootRef = FirebaseDatabase.getInstance().getReference();
       }

       // Write override marker to Firebase: user_overrides/{userId}/hidden_builtins/{builtinKey} = timestamp
       DatabaseReference overrideRef = rootRef.child("user_overrides").child(userId).child("hidden_builtins").child(builtinKey);
       overrideRef.setValue(ServerValue.TIMESTAMP)
               .addOnSuccessListener(aVoid -> {
                   Log.d(TAG, "saveHiddenBuiltinToFirebase: saved override " + builtinKey);
                   if (callback != null) callback.run();
               })
               .addOnFailureListener(e -> {
                   Log.w(TAG, "saveHiddenBuiltinToFirebase: failed saving override " + builtinKey, e);
                   // still call callback so caller can update UI/fall back
                   if (callback != null) callback.run();
               });
   }

    private void deleteFromFirebase(String firebaseKey, Runnable callback) {
        if (firebaseKey == null || firebaseKey.isEmpty()) {
            if (callback != null) callback.run();
            return;
        }

        // TODO: thay path "schedules" bằng path thực tế của bạn
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("schedules").child(firebaseKey);
        ref.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // vẫn gọi callback nếu bạn muốn cập nhật UI bất chấp lỗi
                });
    }

    /**
     * Thực hiện xóa (thực tế): cập nhật UI, xóa khỏi template nếu cần, xóa overrides/prefs/local/firebase,
     * load lại ngày nếu template đã thay đổi, và hiển thị thông báo cho người dùng.
     */
    private void performDeletion(int position, ScheduleItem item) {
        if (item == null) return;

        // Optimistic UI removal for snappy UX
        boolean removedFromList = false;
        if (currentList != null) {
            if (position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                removedFromList = true;
            } else {
                for (int i = currentList.size() - 1; i >= 0; i--) {
                    ScheduleItem it = currentList.get(i);
                    if (equalsByTimeAndActivity(it, item)) {
                        currentList.remove(i);
                        removedFromList = true;
                    }
                }
            }
            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
        }

        // 1) Remove from persisted template (if applicable)
        boolean removedFromTemplate = false;
        try {
            if (currentTemplateDetails != null && currentTemplateDetails.getTitle() != null) {
                String title = currentTemplateDetails.getTitle();
                String dayKey = getDayKeyAsString(item.getDayOfWeek());
                TimeSlot slot = new TimeSlot(item.getStartTime(), item.getEndTime(), item.getActivity());
                DetailedSchedule updated = ScheduleData.removeSlotFromTemplateAndGet(this, title, dayKey, slot);
                if (updated != null) {
                    currentTemplateDetails = updated;
                    removedFromTemplate = true;
                    Log.d(TAG, "Removed slot from custom template: " + title + " / " + dayKey);
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Error removing slot from template", ex);
        }

        // 2) Remove from overrides prefs / local storage / Firebase
        try {
            String fk2 = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
            if (fk2.isEmpty() || fk2.startsWith("override_")) {
                // override or local-only -> remove from prefs/local
                saveOverrideRemove(item);
                removeLocalUserItem(item);
            } else {
                // remove from Firebase (use per-user schedule if currentScheduleName set)
                String dayNode = "day_" + item.getDayOfWeek();
                DatabaseReference targetRef;
                if (currentScheduleName != null && !currentScheduleName.trim().isEmpty()) {
                    targetRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode).child(fk2);
                } else {
                    targetRef = schedulesRef.child(dayNode).child(fk2);
                }

                targetRef.removeValue()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Removed item from Firebase key=" + fk2))
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to remove item from Firebase: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(Layout6Activity.this, "Không thể xóa trên server: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        });

                // also remove any local cached copy
                removeLocalUserItem(item);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Error removing from Firebase/prefs", ex);
        }

        // 3) If removed from template, reload canonical template day to ensure UI consistent
        if (removedFromTemplate) {
            loadScheduleDataForDay(selectedDay);
        }

        // Final user feedback
        if (removedFromList) {
            Toast.makeText(this, "Đã xóa hoạt động.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Đã cố gắng xóa hoạt động.", Toast.LENGTH_SHORT).show();
        }
    }




    private void addOrUpdateLocalUserItem(ScheduleItem item) {
        if (item == null) return;
        int day = item.getDayOfWeek();
        List<ScheduleItem> existing = getLocalUserItems(day);
        boolean updated = false;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        for (int i = 0; i < existing.size(); i++) {
            ScheduleItem ex = existing.get(i);
            String exFk = ex.getFirebaseKey() == null ? "" : ex.getFirebaseKey();
            if (!exFk.isEmpty() && exFk.equals(fk)) {
                existing.set(i, item);
                updated = true;
                break;
            }
        }
        if (!updated) existing.add(item);
        saveLocalUserItems(day, existing);
    }
    private List<ScheduleItem> getLocalUserItems(int day) {
        String key = localKeyForDay(day);
        String json = prefs.getString(key, null);
        List<ScheduleItem> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String fk = o.optString("firebaseKey", null);
                String s = o.optString("startTime", "");
                String e = o.optString("endTime", "");
                String a = o.optString("activity", "");
                int d = o.optInt("day", day);
                ScheduleItem it = new ScheduleItem(0, s, e, a, d);
                if (fk != null && !fk.isEmpty()) it.setFirebaseKey(fk);
                list.add(it);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "parse local items error", ex);
        }
        return list;
    }
    private void saveLocalUserItems(int day, List<ScheduleItem> items) {
        try {
            String key = localKeyForDay(day);
            JSONArray arr = new JSONArray();
            if (items != null) {
                for (ScheduleItem it : items) {
                    JSONObject o = new JSONObject();
                    o.put("firebaseKey", it.getFirebaseKey() == null ? "" : it.getFirebaseKey());
                    o.put("startTime", it.getStartTime() == null ? "" : it.getStartTime());
                    o.put("endTime", it.getEndTime() == null ? "" : it.getEndTime());
                    o.put("activity", it.getActivity() == null ? "" : it.getActivity());
                    o.put("day", it.getDayOfWeek());
                    arr.put(o);
                }
            }
            prefs.edit().putString(key, arr.toString()).apply();
            Log.d(TAG, "Saved " + (items == null ? 0 : items.size()) + " local items to key=" + key);
        } catch (JSONException ex) {
            Log.e(TAG, "saveLocalUserItems json error", ex);
        } catch (Exception ex) {
            Log.w(TAG, "saveLocalUserItems failed", ex);
        }
    }






    private void addToCurrentTemplateAndSave(ScheduleItem newItem) {
        if (newItem == null) return;

        // If there is no template selected, we still save the single item into Firebase (already done elsewhere)
        if (currentTemplateDetails == null) {
            Log.w(TAG, "No current template selected - skipping template persistence");
            return;
        }

        String title = currentTemplateDetails.getTitle();
        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "Current template has no title - cannot persist");
            return;
        }

        String dayKey = getDayKeyAsString(newItem.getDayOfWeek());
        TimeSlot slot = new TimeSlot(
                newItem.getStartTime() == null ? "" : newItem.getStartTime(),
                newItem.getEndTime() == null ? "" : newItem.getEndTime(),
                newItem.getActivity() == null ? "" : newItem.getActivity()
        );

        // Optimistic local persistence (for offline/quick UI)
        try {
            addOrUpdateLocalUserItem(newItem);
        } catch (Exception ex) {
            Log.w(TAG, "addOrUpdateLocalUserItem failed", ex);
        }

        // Use ScheduleData's API to add and get the saved template back
        DetailedSchedule updated = ScheduleData.addSlotToTemplateAndGet(this, title, dayKey, slot);
        if (updated != null) {
            currentTemplateDetails = updated;
            Log.d(TAG, "addToCurrentTemplateAndSave: persisted slot and reloaded template for title=" + title);
            // refresh UI for current day immediately
            loadScheduleDataForDay(selectedDay);
            Toast.makeText(this, "Đã lưu vào mẫu: " + slot.getActivity(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Fallback behavior: try to merge and save directly on currentTemplateDetails
        Log.w(TAG, "addToCurrentTemplateAndSave: addSlotToTemplateAndGet returned null - attempting fallback save");
        Map<String, List<TimeSlot>> weekly;
        try {
            weekly = currentTemplateDetails.getWeeklyActivities();
        } catch (Exception ex) {
            weekly = null;
        }
        if (weekly == null) weekly = new HashMap<>();
        Map<String, List<TimeSlot>> copy = new HashMap<>();
        for (Map.Entry<String, List<TimeSlot>> e : weekly.entrySet()) {
            copy.put(e.getKey(), e.getValue() == null ? new ArrayList<>() : new ArrayList<>(e.getValue()));
        }
        List<TimeSlot> list = copy.get(dayKey);
        if (list == null) {
            list = new ArrayList<>();
            copy.put(dayKey, list);
        }
        boolean exists = false;
        for (TimeSlot ts : list) {
            String s = ts.getStartTime() == null ? "" : ts.getStartTime();
            String e = ts.getEndTime() == null ? "" : ts.getEndTime();
            String a = ts.getActivityName() == null ? "" : ts.getActivityName();
            if (s.equals(slot.getStartTime()) && e.equals(slot.getEndTime()) && a.equals(slot.getActivityName())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            list.add(slot);
            try {
                DetailedSchedule ds = new DetailedSchedule(
                        currentTemplateDetails.getTitle(),
                        currentTemplateDetails.getDescription(),
                        currentTemplateDetails.getTags() == null ? new ArrayList<>() : new ArrayList<>(currentTemplateDetails.getTags()),
                        copy
                );
                ScheduleData.saveCustomTemplate(this, ds);
                currentTemplateDetails = ds;
                loadScheduleDataForDay(selectedDay);
                Toast.makeText(this, "Đã lưu vào mẫu (fallback): " + slot.getActivity(), Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                try {
                    currentTemplateDetails.setWeeklyActivities(copy);
                    ScheduleData.saveCustomTemplate(this, currentTemplateDetails);
                    loadScheduleDataForDay(selectedDay);
                    Toast.makeText(this, "Đã lưu vào mẫu (fallback setter): " + slot.getActivity(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.w(TAG, "Fallback save failed", e);
                    Toast.makeText(this, "Không thể lưu mục vào mẫu", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.d(TAG, "Slot already exists in template - skip adding");
        }
    }



    private String currentScheduleName = null;


    /**
     * Ensures we have a current schedule name, prompting the user if needed.
     * Calls onReady.run() once a non-empty name is available.
     */
    private void ensureScheduleNamedThen(Runnable onNamed) {
        currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                ? tvTitleHeader.getText().toString().trim()
                : null;
        if (currentScheduleName != null && !currentScheduleName.isEmpty()) {
            if (onNamed != null) onNamed.run();
            return;
        }

        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setHint("Nhập tên lịch (ví dụ: Lịch cho sinh viên)");

        new AlertDialog.Builder(this)
                .setTitle("Chưa có tên lịch")
                .setMessage("Bạn cần đặt tên cho lịch để các thay đổi chỉ lưu vào lịch đó. Nhập tên lịch hoặc hủy.")
                .setView(et)
                .setPositiveButton("Đặt tên & tiếp tục", (d, w) -> {
                    String name = et.getText() == null ? null : et.getText().toString().trim();
                    if (name == null || name.isEmpty()) {
                        Toast.makeText(this, "Bạn phải nhập tên lịch để tiếp tục", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tvTitleHeader != null) tvTitleHeader.setText(name);
                    currentScheduleName = name;
                    // persist active schedule preference
                    SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
                    profilePrefs.edit().putString(PREF_ACTIVE_SCHEDULE, currentScheduleName).apply();

                    // enable FAB now that schedule name exists
                    updateFabState();

                    if (onNamed != null) onNamed.run();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void updateFabState() {
        currentScheduleName = (tvTitleHeader != null && tvTitleHeader.getText() != null)
                ? tvTitleHeader.getText().toString().trim()
                : null;
        if (fabAdd == null) return;
        fabAdd.setEnabled(currentScheduleName != null && !currentScheduleName.isEmpty());
    }




    /**
     * Convenience overload used by UI code that didn't pass a position.
     */
    // language: java
    private String localKeyForDay(int day) {
        // Consistent local key for storing per-day local items in prefs
        return "local_items_day_" + day;
    }
//    private void saveSingleItemToFirebase(ScheduleItem item) {
//        saveSingleItemToFirebase(item, -1);
//    }
//    private void saveSingleItemToFirebase(ScheduleItem item, int position) {
//        if (item == null) return;
//
//        String dayNode = "day_" + item.getDayOfWeek();
//        DatabaseReference targetDayRef;
//        if (currentScheduleName != null && !currentScheduleName.trim().isEmpty()) {
//            targetDayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
//        } else {
//            targetDayRef = schedulesRef.child(dayNode);
//        }
//
//        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
//
//        // If empty or marked local_ (temporary), push a new node
//        if (fk.isEmpty() || fk.startsWith("local_")) {
//            DatabaseReference pushRef = targetDayRef.push();
//            // Optionally set a temporary key to reflect presence in UI (already maybe "")
//            String tempKey = pushRef.getKey();
//            // Set the value on Firebase
//            pushRef.setValue(item)
//                    .addOnSuccessListener(aVoid -> {
//                        // Update item with real key and update UI list
//                        try {
//                            item.setFirebaseKey(pushRef.getKey());
//                            Log.d(TAG, "saveSingleItemToFirebase: pushed item key=" + pushRef.getKey());
//
//                            // Update currentList: prefer position if given
//                            if (position >= 0 && position < currentList.size()) {
//                                currentList.set(position, item);
//                            } else {
//                                // find by matching time+activity and update firebaseKey if match
//                                boolean updated = false;
//                                for (int i = 0; i < currentList.size(); i++) {
//                                    ScheduleItem it = currentList.get(i);
//                                    if (equalsByTimeAndActivity(it, item)) {
//                                        currentList.set(i, item);
//                                        updated = true;
//                                        break;
//                                    }
//                                }
//                                if (!updated) {
//                                    // if not found, append
//                                    currentList.add(item);
//                                }
//                            }
//
//                            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
//                        } catch (Exception ex) {
//                            Log.w(TAG, "saveSingleItemToFirebase callback error", ex);
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        Log.w(TAG, "Failed to push item to Firebase: " + e.getMessage(), e);
//                        runOnUiThread(() -> Toast.makeText(Layout6Activity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
//                    });
//        } else {
//            // Existing key: update the node
//            DatabaseReference targetRef = targetDayRef.child(fk);
//            targetRef.setValue(item)
//                    .addOnSuccessListener(aVoid -> {
//                        Log.d(TAG, "saveSingleItemToFirebase: updated item key=" + fk);
//                        try {
//                            // update the UI list
//                            if (position >= 0 && position < currentList.size()) {
//                                currentList.set(position, item);
//                            } else {
//                                // replace by matching key, or by time+activity as fallback
//                                boolean replaced = false;
//                                for (int i = 0; i < currentList.size(); i++) {
//                                    ScheduleItem it = currentList.get(i);
//                                    String itFk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
//                                    if (!itFk.isEmpty() && itFk.equals(fk)) {
//                                        currentList.set(i, item);
//                                        replaced = true;
//                                        break;
//                                    }
//                                }
//                                if (!replaced) {
//                                    for (int i = 0; i < currentList.size(); i++) {
//                                        if (equalsByTimeAndActivity(currentList.get(i), item)) {
//                                            currentList.set(i, item);
//                                            replaced = true;
//                                            break;
//                                        }
//                                    }
//                                }
//                                if (!replaced) {
//                                    currentList.add(item);
//                                }
//                            }
//                            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
//                        } catch (Exception ex) {
//                            Log.w(TAG, "saveSingleItemToFirebase update callback error", ex);
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        Log.w(TAG, "Failed to update item on Firebase: " + e.getMessage(), e);
//                        runOnUiThread(() -> Toast.makeText(Layout6Activity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
//                    });
//        }
//    }
private void saveSingleItemToFirebase(ScheduleItem item) {
    saveSingleItemToFirebase(item, -1);
}

    private void saveSingleItemToFirebase(ScheduleItem item, int position) {
        if (item == null) return;

        String dayNode = "day_" + item.getDayOfWeek();
        DatabaseReference targetDayRef;
        if (currentScheduleName != null && !currentScheduleName.trim().isEmpty()) {
            targetDayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
        } else {
            targetDayRef = schedulesRef.child(dayNode);
        }

        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();

        // If empty or local_ use push()
        if (fk.isEmpty() || fk.startsWith("local_")) {
            DatabaseReference pushRef = targetDayRef.push();
            pushRef.setValue(item)
                    .addOnSuccessListener(aVoid -> {
                        try {
                            String realKey = pushRef.getKey();
                            item.setFirebaseKey(realKey);
                            Log.d(TAG, "saveSingleItemToFirebase: pushed item key=" + realKey);

                            // Update currentList
                            if (position >= 0 && position < currentList.size()) {
                                currentList.set(position, item);
                            } else {
                                // try to find matching by time+activity and update
                                boolean found = false;
                                for (int i = 0; i < currentList.size(); i++) {
                                    ScheduleItem it = currentList.get(i);
                                    if (equalsByTimeAndActivity(it, item)) {
                                        currentList.set(i, item);
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    currentList.add(item);
                                }
                            }
                            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                        } catch (Exception ex) {
                            Log.w(TAG, "saveSingleItemToFirebase push callback error", ex);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to push item to Firebase: " + e.getMessage(), e);
                        runOnUiThread(() -> Toast.makeText(Layout6Activity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    });
        } else {
            // update existing key
            DatabaseReference nodeRef = targetDayRef.child(fk);
            nodeRef.setValue(item)
                    .addOnSuccessListener(aVoid -> {
                        try {
                            Log.d(TAG, "saveSingleItemToFirebase: updated item key=" + fk);
                            if (position >= 0 && position < currentList.size()) {
                                currentList.set(position, item);
                            } else {
                                boolean replaced = false;
                                for (int i = 0; i < currentList.size(); i++) {
                                    ScheduleItem it = currentList.get(i);
                                    String itk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
                                    if (!itk.isEmpty() && itk.equals(fk)) {
                                        currentList.set(i, item);
                                        replaced = true;
                                        break;
                                    }
                                }
                                if (!replaced) {
                                    for (int i = 0; i < currentList.size(); i++) {
                                        if (equalsByTimeAndActivity(currentList.get(i), item)) {
                                            currentList.set(i, item);
                                            replaced = true;
                                            break;
                                        }
                                    }
                                }
                                if (!replaced) currentList.add(item);
                            }
                            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                        } catch (Exception ex) {
                            Log.w(TAG, "saveSingleItemToFirebase update callback error", ex);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to update item on Firebase: " + e.getMessage(), e);
                        runOnUiThread(() -> Toast.makeText(Layout6Activity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    });
        }
    }
    private void pushSingleActivityHistory(ScheduleItem item, String titleOptional) {
        if (item == null) return;

        String title = titleOptional;
        if (title == null || title.trim().isEmpty()) {
            if (tvTitleHeader != null && tvTitleHeader.getText() != null && !tvTitleHeader.getText().toString().trim().isEmpty()) {
                title = tvTitleHeader.getText().toString().trim();
            } else {
                title = "Lịch ngày " + (item.getDayOfWeek() == 8 ? "CN" : ("Thứ " + item.getDayOfWeek()));
            }
        }

        String time = (item.getStartTime() == null ? "" : item.getStartTime()) + "-" + (item.getEndTime() == null ? "" : item.getEndTime());
        String activity = item.getActivity() == null ? "" : item.getActivity();

        try {
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            String json = profilePrefs.getString(PROFILE_HISTORY_KEY, null);
            JSONArray arr = (json == null) ? new JSONArray() : new JSONArray(json);

            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("subtitle", time + ": " + activity);
            obj.put("day", item.getDayOfWeek());
            obj.put("isUserAdded", true);

            JSONArray acts = new JSONArray();
            JSONObject aobj = new JSONObject();
            aobj.put("time", time);
            aobj.put("activity", activity);
            aobj.put("day", item.getDayOfWeek());
            acts.put(aobj);
            obj.put("activities", acts);

            if (item.getFirebaseKey() != null) obj.put("linkedScheduleKey", item.getFirebaseKey());

            // insert at front
            JSONArray newArr = new JSONArray();
            newArr.put(obj);
            for (int i = 0; i < arr.length(); i++) newArr.put(arr.get(i));
            profilePrefs.edit().putString(PROFILE_HISTORY_KEY, newArr.toString()).apply();

            // push to Firebase history
            DatabaseReference historyRef = rootRef.child("users").child(userId).child("history");
            DatabaseReference p = historyRef.push();
            p.child("title").setValue(title);
            p.child("subtitle").setValue(time + ": " + activity);
            p.child("day").setValue(item.getDayOfWeek());
            p.child("isUserAdded").setValue(true);
            DatabaseReference actsRef = p.child("activities");
            DatabaseReference ap = actsRef.push();
            ap.child("time").setValue(time);
            ap.child("activity").setValue(activity);
            ap.child("day").setValue(item.getDayOfWeek());
            p.child("timestamp").setValue(ServerValue.TIMESTAMP);

        } catch (JSONException ex) {
            Log.e(TAG, "pushSingleActivityHistory error", ex);
        } catch (Exception ex) {
            Log.w(TAG, "pushSingleActivityHistory failed", ex);
        }
    }
    private void deleteScheduleItem(int position, ScheduleItem item) {
        if (item == null) return;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        if (fk.startsWith("builtin_")) {
            Toast.makeText(this, "Không thể xóa lịch mặc định", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fk.startsWith("override_")) {
            // remove from prefs overrides and sync to Firebase overrides node
            removeOverrideAndSync(item.getDayOfWeek(), fk);
            // remove locally
            if (currentList != null && position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            }
            // also remove from local cached per-day list if present
            removeLocalUserItem(item.getDayOfWeek(), fk);

            Toast.makeText(this, "Đã xóa mục (override)", Toast.LENGTH_SHORT).show();
            return;
        }

        // else it's a regular firebase item: remove from schedules/day_N/<fk> or per-user schedule
        DatabaseReference dayRef;
        if (currentScheduleName != null) {
            dayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child("day_" + item.getDayOfWeek());
        } else {
            dayRef = schedulesRef.child("day_" + item.getDayOfWeek());
        }

        if (fk == null || fk.isEmpty()) {
            // no key -> just remove locally (shouldn't normally happen)
            if (currentList != null && position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            }
            Toast.makeText(this, "Đã xóa mục cục bộ", Toast.LENGTH_SHORT).show();
            return;
        }

        dayRef.child(fk).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // remove locally from currentList
                    boolean removed = false;
                    if (currentList != null) {
                        for (int i = 0; i < currentList.size(); i++) {
                            ScheduleItem si = currentList.get(i);
                            if (si.getFirebaseKey() != null && si.getFirebaseKey().equals(fk)) {
                                currentList.remove(i);
                                removed = true;
                                break;
                            }
                        }
                    }
                    if (!removed && currentList != null && position >= 0 && position < currentList.size()) {
                        currentList.remove(position);
                    }
                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

                    // remove from local cache as well
                    removeLocalUserItem(item.getDayOfWeek(), fk);

                    Toast.makeText(Layout6Activity.this, "Đã xóa mục", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(Layout6Activity.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void removeLocalUserItem(int day, String firebaseKey) {
        if (firebaseKey == null || firebaseKey.isEmpty()) return;
        List<ScheduleItem> existing = getLocalUserItems(day);
        boolean changed = false;
        for (int i = existing.size() - 1; i >= 0; i--) {
            ScheduleItem it = existing.get(i);
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (fk.equals(firebaseKey)) {
                existing.remove(i);
                changed = true;
            }
        }
        if (changed) {
            saveLocalUserItems(day, existing);
        }
    }



    private void removeOverrideAndSync(int day, String overrideKey) {
        String key = "overrides_day_" + day;
        List<ScheduleItem> existing = getOverridesFromPrefs(day);
        boolean changed = false;
        for (int i = existing.size() - 1; i >= 0; i--) {
            ScheduleItem it = existing.get(i);
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (fk.equals(overrideKey)) {
                existing.remove(i);
                changed = true;
            }
        }
        // save back to prefs
        JSONArray arr = new JSONArray();
        try {
            for (ScheduleItem it : existing) {
                JSONObject o = new JSONObject();
                o.put("firebaseKey", it.getFirebaseKey());
                o.put("startTime", it.getStartTime());
                o.put("endTime", it.getEndTime());
                o.put("activity", it.getActivity());
                o.put("day", it.getDayOfWeek());
                arr.put(o);
            }
            prefs.edit().putString(key, arr.toString()).apply();
        } catch (JSONException ex) {
            Log.e(TAG, "removeOverrideAndSync - json error", ex);
        }

        // sync to Firebase overrides node: remove then re-push all
        DatabaseReference overridesRef = rootRef.child("users").child(userId).child("overrides_day_" + day);
        overridesRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override public void onComplete(@NonNull Task<Void> t) {
                for (ScheduleItem it : existing) {
                    DatabaseReference p = overridesRef.push();
                    p.child("firebaseKey").setValue(it.getFirebaseKey());
                    p.child("startTime").setValue(it.getStartTime());
                    p.child("endTime").setValue(it.getEndTime());
                    p.child("activity").setValue(it.getActivity());
                    p.child("day").setValue(it.getDayOfWeek());
                }
            }
        });
    }



    // 1) Helper: remove an item from local storage (local_items_day_{day})
    private void removeLocalUserItem(ScheduleItem item) {
        if (item == null) return;
        int day = item.getDayOfWeek();
        List<ScheduleItem> existing = getLocalUserItems(day);
        boolean changed = false;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        for (int i = existing.size() - 1; i >= 0; i--) {
            ScheduleItem ex = existing.get(i);
            String exFk = ex.getFirebaseKey() == null ? "" : ex.getFirebaseKey();
            if (!fk.isEmpty() && fk.equals(exFk)) {
                existing.remove(i);
                changed = true;
            } else {
                // also match by time+activity as fallback if no firebaseKey
                if ((ex.getStartTime() == null ? "" : ex.getStartTime()).equals(item.getStartTime() == null ? "" : item.getStartTime())
                        && (ex.getEndTime() == null ? "" : ex.getEndTime()).equals(item.getEndTime() == null ? "" : item.getEndTime())
                        && (ex.getActivity() == null ? "" : ex.getActivity()).equals(item.getActivity() == null ? "" : item.getActivity())) {
                    existing.remove(i);
                    changed = true;
                }
            }
        }
        if (changed) {
            saveLocalUserItems(day, existing);
        }
    }

    // 2) Main delete handler (call this from adapter's delete callback)


    // Helper: remove an override stored in prefs matching this ScheduleItem
    private void saveOverrideRemove(ScheduleItem item) {
        if (item == null) return;
        int day = item.getDayOfWeek();
        String key = "overrides_day_" + day;
        List<ScheduleItem> existing = getOverridesFromPrefs(day);
        boolean changed = false;
        String fk = item.getFirebaseKey() == null ? "" : item.getFirebaseKey();
        for (int i = existing.size() - 1; i >= 0; i--) {
            ScheduleItem ex = existing.get(i);
            String exFk = ex.getFirebaseKey() == null ? "" : ex.getFirebaseKey();
            if (!fk.isEmpty() && fk.equals(exFk)) {
                existing.remove(i);
                changed = true;
            } else {
                if ((ex.getStartTime() == null ? "" : ex.getStartTime()).equals(item.getStartTime() == null ? "" : item.getStartTime())
                        && (ex.getEndTime() == null ? "" : ex.getEndTime()).equals(item.getEndTime() == null ? "" : item.getEndTime())
                        && (ex.getActivity() == null ? "" : ex.getActivity()).equals(item.getActivity() == null ? "" : item.getActivity())) {
                    existing.remove(i);
                    changed = true;
                }
            }
        }
        if (changed) {
            try {
                JSONArray arr = new JSONArray();
                for (ScheduleItem it : existing) {
                    JSONObject o = new JSONObject();
                    o.put("firebaseKey", it.getFirebaseKey() == null ? "" : it.getFirebaseKey());
                    o.put("startTime", it.getStartTime() == null ? "" : it.getStartTime());
                    o.put("endTime", it.getEndTime() == null ? "" : it.getEndTime());
                    o.put("activity", it.getActivity() == null ? "" : it.getActivity());
                    o.put("day", it.getDayOfWeek());
                    arr.put(o);
                }
                prefs.edit().putString(key, arr.toString()).apply();
            } catch (JSONException ex) {
                Log.e(TAG, "saveOverrideRemove json error", ex);
            }
        }
    }







    private void saveScheduleToHomeDisplay(int day, List<ScheduleItem> items) {
        if (items == null) items = new ArrayList<>();
        try {
            JSONObject root = new JSONObject();
            root.put("day", day);
            JSONArray acts = new JSONArray();
            for (ScheduleItem it : items) {
                JSONObject o = new JSONObject();
                o.put("start", it.getStartTime());
                o.put("end", it.getEndTime());
                o.put("activity", it.getActivity());
                acts.put(o);
            }
            root.put("activities", acts);

            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, root.toString())
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
                    .apply();

            DatabaseReference homeRef = rootRef.child("users").child(userId).child("home_display");
            homeRef.setValue(root.toString());
        } catch (JSONException ex) {
            Log.e(TAG, "saveScheduleToHomeDisplay error", ex);
        }
    }

    private void saveAllWeekScheduleToHomeDisplay() {
        try {
            JSONObject weekData = new JSONObject();

            // Lưu lịch cho tất cả các ngày từ Thứ 2 (2) đến Chủ Nhật (8)
            for (int day = 2; day <= 8; day++) {
                List<ScheduleItem> dayItems = loadScheduleItemsForDay(day);
                JSONArray dayActivities = new JSONArray();

                for (ScheduleItem item : dayItems) {
                    JSONObject actObj = new JSONObject();
                    actObj.put("start", item.getStartTime());
                    actObj.put("end", item.getEndTime());
                    actObj.put("activity", item.getActivity());
                    actObj.put("day", day);
                    dayActivities.put(actObj);
                }

                weekData.put("day_" + day, dayActivities);
            }

            // Lưu vào SharedPreferences
            SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, weekData.toString())
                    .putInt(HOME_DISPLAY_DAY_KEY, selectedDay) // Lưu ngày hiện tại đang chọn
                    .apply();

            // Lưu vào Firebase
            DatabaseReference homeRef = rootRef.child("users").child(userId).child("home_display");
            homeRef.setValue(weekData.toString());

            Log.d(TAG, "Saved all week schedule to home display");
        } catch (JSONException ex) {
            Log.e(TAG, "saveAllWeekScheduleToHomeDisplay error", ex);
            Toast.makeText(this, "Lỗi lưu lịch tuần: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<ScheduleItem> loadScheduleItemsForDay(int day) {
        List<ScheduleItem> items = new ArrayList<>();

        // Nếu có template, load từ template
        if (currentTemplateDetails != null) {
            String dayKey = getDayKeyAsString(day);
            List<TimeSlot> timeSlots = currentTemplateDetails.getWeeklyActivities().get(dayKey);

            if (timeSlots != null) {
                for (TimeSlot slot : timeSlots) {
                    items.add(new ScheduleItem(0, slot.getStartTime(), slot.getEndTime(), slot.getActivityName(), day));
                }
            }
        } else {
            // Nếu không có template, load từ Firebase (đồng bộ - simplified)
            // Trong trường hợp thực tế, bạn có thể cần load async
            items.addAll(getDefaultItemsForDay(day));
            items.addAll(getOverridesFromPrefs(day));
        }

        return items;
    }
private final java.util.Set<String> hiddenBuiltins = new java.util.HashSet<>();

private void loadHiddenBuiltinsFromPrefs() {
    try {
        Set<String> stored = prefs.getStringSet("hidden_builtins", null);
        hiddenBuiltins.clear();
        if (stored != null) hiddenBuiltins.addAll(stored);
    } catch (Exception ex) {
        Log.w(TAG, "loadHiddenBuiltinsFromPrefs failed", ex);
    }
}

// java
// Updated loadScheduleItemsFromFirebase (no unsafe cast)
private void loadScheduleItemsFromFirebase() {
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("schedules");
    ref.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            List<ScheduleItem> loadedItems = new ArrayList<>();
            for (DataSnapshot child : snapshot.getChildren()) {
                ScheduleItem item = child.getValue(ScheduleItem.class);
                if (item == null) continue;
                if ((item.getFirebaseKey() == null || item.getFirebaseKey().isEmpty()) && child.getKey() != null) {
                    item.setFirebaseKey(child.getKey());
                }
                loadedItems.add(item);
            }

            currentList.clear();
            for (ScheduleItem s : loadedItems) {
                String fk = s.getFirebaseKey() == null ? "" : s.getFirebaseKey();
                if (s.isBuiltin() && hiddenBuiltins.contains(fk)) continue;
                currentList.add(s);
            }

            runOnUiThread(() -> {
                // Prefer adapter.updateList(...) if available
                boolean updated = false;
                if (scheduleAdapter != null) {
                    try {
                        scheduleAdapter.updateList(currentList);
                        updated = true;
                    } catch (Exception ignored) { }
                }

                if (updated) return;

                RecyclerView.Adapter rvAdapter = (scheduleRecyclerView != null) ? scheduleRecyclerView.getAdapter() : null;
                if (rvAdapter instanceof androidx.recyclerview.widget.ListAdapter) {
                    // safe cast only when the attached adapter actually implements ListAdapter
                    ((androidx.recyclerview.widget.ListAdapter) rvAdapter).submitList(new ArrayList<>(currentList));
                } else if (rvAdapter != null) {
                    rvAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onCancelled(DatabaseError error) {
            Log.w(TAG, "loadScheduleItemsFromFirebase cancelled", error.toException());
        }
    });
}

// java
// Updated addTemplateToCurrentList (no unsafe cast)
private void addTemplateToCurrentList(DetailedSchedule ds) {
    if (ds == null) return;
    Map<String, List<TimeSlot>> weekly = ds.getWeeklyActivities();
    if (weekly == null) return;

    List<ScheduleItem> loadedItems = new ArrayList<>();
    for (Map.Entry<String, List<TimeSlot>> e : weekly.entrySet()) {
        for (TimeSlot ts : e.getValue()) {
            ScheduleItem item = new ScheduleItem();
            item.setStartTime(ts.getStartTime());
            item.setEndTime(ts.getEndTime());
            item.setActivity(ts.getActivity());
            item.setFirebaseKey(ts.getFirebaseKey());
            item.setBuiltin(ts.isBuiltin());
            loadedItems.add(item);
        }
    }

    currentList.clear();
    for (ScheduleItem s : loadedItems) {
        String fk = s.getFirebaseKey() == null ? "" : s.getFirebaseKey();
        if (s.isBuiltin() && hiddenBuiltins.contains(fk)) continue;
        currentList.add(s);
    }

    // Update adapter safely
    boolean updated = false;
    if (scheduleAdapter != null) {
        try {
            scheduleAdapter.updateList(currentList);
            updated = true;
        } catch (Exception ignored) { }
    }

    if (updated) return;

    RecyclerView.Adapter rvAdapter = (scheduleRecyclerView != null) ? scheduleRecyclerView.getAdapter() : null;
    if (rvAdapter instanceof androidx.recyclerview.widget.ListAdapter) {
        ((androidx.recyclerview.widget.ListAdapter) rvAdapter).submitList(new ArrayList<>(currentList));
    } else if (rvAdapter != null) {
        rvAdapter.notifyDataSetChanged();
    }
}

    private List<ScheduleItem> getOverridesFromPrefs(int day) {
        String key = "overrides_day_" + day;
        String json = prefs.getString(key, null);
        List<ScheduleItem> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ScheduleItem it = new ScheduleItem();
                it.setFirebaseKey(o.optString("firebaseKey"));
                it.setStartTime(o.optString("startTime"));
                it.setEndTime(o.optString("endTime"));
                it.setActivity(o.optString("activity"));
                it.setDayOfWeek(o.optInt("day", day));
                list.add(it);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "parse overrides error", ex);
        }
        return list;
    }

    // --- RESTORED METHODS START ---

    private void saveOverrideToPrefs(int day, ScheduleItem override) {
        if (override == null) return;
        String key = "overrides_day_" + day;
        List<ScheduleItem> existing = getOverridesFromPrefs(day);

        boolean updated = false;
        if (override.getFirebaseKey() == null || override.getFirebaseKey().isEmpty()) {
            override.setFirebaseKey("override_" + System.currentTimeMillis());
        }
        for (int i = 0; i < existing.size(); i++) {
            ScheduleItem ex = existing.get(i);
            if (ex.getFirebaseKey() != null && ex.getFirebaseKey().equals(override.getFirebaseKey())) {
                existing.set(i, override);
                updated = true;
                break;
            }
        }
        if (!updated) existing.add(override);

        JSONArray arr = new JSONArray();
        try {
            for (ScheduleItem it : existing) {
                JSONObject o = new JSONObject();
                o.put("firebaseKey", it.getFirebaseKey());
                o.put("startTime", it.getStartTime());
                o.put("endTime", it.getEndTime());
                o.put("activity", it.getActivity());
                o.put("day", it.getDayOfWeek());
                arr.put(o);
            }
            prefs.edit().putString(key, arr.toString()).apply();
            Log.d(TAG, "Saved override to prefs for day " + day);

            DatabaseReference overridesRef = rootRef.child("users").child(userId).child("overrides_day_" + day);
            overridesRef.removeValue().addOnCompleteListener(t -> {
                for (ScheduleItem it : existing) {
                    DatabaseReference p = overridesRef.push();
                    p.child("firebaseKey").setValue(it.getFirebaseKey());
                    p.child("startTime").setValue(it.getStartTime());
                    p.child("endTime").setValue(it.getEndTime());
                    p.child("activity").setValue(it.getActivity());
                    p.child("day").setValue(it.getDayOfWeek());
                }
            });

        } catch (JSONException ex) {
            Log.e(TAG, "save override error", ex);
        }
    }

    private void saveScheduleToFirebase() {
        if (currentList == null) currentList = new ArrayList<>();

        String dayNode = "day_" + selectedDay;
        List<ScheduleItem> toUpload = new ArrayList<>();
        for (ScheduleItem it : currentList) {
            // create a copy or clear transient fields if needed; keep firebaseKey as-is
            ScheduleItem copy = new ScheduleItem(it.getId(), it.getStartTime(), it.getEndTime(), it.getActivity(), it.getDayOfWeek());
            copy.setFirebaseKey(it.getFirebaseKey());
            toUpload.add(copy);
        }

        DatabaseReference dayRef;
        if (currentScheduleName != null && !currentScheduleName.trim().isEmpty()) {
            dayRef = rootRef.child("users").child(userId).child("schedules").child(currentScheduleName).child(dayNode);
        } else {
            dayRef = schedulesRef.child(dayNode);
        }

        dayRef.setValue(toUpload)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved schedule for " + dayNode + " to " + (currentScheduleName != null ? "user schedule" : "global schedules")))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed saving schedule for " + dayNode, e);
                    Toast.makeText(this, "Lỗi khi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void saveScheduleToProfileHistory(int day) {
        if (currentList == null) currentList = new ArrayList<>();

        SharedPreferences profilePrefs = getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        String json = profilePrefs.getString(PROFILE_HISTORY_KEY, null);
        JSONArray arr;
        try {
            arr = (json == null) ? new JSONArray() : new JSONArray(json);
        } catch (JSONException ex) {
            arr = new JSONArray();
        }

        String title = (tvTitleHeader != null && tvTitleHeader.getText() != null && !tvTitleHeader.getText().toString().trim().isEmpty())
                ? tvTitleHeader.getText().toString().trim()
                : ("Lịch ngày " + (day == 8 ? "CN" : ("Thứ " + day)));

        JSONArray actsArr = new JSONArray();
        StringBuilder subtitleSb = new StringBuilder();
        int maxToShow = 6;
        int count = 0;
        for (ScheduleItem it : currentList) {
            String s = it.getStartTime() == null ? "" : it.getStartTime();
            String e = it.getEndTime() == null ? "" : it.getEndTime();
            String a = it.getActivity() == null ? "" : it.getActivity();
            String row = s + "-" + e + ": " + a;
            try {
                JSONObject actObj = new JSONObject();
                actObj.put("time", row);
                actObj.put("activity", a);
                actObj.put("day", it.getDayOfWeek());
                actsArr.put(actObj);
            } catch (JSONException ex) {
                Log.w(TAG, "activity json error", ex);
            }
            if (count < maxToShow) {
                if (subtitleSb.length() > 0) subtitleSb.append("  •  ");
                subtitleSb.append(row);
            }
            count++;
        }
        if (subtitleSb.length() == 0) subtitleSb.append("Không có mục cụ thể");
        String subtitle = subtitleSb.toString();

        try {
            if (editingHistoryKey == null) {
                JSONObject obj = new JSONObject();
                obj.put("title", title);
                obj.put("subtitle", subtitle);
                obj.put("day", day);
                obj.put("activities", actsArr);
                arr.put(obj);
                profilePrefs.edit().putString(PROFILE_HISTORY_KEY, arr.toString()).apply();
                Log.d(TAG, "Saved schedule summary locally to profile history: " + title);

                pushScheduleSummaryToFirebase(null, title, subtitle, day, currentList);
            } else {
                boolean updatedLocal = false;
                try {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String hk = o.optString("historyKey", null);
                        if (hk != null && hk.equals(editingHistoryKey)) {
                            o.put("title", title);
                            o.put("subtitle", subtitle);
                            o.put("day", day);
                            o.put("activities", actsArr);
                            updatedLocal = true;
                            break;
                        }
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "local update error", ex);
                }
                if (!updatedLocal) {
                    JSONObject obj = new JSONObject();
                    obj.put("historyKey", editingHistoryKey);
                    obj.put("title", title);
                    obj.put("subtitle", subtitle);
                    obj.put("day", day);
                    obj.put("activities", actsArr);
                    arr.put(obj);
                }
                profilePrefs.edit().putString(PROFILE_HISTORY_KEY, arr.toString()).apply();
                Log.d(TAG, "Updated schedule summary locally for key=" + editingHistoryKey);

                pushScheduleSummaryToFirebase(editingHistoryKey, title, subtitle, day, currentList);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "saveScheduleToProfileHistory error", ex);
        }
    }

    private void pushScheduleSummaryToFirebase(String historyKey, String title, String subtitle, int day, List<ScheduleItem> items) {
        try {
            DatabaseReference historyRef = rootRef.child("users").child(userId).child("history");
            DatabaseReference targetRef;
            if (historyKey == null) {
                targetRef = historyRef.push();
            } else {
                targetRef = historyRef.child(historyKey);
            }

            targetRef.child("title").setValue(title);
            targetRef.child("subtitle").setValue(subtitle);
            targetRef.child("day").setValue(day);

            DatabaseReference actsRef = targetRef.child("activities");
            actsRef.removeValue().addOnCompleteListener(t -> {
                for (ScheduleItem it : items) {
                    String s = it.getStartTime() == null ? "" : it.getStartTime();
                    String e = it.getEndTime() == null ? "" : it.getEndTime();
                    String a = it.getActivity() == null ? "" : it.getActivity();
                    String row = s + "-" + e + ": " + a;
                    actsRef.push().setValue(row);
                }
                targetRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
            });

            if (historyKey == null) {
                Log.d(TAG, "Pushed new schedule summary to Firebase for user=" + userId);
            } else {
                Log.d(TAG, "Updated schedule summary to Firebase for key=" + historyKey);
            }
        } catch (Exception ex) {
            Log.w(TAG, "pushScheduleSummaryToFirebase failed", ex);
        }
    }

    private boolean isOverlapping(String newStart, String newEnd, List<ScheduleItem> existing, ScheduleItem exclude) {
        int ns = timeToMinutes(newStart);
        int ne = timeToMinutes(newEnd);
        if (ns < 0 || ne < 0 || ne <= ns) return true;

        if (existing == null) return false;
        for (ScheduleItem it : existing) {
            if (exclude != null && it == exclude) continue;
            String s = it.getStartTime();
            String e = it.getEndTime();
            int is = timeToMinutes(s);
            int ie = timeToMinutes(e);
            if (is < 0 || ie < 0) continue;
            if (ns < ie && is < ne) return true;
        }
        return false;
    }

    private boolean equalsByTimeAndActivity(ScheduleItem a, ScheduleItem b) {
        if (a == null || b == null) return false;
        String as = a.getStartTime() == null ? "" : a.getStartTime();
        String ae = a.getEndTime() == null ? "" : a.getEndTime();
        String aa = a.getActivity() == null ? "" : a.getActivity();
        String bs = b.getStartTime() == null ? "" : b.getStartTime();
        String be = b.getEndTime() == null ? "" : b.getEndTime();
        String ba = b.getActivity() == null ? "" : b.getActivity();
        return as.equals(bs) && ae.equals(be) && aa.equals(ba);
    }

    private List<ScheduleItem> getDefaultItemsForDay(int day) {
        List<ScheduleItem> defaults = new ArrayList<>();
        // Based on the original file, it seems there was a complex switch here.
        // For now, returning an empty list as the main logic is template-based or from Firebase.
        // You can re-paste the original switch/case logic here if needed.
        return defaults;
    }

    private int timeToMinutes(String hhmm) {
        if (hhmm == null || !hhmm.contains(":")) return -1;
        try {
            String[] parts = hhmm.split(":");
            int hh = Integer.parseInt(parts[0].trim());
            int mm = Integer.parseInt(parts[1].trim());
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return -1;
            return hh * 60 + mm;
        } catch (Exception e) {
            return -1;
        }
    }

    // --- RESTORED METHODS END ---

    /**
     * Resolve template details by title.
     * First try to find a saved (custom) template with the same title via ScheduleData.getAllTemplates(context).
     * If none found, fallback to creating a default template by title (original behavior).
     */
    private DetailedSchedule getTemplateDetailsByTitle(String title) {
        if (title == null) return null;

        // 1) Try to find among saved templates (defaults + custom saved ones)
        try {
            List<ScheduleTemplate> all = ScheduleData.getAllTemplates(this);
            if (all != null) {
                for (ScheduleTemplate st : all) {
                    if (st == null) continue;
                    String t = st.getTitle();
                    if (t == null) continue;
                    if (t.trim().equalsIgnoreCase(title.trim())) {
                        if (st instanceof DetailedSchedule) {
                            return (DetailedSchedule) st;
                        } else {
                            // If it's a ScheduleTemplate but not DetailedSchedule, attempt to convert if possible
                            // (assuming ScheduleTemplate has getWeeklyActivities etc.)
                            try {
                                return new DetailedSchedule(
                                        st.getTitle(),
                                        st.getDescription(),
                                        st.getTags(),
                                        st.getWeeklyActivities()
                                );
                            } catch (Exception ignored) { }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Error while searching saved templates for title=" + title, ex);
        }

        // 2) Fallback to original factory methods
        switch (title) {
            case "CÚ ĐÊM":
                return ScheduleData.createNightOwlTemplate();
            case "Chuyên buổi sáng":
                return ScheduleData.createMorningPersonTemplate();
            case "Học môn chuyên sâu":
                return ScheduleData.createDeepWorkTemplate();
            case "Sáng học, tối chơi":
                return ScheduleData.createWorkHardPlayHardTemplate();
            case "Tối ưu hóa chu kỳ ngủ":
                return ScheduleData.createSprintWeekTemplate();
            case "Vừa học vừa ăn gián đoạn 16-8":
                return ScheduleData.createIntermittentFastingTemplate();
            case "Vừa học vừa làm":
                return ScheduleData.createWorkAndLearnTemplate();
            case "Năng suất theo khoa học":
                return ScheduleData.createScientificProductivityTemplate();
            case "Lịch sinh hoạt của Bác Hồ":
                return ScheduleData.createUncleHoLifestyleTemplate();
            default:
                return null;
        }
    }
    private int timeToMinutesOrMax(String hhmm) {
        int m = timeToMinutes(hhmm);
        return m < 0 ? Integer.MAX_VALUE : m;
    }

    // Add this helper method inside Layout6Activity (near other persistence methods).
// Use it wherever you currently call saveSingleItemToFirebase(...) + addToCurrentTemplateAndSave(...).
    private void addAndPersistNewItem(ScheduleItem newItem) {
        if (newItem == null) return;
        if (newItem.getDayOfWeek() <= 0) newItem.setDayOfWeek(selectedDay);
        if (currentList == null) currentList = new ArrayList<>();
        currentList.add(newItem);

        currentList.sort((a, b) -> Integer.compare(
                timeToMinutesOrMax(a.getStartTime()),
                timeToMinutesOrMax(b.getStartTime())
        ));
        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
        saveSingleItemToFirebase(newItem); // this method in this class already handles generating key and pushing
        addOrUpdateLocalUserItem(newItem);
        addToCurrentTemplateAndSave(newItem);
        pushSingleActivityHistory(newItem, null);
        Toast.makeText(this, "Đã thêm và lưu hoạt động: " + (newItem.getActivity() == null ? "" : newItem.getActivity()), Toast.LENGTH_SHORT).show();
    }
    private String getDayKeyAsString(int day) {
        switch (day) {
            case 2: return "Thứ 2";
            case 3: return "Thứ 3";
            case 4: return "Thứ 4";
            case 5: return "Thứ 5";
            case 6: return "Thứ 6";
            case 7: return "Thứ 7";
            case 8: return "Chủ Nhật";
            default: return "";
        }
    }

}
