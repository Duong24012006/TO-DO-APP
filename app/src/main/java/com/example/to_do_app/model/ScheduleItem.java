package com.example.to_do_app.model;

public class ScheduleItem {
    private int id;
    private String time;
    private String startTime; // Thời gian bắt đầu, ví dụ "06:00"
    private String endTime;   // Thời gian kết thúc, ví dụ "07:00"
    private String activity;  // Mô tả hoạt động
    private int dayOfWeek;    // 2=Thứ 2, 3=Thứ 3, ..., 8=CN

    public ScheduleItem(int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.dayOfWeek = dayOfWeek;
    }

    // ID
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Start Time
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    // End Time
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // Activity description
    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    // Day of week
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
