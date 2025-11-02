package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for a schedule item used across the app and stored in Firebase.
 * - Implements Serializable so it can be passed via Intent extras easily.
 * - Provides consistent getters/setters used by adapters and activities:
 *     getStartTime(), getEndTime(), getActivity(), getDayOfWeek()
 * - Adds convenience aliases getTitle()/setTitle() because some UI/code may expect "title".
 * - Adds toMap() helper if you want to write a Map to Firebase.
 */
public class ScheduleItem implements Serializable {
    private int id;
    private String title;       // alias for activity/name — used in some places as "title"
    private String name;        // optional alternate field
    private String time;
    private String startTime;   // Thời gian bắt đầu, ví dụ "06:00"
    private String endTime;     // Thời gian kết thúc, ví dụ "07:00"
    private String activity;    // Mô tả hoạt động
    private int dayOfWeek;      // 2=Thứ 2, 3=Thứ 3, ..., 8=CN

    // Default constructor cần cho Firebase
    public ScheduleItem() { }

    // Full constructor
    public ScheduleItem(int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.title = activity;
        this.dayOfWeek = dayOfWeek;
    }

    // Minimal constructors
    public ScheduleItem(String name) {
        this.name = name;
        this.title = name;
        this.activity = name;
    }

    public ScheduleItem(String startTime, String endTime, String activity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.title = activity;
    }

    // ID
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Title aliases (some UI/code expect getTitle())
    public String getTitle() {
        if (title != null) return title;
        if (activity != null) return activity;
        return name;
    }
    public void setTitle(String title) {
        this.title = title;
        this.activity = title;
        if (this.name == null) this.name = title;
    }

    // Name (alternate)
    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        if (this.title == null) this.title = name;
        if (this.activity == null) this.activity = name;
    }

    // Start Time
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    // End Time
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // Activity description
    public String getActivity() {
        if (activity != null) return activity;
        if (title != null) return title;
        return name;
    }
    public void setActivity(String activity) {
        this.activity = activity;
        this.title = activity;
    }

    // Time (optional)
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    // Day of week
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    /**
     * Helper to convert to a Map for Firebase updates (if needed).
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("name", name);
        m.put("time", time);
        m.put("startTime", startTime);
        m.put("endTime", endTime);
        m.put("activity", activity);
        m.put("dayOfWeek", dayOfWeek);
        return m;
    }

    @Override
    public String toString() {
        return "ScheduleItem{" +
                "id=" + id +
                ", title='" + getTitle() + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", activity='" + getActivity() + '\'' +
                ", dayOfWeek=" + dayOfWeek +
                '}';
    }
}