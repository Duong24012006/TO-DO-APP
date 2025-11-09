package com.example.to_do_app.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp Con: Đại diện cho một lịch trình chi tiết, kế thừa từ ScheduleTemplate.
 * Chứa danh sách các hoạt động cụ thể được phân theo ngày trong tuần.
 */
public class DetailedSchedule extends ScheduleTemplate implements Serializable {

    // Sử dụng TimeSlot thay cho ScheduleActivity
    private Map<String, List<TimeSlot>> weeklyActivities;

    // Constructor rỗng bắt buộc cho Firebase
    public DetailedSchedule() {
        super(); // Gọi constructor của lớp cha
        this.weeklyActivities = new HashMap<>();
    }

    // Sử dụng TimeSlot trong constructor
    public DetailedSchedule(String title, String description, List<String> tags, Map<String, List<TimeSlot>> weeklyActivities) {
        super(title, description, tags); // Gọi constructor của lớp cha để gán title, description, tags
        this.weeklyActivities = weeklyActivities;
    }

    // --- Getter and Setter ---
    public Map<String, List<TimeSlot>> getWeeklyActivities() {
        return weeklyActivities;
    }

    public void setWeeklyActivities(Map<String, List<TimeSlot>> weeklyActivities) {
        this.weeklyActivities = weeklyActivities;
    }
}
