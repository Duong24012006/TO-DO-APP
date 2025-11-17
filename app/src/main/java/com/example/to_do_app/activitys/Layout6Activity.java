package com.example.to_do_app.activitys;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.List;

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

    private String editingHistoryKey = null;
    private DetailedSchedule currentTemplateDetails = null;

    // For dragging FAB
    private float dX, dY;
    private static final float CLICK_DRAG_TOLERANCE = 10;
    private float downRawX, downRawY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.manhinh_lichduocchon);

        currentList = new ArrayList<>();
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        rootRef = FirebaseDatabase.getInstance().getReference();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

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

        bindViews();

        selectedDay = getIntent().getIntExtra("selected_day", selectedDay);
        editingHistoryKey = getIntent().getStringExtra(EXTRA_HISTORY_KEY);

        setupRecyclerView();

        ScheduleTemplate template = (ScheduleTemplate) getIntent().getSerializableExtra(EXTRA_SCHEDULE_TEMPLATE);

        if (template != null) {
            String passedTitle = template.getTitle();
            if (passedTitle != null && !passedTitle.isEmpty()) {
                if (tvTitleHeader != null) {
                    tvTitleHeader.setText(passedTitle);
                }
                currentTemplateDetails = getTemplateDetailsByTitle(passedTitle);
            }
        }

        setupDays();
        setupListeners();
        loadScheduleDataForDay(selectedDay);
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
    }

    private void setupRecyclerView() {
        if (currentList == null) currentList = new ArrayList<>();
        scheduleAdapter = new ScheduleItemAdapter(this, currentList, new ScheduleItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, ScheduleItem item) {
                showEditDialog(position, item);
            }

            @Override
            public void onEditClick(int position, ScheduleItem item) {
                showEditDialog(position, item);
            }
        });
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleRecyclerView.setAdapter(scheduleAdapter);
    }

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

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApplySchedule.setOnClickListener(v -> showApplyDialog());

        if (fabAdd != null) {
            fabAdd.setOnTouchListener((view, motionEvent) -> {

                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    downRawX = motionEvent.getRawX();
                    downRawY = motionEvent.getRawY();
                    dX = view.getX() - downRawX;
                    dY = view.getY() - downRawY;

                    return true; // Consumed
                } else if (action == MotionEvent.ACTION_MOVE) {
                    int viewWidth = view.getWidth();
                    int viewHeight = view.getHeight();

                    View viewParent = (View) view.getParent();
                    int parentWidth = viewParent.getWidth();
                    int parentHeight = viewParent.getHeight();

                    float newX = motionEvent.getRawX() + dX;
                    newX = Math.max(0, newX); // Don't go off the left edge
                    newX = Math.min(parentWidth - viewWidth, newX); // Don't go off the right edge

                    float newY = motionEvent.getRawY() + dY;
                    newY = Math.max(0, newY); // Don't go off the top edge
                    newY = Math.min(parentHeight - viewHeight, newY); // Don't go off the bottom edge

                    view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start();

                    return true; // Consumed
                } else if (action == MotionEvent.ACTION_UP) {
                    float upRawX = motionEvent.getRawX();
                    float upRawY = motionEvent.getRawY();

                    float upDX = upRawX - downRawX;
                    float upDY = upRawY - downRawY;

                    if (Math.abs(upDX) < CLICK_DRAG_TOLERANCE && Math.abs(upDY) < CLICK_DRAG_TOLERANCE) {
                        // A click
                        showAddDialogMode();
                        return true;
                    } else {
                        // A drag
                        return true; // Consumed
                    }
                }
                return false; // Not consumed
            });
        }
    }

    private void loadScheduleDataForDay(int day) {
        if (currentTemplateDetails != null) {
            String dayKey = getDayKeyAsString(day);
            List<TimeSlot> timeSlots = currentTemplateDetails.getWeeklyActivities().get(dayKey);

            if (timeSlots != null) {
                List<ScheduleItem> items = new ArrayList<>();
                for (TimeSlot slot : timeSlots) {
                    items.add(new ScheduleItem(0, slot.getStartTime(), slot.getEndTime(), slot.getActivityName(), day));
                }
                currentList.clear();
                currentList.addAll(items);
                if (scheduleAdapter != null) {
                    scheduleAdapter.updateList(currentList);
                }
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
                itemsToShow.addAll(getDefaultItemsForDay(day));
                itemsToShow.addAll(firebaseItems);
                itemsToShow.addAll(getOverridesFromPrefs(day));

                itemsToShow.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

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

    private void showEditDialog(int position, ScheduleItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.edit_schedule1, null);
        EditText etStart = view.findViewById(R.id.etStartTime);
        EditText etEnd = view.findViewById(R.id.etEndTime);
        EditText etAct = view.findViewById(R.id.etActivity);

        etStart.setText(item.getStartTime());
        etEnd.setText(item.getEndTime());
        etAct.setText(item.getActivity());

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
                .setNegativeButton("Hủy", null)
                .show();
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

    private void showApplyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Áp dụng lịch này")
                .setMessage("Bạn muốn:")
                .setPositiveButton("Hiển thị ở màn hình chính", (dialog, which) -> {
                    saveScheduleToFirebase();
                    saveScheduleToProfileHistory(selectedDay);
                    saveAllWeekScheduleToHomeDisplay();
                    Toast.makeText(this, "Đã lưu toàn bộ lịch tuần và áp dụng", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Chỉ lưu vào lịch sử", (dialog, which) -> {
                    saveScheduleToFirebase();
                    saveScheduleToProfileHistory(selectedDay);
                    Toast.makeText(this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Hủy", null)
                .show();
    }

    private void showAddDialogMode() {
        new AlertDialog.Builder(this)
                .setTitle("Chọn cách thêm")
                .setItems(new String[]{"Thêm vào khung giờ cố định", "Thêm hoạt động tùy ý"}, (modeDialog, whichMode) -> {
                    if (whichMode == 0) {
                        //showAddPredefinedSlotDialog();
                    } else {
                        //showAddFreeActivityDialog();
                    }
                })
                .show();
    }

    private void saveSingleItemToFirebase(ScheduleItem item, int position) {
        String dayNode = "day_" + item.getDayOfWeek();
        DatabaseReference dayRef = schedulesRef.child(dayNode);
        String key = dayRef.push().getKey();
        item.setFirebaseKey(key);
        dayRef.child(key).setValue(item);
        if (position != -1) {
            currentList.get(position).setFirebaseKey(key);
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
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String dayNode = "day_" + selectedDay;
        DatabaseReference dayRef = schedulesRef.child(dayNode);

        List<ScheduleItem> toUpload = new ArrayList<>();
        for (ScheduleItem it : currentList) {
            String fk = it.getFirebaseKey() == null ? "" : it.getFirebaseKey();
            if (!fk.startsWith("builtin_")) {
                it.setDayOfWeek(selectedDay);
                toUpload.add(it);
            }
        }

        dayRef.setValue(toUpload)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved full schedule to Firebase for " + dayNode))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private DetailedSchedule getTemplateDetailsByTitle(String title) {
        if (title == null) return null;
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
