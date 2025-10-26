package com.example.to_do_app.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.to_do_app.R;
import com.example.to_do_app.adapter.TimeAdapter;
import com.example.to_do_app.data.ScheduleData;
import com.example.to_do_app.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout6Activity (manhinh_lichduocchon) - cleaned and fixed imports/usages so it compiles in Android Studio.
 *
 * Notes:
 * - Uses androidx.cardview.widget.CardView for btnBack and day cards (no conflicting android.widget.CardView).
 * - Does not use Java import aliasing (which is invalid in Java).
 * - Uses anonymous inner classes for click listeners for maximum compatibility.
 */
public class Layout6Activity extends AppCompatActivity {

    private CardView btnBack;
    private Button btnApplySchedule;
    private RecyclerView scheduleRecyclerView;
    private TimeAdapter timeAdapter;
    private List<TimeSlot> currentList;

    // day cards
    private CardView day2, day3, day4, day5, day6, day7, dayCN;
    private View selectedDayView; // currently selected day view
    private int selectedDay = 2; // default selected day (Thứ 2)

    private LinearLayout daysContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manhinh_lichduocchon);

        bindViews();
        setupRecyclerView();
        setupDays();
        setupListeners();

        // load initial data for selectedDay
        loadScheduleDataForDay(selectedDay);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnApplySchedule = findViewById(R.id.btnApplySchedule);
        scheduleRecyclerView = findViewById(R.id.scheduleRecyclerView);

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
        currentList = new ArrayList<>();
        timeAdapter = new TimeAdapter(currentList, new TimeAdapter.OnEditClickListener() {
            @Override
            public void onEditClick(int position, TimeSlot slot) {
                showEditDialog(position, slot);
            }
        });
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scheduleRecyclerView.setAdapter(timeAdapter);
    }

    private void setupDays() {
        // Example: mark weekday 'today' as activated (for demo set today = 4)
        int today = 4;
        if (today == 2) day2.setActivated(true);
        else if (today == 3) day3.setActivated(true);
        else if (today == 4) day4.setActivated(true);
        else if (today == 5) day5.setActivated(true);
        else if (today == 6) day6.setActivated(true);
        else if (today == 7) day7.setActivated(true);
        else dayCN.setActivated(true);

        // Click listener for days
        View.OnClickListener dayClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // clear previous selection
                if (selectedDayView != null) selectedDayView.setSelected(false);

                // set new selection
                v.setSelected(true);
                selectedDayView = v;

                // map id to day number
                int id = v.getId();
                if (id == R.id.day2) selectedDay = 2;
                else if (id == R.id.day3) selectedDay = 3;
                else if (id == R.id.day4) selectedDay = 4;
                else if (id == R.id.day5) selectedDay = 5;
                else if (id == R.id.day6) selectedDay = 6;
                else if (id == R.id.day7) selectedDay = 7;
                else if (id == R.id.dayCN) selectedDay = 8;

                // reload schedule
                loadScheduleDataForDay(selectedDay);
            }
        };

        day2.setOnClickListener(dayClick);
        day3.setOnClickListener(dayClick);
        day4.setOnClickListener(dayClick);
        day5.setOnClickListener(dayClick);
        day6.setOnClickListener(dayClick);
        day7.setOnClickListener(dayClick);
        dayCN.setOnClickListener(dayClick);

        // select default visually
        day2.setSelected(true);
        selectedDayView = day2;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // try to go back to ManhinhChonlichActivity if it exists; otherwise finish()
                try {
                    Intent it = new Intent(Layout6Activity.this,
                            Class.forName("com.example.to_do_app.activity.ManhinhChonlichActivity"));
                    startActivity(it);
                    finish();
                } catch (ClassNotFoundException e) {
                    finish();
                }
            }
        });

        btnApplySchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApplyDialog();
            }
        });
    }

    /**
     * Load schedule data for a given day.
     * Replace this with real DB/SharedPreferences loading in your app.
     */
    private void loadScheduleDataForDay(int day) {
        currentList.clear();
        currentList.addAll(ScheduleData.getScheduleForDay(day));
        timeAdapter.updateList(currentList);
    }

    private void showEditDialog(final int position, final TimeSlot slot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_schedule, null);
        final EditText etStart = view.findViewById(R.id.etStartTime);
        final EditText etEnd = view.findViewById(R.id.etEndTime);
        final EditText etAct = view.findViewById(R.id.etActivity);

        etStart.setText(slot.getStartTime());
        etEnd.setText(slot.getEndTime());
        etAct.setText(slot.getActivity());

        builder.setView(view)
                .setTitle("Chỉnh sửa lịch trình")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newStart = etStart.getText().toString().trim();
                    String newEnd = etEnd.getText().toString().trim();
                    String newAct = etAct.getText().toString().trim();

                    if (newStart.isEmpty() || newEnd.isEmpty()) {
                        Toast.makeText(Layout6Activity.this, "Vui lòng nhập đầy đủ thời gian", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    slot.setStartTime(newStart);
                    slot.setEndTime(newEnd);
                    slot.setActivity(newAct);
                    timeAdapter.notifyItemChanged(position);
                    Toast.makeText(Layout6Activity.this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showApplyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Áp dụng lịch này")
                .setMessage("Bạn muốn:")
                .setPositiveButton("Hiển thị ở màn hình chính", (dialog, which) -> {
                    saveScheduleToMain();
                    saveScheduleToHistory();
                    Toast.makeText(Layout6Activity.this, "Đã áp dụng vào màn hình chính", Toast.LENGTH_SHORT).show();
                    // attempt to go to MainActivity if exists
                    try {
                        Intent it = new Intent(Layout6Activity.this,
                                Class.forName("com.example.to_do_app.activity.MainActivity"));
                        it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(it);
                        finish();
                    } catch (ClassNotFoundException e) {
                        // ignore if MainActivity not present
                    }
                })
                .setNegativeButton("Chỉ lưu vào lịch sử", (dialog, which) -> {
                    saveScheduleToHistory();
                    Toast.makeText(Layout6Activity.this, "Đã lưu vào lịch sử", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Hủy", null)
                .show();
    }

    // placeholder - implement persistence as you prefer (SharedPreferences / Room)
    private void saveScheduleToMain() {
        // TODO: store currentList for selectedDay so MainActivity can load it later
    }

    private void saveScheduleToHistory() {
        // TODO: append current schedule to history DB or file
    }
}