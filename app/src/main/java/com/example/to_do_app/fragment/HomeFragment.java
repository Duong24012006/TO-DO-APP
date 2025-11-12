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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements ScheduleItemAdapter.OnItemClickListener {

    private LinearLayout llMon, llTue, llWed, llThu, llFri, llSat, llSun;
    private View selectedDay = null;

    // RecyclerView components
    private RecyclerView recyclerView;
    private ScheduleItemAdapter scheduleAdapter;

    // Data map using ScheduleItem model
    private Map<Integer, List<ScheduleItem>> taskMap = new HashMap<>();

    // Firebase and SharedPreferences keys
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

        // Map day layouts
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

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.homerecyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduleAdapter = new ScheduleItemAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(scheduleAdapter);

        // Assign click listeners to days
        for (int i = 0; i < dayLayouts.size(); i++) {
            int dayKey = (i + 2) <= 7 ? i + 2 : 1; // Mon=2, Sun=1
            final LinearLayout dayLayout = dayLayouts.get(i);
            dayLayout.setOnClickListener(v -> selectDay(dayLayout, dayKey)); // Fixed: Changed 'key' to 'dayKey'
        }

        // Select Monday by default
        selectDay(llMon, 2);

        // Attach realtime listener
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

            taskMap.put(day, list);

            // Select the day that was loaded
            LinearLayout targetLayout = getLayoutForDay(day);
            if (targetLayout != null) selectDay(targetLayout, day);

            // Persist locally
            profilePrefs.edit()
                    .putString(HOME_DISPLAY_ACTIVITIES_KEY, json)
                    .putInt(HOME_DISPLAY_DAY_KEY, day)
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
            tasks = new ArrayList<>(); // Use an empty list if no tasks for the day
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
            case 1: return llSun;
            default: return llMon;
        }
    }

    @Override
    public void onItemClick(int position, ScheduleItem item) {
        // Handle item clicks, e.g., show details
        Toast.makeText(getContext(), "Clicked: " + item.getActivity(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditClick(int position, ScheduleItem item) {
        // Handle edit clicks, e.g., open edit screen
        Toast.makeText(getContext(), "Edit: " + item.getActivity(), Toast.LENGTH_SHORT).show();
    }
}
