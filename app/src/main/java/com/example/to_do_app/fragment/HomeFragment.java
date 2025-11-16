
    package com.example.to_do_app.fragment;

    import android.content.Context;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.LinearLayout;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.to_do_app.R;
    import com.example.to_do_app.adapters.ScheduleItemAdapter;
    import com.example.to_do_app.model.ScheduleItem;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.Comparator;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    public class HomeFragment extends Fragment implements ScheduleItemAdapter.OnItemClickListener {

        private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
        private View selectedDay = null;

        private RecyclerView recyclerView;
        private ScheduleItemAdapter scheduleAdapter;

        private Map<Integer, List<ScheduleItem>> taskMap = new HashMap<>();

        private static final String PROFILE_PREFS = "profile_prefs";
        private static final String HOME_DISPLAY_ACTIVITIES_KEY = "home_display_activities";
        private static final String HOME_DISPLAY_DAY_KEY = "home_display_day";

        private SharedPreferences profilePrefs;
        private DatabaseReference homeDisplayRef;
        private ValueEventListener homeDisplayListener;

        private String userId;

        public HomeFragment() { }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.home_fragment, container, false);

            profilePrefs = requireContext().getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

            userId = profilePrefs.getString("profile_user_id", null);
            if (userId == null) {
                userId = "user_" + System.currentTimeMillis();
                profilePrefs.edit().putString("profile_user_id", userId).apply();
            }

            homeDisplayRef = rootRef.child("users").child(userId).child("home_display");

            llMon = view.findViewById(R.id.llMon);
            llTue = view.findViewById(R.id.llTue);
            llWed = view.findViewById(R.id.llWed);
            llThu = view.findViewById(R.id.llThu);
            llFri = view.findViewById(R.id.llFri);
            llSat = view.findViewById(R.id.llSat);
            llSun = view.findViewById(R.id.llSun);

            List<LinearLayout> dayLayouts = new ArrayList<>();
            dayLayouts.add(llMon); dayLayouts.add(llTue); dayLayouts.add(llWed);
            dayLayouts.add(llThu); dayLayouts.add(llFri); dayLayouts.add(llSat); dayLayouts.add(llSun);

            recyclerView = view.findViewById(R.id.homerecyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            // MODIFIED: Use the new constructor to hide the edit button
            scheduleAdapter = new ScheduleItemAdapter(getContext(), new ArrayList<>(), this, false);
            recyclerView.setAdapter(scheduleAdapter);

            for (int i = 0; i < dayLayouts.size(); i++) {
                int dayKey = (i + 2) <= 7 ? i + 2 : 8;  // Chủ Nhật = 8
                final LinearLayout dayLayout = dayLayouts.get(i);
                dayLayout.setOnClickListener(v -> selectDay(dayLayout, dayKey));
            }

            selectDay(llMon, 2);
            attachHomeDisplayListener();

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            loadHomeDisplayFromPrefsIfAny();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (homeDisplayRef != null && homeDisplayListener != null) {
                homeDisplayRef.removeEventListener(homeDisplayListener);
            }
        }

        private void attachHomeDisplayListener() {
            if (homeDisplayRef == null) return;

            homeDisplayListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        loadHomeDisplayFromPrefsIfAny();
                        return;
                    }
                    String jsonString = String.valueOf(snapshot.getValue());
                    applyHomeDisplayJson(jsonString);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadHomeDisplayFromPrefsIfAny();
                }
            };
            homeDisplayRef.addValueEventListener(homeDisplayListener);
        }

        private void loadHomeDisplayFromPrefsIfAny() {
            String json = profilePrefs.getString(HOME_DISPLAY_ACTIVITIES_KEY, null);
            if (json != null) {
                applyHomeDisplayJson(json);
            }
        }

        private void applyHomeDisplayJson(String json) {
            if (json == null || json.isEmpty()) return;
            try {
                JSONObject root = new JSONObject(json);

                // Kiểm tra xem có phải dữ liệu mới (toàn bộ tuần) hay dữ liệu cũ (một ngày)
                if (root.has("day_2") || root.has("day_3") || root.has("day_4") ||
                    root.has("day_5") || root.has("day_6") || root.has("day_7") || root.has("day_8")) {
                    // Dữ liệu mới - toàn bộ tuần
                    taskMap.clear();

                    for (int day = 2; day <= 8; day++) {
                        String dayKey = "day_" + day;
                        JSONArray acts = root.optJSONArray(dayKey);

                        if (acts != null) {
                            List<ScheduleItem> list = new ArrayList<>();
                            for (int i = 0; i < acts.length(); i++) {
                                JSONObject o = acts.optJSONObject(i);
                                if (o == null) continue;
                                ScheduleItem item = new ScheduleItem();
                                item.setStartTime(o.optString("start", ""));
                                item.setEndTime(o.optString("end", ""));
                                item.setActivity(o.optString("activity", ""));
                                item.setDayOfWeek(o.optInt("day", day));
                                list.add(item);
                            }
                            sortListByStartTime(list);
                            taskMap.put(day, list);
                        }
                    }

                    // Hiển thị ngày đã lưu hoặc mặc định Thứ 2
                    int savedDay = profilePrefs.getInt(HOME_DISPLAY_DAY_KEY, 2);
                    LinearLayout targetLayout = getLayoutForDay(savedDay);
                    if (targetLayout != null) selectDay(targetLayout, savedDay);

                } else {
                    // Dữ liệu cũ - chỉ một ngày (để tương thích ngược)
                    int day = root.optInt("day", 2);
                    JSONArray acts = root.optJSONArray("activities");
                    if (acts == null) return;

                    List<ScheduleItem> list = new ArrayList<>();
                    for (int i = 0; i < acts.length(); i++) {
                        JSONObject o = acts.optJSONObject(i);
                        if (o == null) continue;
                        ScheduleItem item = new ScheduleItem();
                        item.setStartTime(o.optString("start", ""));
                        item.setEndTime(o.optString("end", ""));
                        item.setActivity(o.optString("activity", ""));
                        list.add(item);
                    }

                    sortListByStartTime(list);
                    taskMap.put(day, list);

                    LinearLayout targetLayout = getLayoutForDay(day);
                    if (targetLayout != null) selectDay(targetLayout, day);
                }

                profilePrefs.edit()
                        .putString(HOME_DISPLAY_ACTIVITIES_KEY, json)
                        .apply();

            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        private void selectDay(LinearLayout dayLayout, int dayKey) {
            if (selectedDay != null) {
                selectedDay.setSelected(false);
            }
            dayLayout.setSelected(true);
            selectedDay = dayLayout;

            List<ScheduleItem> tasks = taskMap.get(dayKey);
            if (tasks == null) {
                tasks = new ArrayList<>();
            } else {
                // ensure displayed list is sorted (defensive)
                sortListByStartTime(tasks);
            }
            scheduleAdapter.updateList(tasks);
        }

        private LinearLayout getLayoutForDay(int day) {
            switch (day) {
                case 2: return llMon;
                case 3: return llTue;
                case 4: return llWed;
                case 5: return llThu;
                case 6: return llFri;
                case 7: return llSat;
                case 8: return llSun;  // Chủ Nhật = 8
                default: return llMon;
            }
        }

        @Override
        public void onItemClick(int position, ScheduleItem item) {
            // In HomeFragment, clicks might not do anything, or could show a read-only detail view.
            // For now, a simple Toast is fine.
            Toast.makeText(getContext(), "Công việc: " + item.getActivity(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEditClick(int position, ScheduleItem item) {
            // This will not be called in HomeFragment as the button is hidden.
        }

        // --- Helpers: sorting by start time (HH:mm). Empty or invalid times are pushed to the end. ---
        private void sortListByStartTime(List<ScheduleItem> list) {
            if (list == null || list.size() <= 1) return;
            Collections.sort(list, new Comparator<ScheduleItem>() {
                @Override
                public int compare(ScheduleItem a, ScheduleItem b) {
                    int aStart = parseTimeToMinutes(a.getStartTime());
                    int bStart = parseTimeToMinutes(b.getStartTime());
                    if (aStart != bStart) return Integer.compare(aStart, bStart);

                    // tie-breaker: compare end times
                    int aEnd = parseTimeToMinutes(a.getEndTime());
                    int bEnd = parseTimeToMinutes(b.getEndTime());
                    return Integer.compare(aEnd, bEnd);
                }
            });
        }

        private int parseTimeToMinutes(String time) {
            if (time == null) return Integer.MAX_VALUE;
            String t = time.trim();
            if (t.isEmpty()) return Integer.MAX_VALUE;
            // Expected format "HH:mm" or "H:mm". If invalid, push to end.
            try {
                String[] parts = t.split(":");
                if (parts.length < 1) return Integer.MAX_VALUE;
                int hh = Integer.parseInt(parts[0]);
                int mm = 0;
                if (parts.length >= 2 && parts[1].length() > 0) {
                    mm = Integer.parseInt(parts[1]);
                }
                // Normalize hours; if hour >=24 treat as large
                if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return Integer.MAX_VALUE;
                return hh * 60 + mm;
            } catch (Exception ex) {
                return Integer.MAX_VALUE;
            }
        }
    }