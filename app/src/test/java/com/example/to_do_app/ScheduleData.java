package com.example.to_do_app;

import com.example.to_do_app.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

public class ScheduleData {

    public static List<TimeSlot> getScheduleForDay(int day) {
        List<TimeSlot> list = new ArrayList<>();
        switch (day) {
            case 2:
                list.add(new TimeSlot(1, "06:00", "07:00", "Tập thể dục buổi sáng", 2));
                list.add(new TimeSlot(2, "07:00", "11:00", "Học trên trường", 2));
                list.add(new TimeSlot(3, "11:30", "12:30", "Nghỉ trưa + Ăn trưa", 2));
                break;
            case 3:
                list.add(new TimeSlot(4, "06:00", "07:00", "Tập thể dục buổi sáng", 3));
                list.add(new TimeSlot(5, "07:00", "11:00", "Học trên trường", 3));
                break;
            case 4:
                list.add(new TimeSlot(6, "06:30", "07:15", "Chạy bộ", 4));
                list.add(new TimeSlot(7, "09:00", "12:00", "Học trên trường", 4));
                break;
            case 5:
                list.add(new TimeSlot(8, "06:00", "07:00", "Tập thể dục", 5));
                list.add(new TimeSlot(9, "07:00", "11:00", "Học", 5));
                break;
            case 6:
                list.add(new TimeSlot(10, "08:00", "10:00", "Câu lạc bộ", 6));
                break;
            case 7:
                list.add(new TimeSlot(11, "09:00", "11:00", "Việc nhà", 7));
                break;
            case 8:
                list.add(new TimeSlot(12, "08:00", "10:00", "Nghỉ ngơi", 8));
                break;
            default:
                list.add(new TimeSlot(0, "--:--", "--:--", "Chưa có lịch", day));
                break;
        }
        return list;
    }
}