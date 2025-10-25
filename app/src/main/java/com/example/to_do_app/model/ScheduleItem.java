package com.example.to_do_app.model;

public class ScheduleItem {
    private int id;
    private String time;
    private String activity;
    private int dayOfWeek; // 2=Thứ 2, 3=Thứ 3, ..., 8=CN

    public ScheduleItem(int id, String time, String activity, int dayOfWeek) {
        this.id = id;
        this.time = time;
        this.activity = activity;
        this.dayOfWeek = dayOfWeek;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
}