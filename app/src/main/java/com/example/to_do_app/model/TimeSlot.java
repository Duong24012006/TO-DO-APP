package com.example.to_do_app.model;

import java.io.Serializable;

/**
 * Model cho một khung giờ hoạt động cụ thể trong ngày (thay thế cho ScheduleActivity).
 * Ví dụ: "6:00" - "7:00" - "tập thể dục buổi sáng"
 */
public class TimeSlot implements Serializable {
    private String startTime;    // Ví dụ: "6:00"
    private String endTime;      // Ví dụ: "7:00"
    private String activityName; // Ví dụ: "tập thể dục buổi sáng"

    // Constructor rỗng bắt buộc cho Firebase
    public TimeSlot() {}

    public TimeSlot(String startTime, String endTime, String activityName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.activityName = activityName;
    }

    // --- Getters and Setters ---
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
}
