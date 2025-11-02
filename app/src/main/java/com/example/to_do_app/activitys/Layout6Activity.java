package com.example.to_do_app.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapters.ScheduleItemAdapter;
import com.example.to_do_app.model.ScheduleItem;
import com.example.to_do_app.model.ScheduleTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Layout6Activity extends AppCompatActivity {

    private static final String TAG = "Layout6Activity";

    private CardView btnBack;
    private android.widget.TextView tvTitleHeader;
    private Button btnApplySchedule;
    private RecyclerView scheduleRecyclerView;
    private ScheduleItemAdapter scheduleAdapter;
    private List<ScheduleItem> currentList;

    // Day cards
    private LinearLayout day2, day3, day4, day5, day6, day7, dayCN;
    private View selectedDayView;
    private int selectedDay = 2; // default Thứ 2

    private LinearLayout daysContainer;
    private FloatingActionButton fabAdd;

    // Firebase
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manhinh_lichduocchon);

        currentList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("schedules");

        bindViews();

        // read selected day early if caller sent it
        selectedDay = getIntent().getIntExtra("selected_day", selectedDay);

        // init RecyclerView + adapter
        setupRecyclerView();

        // Read extras sent from template adapter
        String passedTitle = getIntent().getStringExtra("EXTRA_TEMPLATE_TITLE");
        String passedDescription = getIntent().getStringExtra("EXTRA_TEMPLATE_DESCRIPTION");
        ArrayList<String> passedTags = getIntent().getStringArrayListExtra("EXTRA_TEMPLATE_TAGS");

        // Nếu truyền tiêu đề template, cập nhật header ngay
        if (passedTitle != null && !passedTitle.isEmpty() && tvTitleHeader != null) {
            tvTitleHeader.setText(passedTitle);
        }

        boolean hasTemplate = (passedTitle != null && !passedTitle.isEmpty())
                || (passedDescription != null && !passedDescription.isEmpty())
                || (passedTags != null && !passedTags.isEmpty());

        if (hasTemplate) {
            // create preview ScheduleItem and show it immediately
            ScheduleItem preview = new ScheduleItem(0, "06:00", "07:00",
                    (passedTitle != null && !passedTitle.isEmpty()) ? passedTitle :
                            (passedDescription != null ? passedDescription : "Activity"),
                    selectedDay);
            currentList.clear();
            currentList.add(preview);

            // update adapter on UI thread
            runOnUiThread(() -> {
                if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            });

            // Do NOT call loadScheduleDataForDay(...) here unless you intend to merge results.
        } else {
            // normal flow: load from firebase for selected day
            loadScheduleDataForDay(selectedDay);
        }

        setupDays();
        setupListeners();
    }

    // Tạo ScheduleItem đơn từ title/description (một chỗ để dễ chỉnh)
    private ScheduleItem createScheduleItemFromTemplate(String title, String description) {
        String titleToUse = (title != null && !title.isEmpty()) ? title
                : (description != null && !description.isEmpty() ? description : "Activity");
        String defaultStart = "06:00";
        String defaultEnd = "07:00";
        int defaultDay = 2;
        return new ScheduleItem(0, defaultStart, defaultEnd, titleToUse, defaultDay);
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

        daysContainer = findViewById(R.id.daysContainer);
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

        // default selection
        day2.setSelected(true);
        selectedDayView = day2;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnApplySchedule.setOnClickListener(v -> showApplyDialog());

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddDialog());
        }
    }

    private void loadScheduleDataForDay(int day) {
        if (currentList == null) currentList = new ArrayList<>();
        currentList.clear();
        String dayNode = "day_" + day;
        databaseReference.child(dayNode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ScheduleItem> firebaseItems = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ScheduleItem item = ds.getValue(ScheduleItem.class);
                    if (item != null) {
                        // ensure we keep firebase key stored in model for later edits/deletes
                        if (item.getFirebaseKey() == null || item.getFirebaseKey().isEmpty()) {
                            item.setFirebaseKey(ds.getKey());
                        }
                        firebaseItems.add(item);
                    }
                }

                // Nếu hiện đang có preview (ví dụ user vừa chuyển từ template), merge thay vì overwrite
                if (currentList != null && !currentList.isEmpty()) {
                    mergeFirebaseItems(firebaseItems);
                } else {
                    currentList.clear();
                    currentList.addAll(firebaseItems);
                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
                }

                if (currentList.isEmpty()) {
                    Toast.makeText(Layout6Activity.this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Layout6Activity.this, "Lỗi tải lịch: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

                    item.setStartTime(newStart);
                    item.setEndTime(newEnd);
                    item.setActivity(newAct);

                    // If item has firebaseKey, update the single node. Otherwise, update local list only.
                    if (item.getFirebaseKey() != null && !item.getFirebaseKey().isEmpty()) {
                        String key = item.getFirebaseKey();
                        databaseReference.child("day_" + item.getDayOfWeek()).child(key).setValue(item)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated item in Firebase key=" + key))
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    if (scheduleAdapter != null) scheduleAdapter.notifyItemChanged(position);
                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showApplyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Áp dụng lịch này")
                .setMessage("Bạn muốn:")
                .setPositiveButton("Hiển thị ở màn hình chính", (dialog, which) -> {
                    saveScheduleToFirebase();
                    Toast.makeText(this, "Đã lưu và áp dụng", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Chỉ lưu vào lịch sử", (dialog, which) -> {
                    saveScheduleToFirebase();
                    Toast.makeText(this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Hủy", null)
                .show();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.edit_schedule1, null); // reuse the same layout if appropriate
        EditText etStart = view.findViewById(R.id.etStartTime);
        EditText etEnd = view.findViewById(R.id.etEndTime);
        EditText etAct = view.findViewById(R.id.etActivity);

        // Optionally put default times
        etStart.setHint("06:00");
        etEnd.setHint("07:00");

        builder.setView(view)
                .setTitle("Thêm lịch trình")
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String newStart = etStart.getText().toString().trim();
                    String newEnd = etEnd.getText().toString().trim();
                    String newAct = etAct.getText().toString().trim();

                    if (newStart.isEmpty() || newEnd.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create new item for currently selected day
                    ScheduleItem newItem = new ScheduleItem(0, newStart, newEnd, newAct, selectedDay);

                    // Add to current list and update adapter
                    if (currentList == null) currentList = new ArrayList<>();
                    currentList.add(newItem);

                    // sort optionally (by startTime)
                    currentList.sort((a, b) -> {
                        String as = a.getStartTime() == null ? "" : a.getStartTime();
                        String bs = b.getStartTime() == null ? "" : b.getStartTime();
                        return as.compareTo(bs);
                    });

                    if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);

                    // Save immediately to Firebase (generate key, store key in model)
                    saveSingleItemToFirebase(newItem);

                    Toast.makeText(this, "Đã thêm mục lịch", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Save a single ScheduleItem to Firebase under the selected day using push() but set the key into the item.
     * This allows later edits/deletes using the firebaseKey.
     */
    private void saveSingleItemToFirebase(ScheduleItem item) {
        if (item == null) return;
        String dayNode = "day_" + item.getDayOfWeek();
        DatabaseReference dayRef = databaseReference.child(dayNode);

        // generate key first
        String key = dayRef.push().getKey();
        if (key == null) {
            // fallback: push directly without key stored
            dayRef.push().setValue(item)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item (no key) to Firebase"))
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        // set key into model so we can later update/delete easily
        item.setFirebaseKey(key);

        // save under the generated key
        dayRef.child(key).setValue(item)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved single item to Firebase with key=" + key))
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Optional helper to merge Firebase-loaded items with current preview list (instead of overwriting)
    // Call this from loadScheduleDataForDay if you want to keep preview and append real data:
    private void mergeFirebaseItems(List<ScheduleItem> firebaseItems) {
        if (currentList == null) currentList = new ArrayList<>();
        if (firebaseItems == null || firebaseItems.isEmpty()) {
            // nothing to merge
            if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
            return;
        }

        for (ScheduleItem item : firebaseItems) {
            boolean exists = false;
            for (ScheduleItem c : currentList) {
                String cs = c.getStartTime() == null ? "" : c.getStartTime();
                String ce = c.getEndTime() == null ? "" : c.getEndTime();
                String as = item.getStartTime() == null ? "" : item.getStartTime();
                String ae = item.getEndTime() == null ? "" : item.getEndTime();
                String ca = c.getActivity() == null ? "" : c.getActivity();
                String ia = item.getActivity() == null ? "" : item.getActivity();

                if (cs.equals(as) && ce.equals(ae) && ca.equals(ia)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) currentList.add(item);
        }

        // Optional: sort by startTime if format is HH:mm
        currentList.sort((a, b) -> {
            String as = a.getStartTime() == null ? "" : a.getStartTime();
            String bs = b.getStartTime() == null ? "" : b.getStartTime();
            return as.compareTo(bs);
        });

        if (scheduleAdapter != null) scheduleAdapter.updateList(currentList);
    }

    private void saveScheduleToFirebase() {
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "Danh sách lịch trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String dayNode = "day_" + selectedDay;
        databaseReference.child(dayNode).setValue(currentList)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã lưu lịch vào Firebase", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}