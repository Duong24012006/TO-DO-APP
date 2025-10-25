package com.example.to_do_app.model;

/**
 * Model cho 1 khung giờ
 */
public class TimeSlot {
    private int id;
    private String startTime;
    private String endTime;
    private String activity;
    private int dayOfWeek; // 2..8 (2=Mon, 8=Sun)

    public TimeSlot(int id, String startTime, String endTime, String activity, int dayOfWeek) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.dayOfWeek = dayOfWeek;
    }

    public int getId() { return id; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}