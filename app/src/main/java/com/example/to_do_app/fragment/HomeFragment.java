package com.example.to_do_app.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.to_do_app.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HomeFragment — now reads "home display" from Firebase /users/<userId>/home_display realtime.
 * Falls back to SharedPreferences if Firebase value not present.
 *
 * Behavior:
 * - When Layout6Activity saves schedule for Home, it writes JSON string to /users/<userId>/home_display
 *   and also to SharedPreferences profile_prefs.home_display_activities.
 * - HomeFragment listens for changes and updates UI immediately.
 */
public class HomeFragment extends Fragment {

    private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
    private List<LinearLayout> dayLayouts;
    private View selectedDay = null;

    private TextView[] taskTitles, taskLocations, taskNotes;

    private Map<Integer, List<Task>> taskMap = new HashMap<>();

    private static class Task {
        String time, title, location, note;
        Task(String time, String title, String location, String note) {
            this.time = time; this.title = title; this.location = location; this.note = note;
        }
    }

    // Shared prefs / firebase keys
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
    private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";

    private SharedPreferences profilePrefs;
    private DatabaseReference rootRef;
    private DatabaseReference homeDisplayRef;
    private ChildEventListener dummyListener; // not used but kept for pattern

    private String userId;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
        rootRef = FirebaseDatabase.getInstance().getReference();

        userId = profilePrefs.getString("profile_user_id", null);
        if (userId == null) {
            userId = "user_" + System.currentTimeMillis();
            profilePrefs.edit().putString("profile_user_id", userId).apply();
        }

        // history/home node reference
        homeDisplayRef = rootRef.child("users").child(userId).child("home_display");

        // Ánh xạ LinearLayout các ngày
        llMon = view.findViewById(R.id.llMon);
        llTue = view.findViewById(R.id.llTue);
        llWed = view.findViewById(R.id.llWed);
        llThu = view.findViewById(R.id.llThu);
        llFri = view.findViewById(R.id.llFri);
        llSat = view.findViewById(R.id.llSat);
        llSun = view.findViewById(R.id.llSun);

        dayLayouts = new ArrayList<>();
        dayLayouts.add(llMon); // Thứ 2 = key 2
        dayLayouts.add(llTue); // 3
        dayLayouts.add(llWed); // 4
        dayLayouts.add(llThu); // 5
        dayLayouts.add(llFri); // 6
        dayLayouts.add(llSat); // 7
        dayLayouts.add(llSun); // CN = 1

        // Ánh xạ các TextView nhiệm vụ
        taskTitles = new TextView[]{
                view.findViewById(R.id.tvTaskTitle1),
                view.findViewById(R.id.tvTaskTitle2),
                view.findViewById(R.id.tvTaskTitle3),
                view.findViewById(R.id.tvTaskTitle4),
                view.findViewById(R.id.tvTaskTitle5),
                view.findViewById(R.id.tvTaskTitle6),
                view.findViewById(R.id.tvTaskTitle7)
        };

        taskLocations = new TextView[]{
                view.findViewById(R.id.tvTaskLocation1),
                view.findViewById(R.id.tvTaskLocation2),
                view.findViewById(R.id.tvTaskLocation3),
                view.findViewById(R.id.tvTaskLocation4),
                view.findViewById(R.id.tvTaskLocation5),
                view.findViewById(R.id.tvTaskLocation6),
                view.findViewById(R.id.tvTaskLocation7)
        };

        taskNotes = new TextView[]{
                view.findViewById(R.id.tvTaskNote1),
                view.findViewById(R.id.tvTaskNote2),
                view.findViewById(R.id.tvTaskNote3),
                view.findViewById(R.id.tvTaskNote4),
                view.findViewById(R.id.tvTaskNote5),
                view.findViewById(R.id.tvTaskNote6),
                view.findViewById(R.id.tvTaskNote7)
        };

        // Khởi tạo dữ liệu mẫu (used when no home display saved)
        initSampleTasks();

        // Gán click cho các ngày
        for (int i = 0; i < dayLayouts.size(); i++) {
            int dayKey = (i + 2) <= 7 ? i + 2 : 1; // Thứ 2 = 2, CN = 1
            final LinearLayout dayLayout = dayLayouts.get(i);
            final int key = dayKey;
            dayLayout.setOnClickListener(v -> selectDay(dayLayout, key));
        }

        // Chọn mặc định Thứ 2
        selectDay(llMon, 2);

        // Attach realtime listener to home_display so UI updates immediately
        attachHomeDisplayListener();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // load fallback prefs if firebase value not available
        loadHomeDisplayFromPrefsIfAny();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // detach listener
        if (homeDisplayRef != null && dummyListener != null) {
            homeDisplayRef.removeEventListener(dummyListener);
            dummyListener = null;
        }
    }

    private void attachHomeDisplayListener() {
        if (homeDisplayRef == null) return;

        // Use a ValueEventListener via addValueEventListener to get the entire JSON string/object
        homeDisplayRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object v = snapshot.getValue();
                if (v == null) {
                    // no remote home_display — fallback to prefs
                    loadHomeDisplayFromPrefsIfAny();
                    return;
                }
                String jsonString = String.valueOf(v);
                applyHomeDisplayJson(jsonString);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // On error, fallback to prefs
                loadHomeDisplayFromPrefsIfAny();
            }
        });
    }

    private void loadHomeDisplayFromPrefsIfAny() {
        String json = profilePrefs.getString(HOME_DISPLAY_ACTIVITIES_KEY, null);
        if (json == null) return;
        applyHomeDisplayJson(json);
    }

    /**
     * Parse stored JSON (same format saved by Layout6Activity) and update taskMap + UI.
     * Format:
     * { "day": <int>, "activities":[ { "start":"06:00","end":"08:00","activity":"..." , "day":2}, ... ] }
     */
    private void applyHomeDisplayJson(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(json);
            int day = root.optInt("day", 2);
            JSONArray acts = root.optJSONArray("activities");
            if (acts == null) return;

            List<Task> list = new ArrayList<>();
            for (int i = 0; i < acts.length(); i++) {
                JSONObject o = acts.optJSONObject(i);
                if (o == null) continue;
                String start = o.optString("start", "");
                String end = o.optString("end", "");
                String activity = o.optString("activity", "");
                String time = (start.isEmpty() && end.isEmpty()) ? "" : (start + " - " + end);
                Task t = new Task(time, activity, "", "");
                list.add(t);
            }

            // replace tasks for that day
            taskMap.put(day, list);

            // switch UI to that day
            LinearLayout targetLayout;
            switch (day) {
                case 2: targetLayout = llMon; break;
                case 3: targetLayout = llTue; break;
                case 4: targetLayout = llWed; break;
                case 5: targetLayout = llThu; break;
                case 6: targetLayout = llFri; break;
                case 7: targetLayout = llSat; break;
                case 1: targetLayout = llSun; break;
                default: targetLayout = llMon; break;
            }

            if (targetLayout != null) selectDay(targetLayout, day);

            // also persist locally (keep prefs in sync if remote updated)
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, json)
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
                    .apply();

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void initSampleTasks() {
        taskMap.put(2, List.of(
                new Task("6:00 - 7:00", "Tập thể dục buổi sáng", "Công viên Lê Thị Riêng", "Mang theo nước và khăn tập"),
                new Task("8:00 - 9:00", "Ăn sáng & chuẩn bị đi làm", "Nhà bếp", "Chuẩn bị tài liệu họp sáng"),
                new Task("10:00 - 11:30", "Họp nhóm dự án", "Phòng họp tầng 3", "Chuẩn bị báo cáo sprint"),
                new Task("14:00 - 16:00", "Làm việc tại văn phòng", "Công ty ABC", "Kiểm tra email khách hàng"),
                new Task("17:00 - 19:00", "Tập gym", "Phòng gym California", "Tập ngực và tay"),
                new Task("20:00 - 22:00", "Học lập trình Android", "Tại nhà", "Hoàn thành project app tính toán"),
                new Task("22:00 - 23:00", "Thư giãn và đọc sách", "Phòng ngủ", "Đọc 30 phút sách Android")
        ));

        taskMap.put(3, List.of(
                new Task("6:00 - 7:00", "Đọc sách buổi sáng", "Nhà", "Đọc 20 trang"),
                new Task("8:00 - 9:00", "Ăn sáng & đi làm", "Nhà bếp", "Chuẩn bị tài liệu")
        ));

        taskMap.put(4, List.of(
                new Task("10:00 - 12:00", "Họp khách hàng", "Công ty ABC", "Chuẩn bị hợp đồng")
        ));

        // Các ngày khác trống
        taskMap.put(5, new ArrayList<>());
        taskMap.put(6, new ArrayList<>());
        taskMap.put(7, new ArrayList<>());
        taskMap.put(1, new ArrayList<>());
    }

    private void selectDay(LinearLayout dayLayout, int dayKey) {
        // Reset ngày trước
        if (selectedDay != null) selectedDay.setSelected(false);

        // Chọn ngày hiện tại
        dayLayout.setSelected(true);
        selectedDay = dayLayout;

        // Cập nhật nhiệm vụ
        List<Task> tasks = taskMap.get(dayKey);
        for (int i = 0; i < 7; i++) {
            if (tasks != null && i < tasks.size()) {
                taskTitles[i].setText(tasks.get(i).title);
                taskLocations[i].setText("Địa điểm: " + tasks.get(i).location);
                taskNotes[i].setText("Ghi chú: " + tasks.get(i).note);
            } else {
                taskTitles[i].setText("");
                taskLocations[i].setText("");
                taskNotes[i].setText("");
            }
        }
    }
}