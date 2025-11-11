package com.example.to_do_app.data;

import com.example.to_do_app.model.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility provider for default ScheduleItem lists per day.
 * This is the extracted version of getDefaultItemsForDay(...) so it can be reused by other parts of the app.
 *
 * Day numbering convention:
 *  - 2..7 => Thứ 2 .. Thứ 7
 *  - 8 (or 1) => Chủ nhật (use whichever your app expects)
 */
public final class ScheduleDefaults {

    private ScheduleDefaults() { /* no instances */ }

    public static List<ScheduleItem> getDefaultItemsForDay(int day) {
        List<ScheduleItem> defaults = new ArrayList<>();
        switch (day) {
            case 2:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Thức dậy & vệ sinh", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & Chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Làm việc / Học buổi sáng", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Tiếp tục công việc", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Ôn tập / Học thêm", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Thư giãn / Gia đình", day));
                break;
            case 3:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Yoga & Sáng", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Họp nhóm / Công việc", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Công việc chuyên môn", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Task cá nhân", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Thư giãn", day));
                break;
            case 4:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Chạy bộ & vệ sinh", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Học / Khóa học", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Side project", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Gym / Thể dục", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Lập kế hoạch tuần", day));
                break;
            case 5:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Thiền & Chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & công việc nhẹ", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Deep work", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa & nghỉ", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Chạy việc / Mua sắm", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Học thêm", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Giải trí", day));
                break;
            case 6:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Đi bộ & Sáng", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng & đọc tin", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Hoàn thiện công việc", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Kiểm tra email", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Gặp gỡ bạn bè", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Thư giãn", day));
                break;
            case 7:
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Dọn dẹp & Chuẩn bị", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Mua sắm / Công việc gia đình", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Nấu ăn & ăn trưa", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Hobby / Sở thích", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Thể dục", day));
                defaults.add(new ScheduleItem(0, "18:00", "20:00", "Gặp gỡ", day));
                defaults.add(new ScheduleItem(0, "20:00", "22:00", "Chuẩn bị cho CN", day));
                break;
            case 8:
            case 1: // accept 1 as synonym for Sunday if caller uses 1
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ngủ nướng & ăn sáng", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Thư giãn (sách, cafe)", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa gia đình", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Lập kế hoạch tuần", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Đi dạo", day));
                defaults.add(new ScheduleItem(0, "18:00", "20:00", "Chuẩn bị thức ăn", day));
                defaults.add(new ScheduleItem(0, "20:00", "22:00", "Xem phim / Thư giãn", day));
                break;
            default:
                defaults.add(new ScheduleItem(0, "06:00", "08:00", "Thức dậy & vệ sinh", day));
                defaults.add(new ScheduleItem(0, "08:00", "10:00", "Ăn sáng", day));
                defaults.add(new ScheduleItem(0, "10:00", "12:00", "Làm việc", day));
                defaults.add(new ScheduleItem(0, "12:00", "14:00", "Ăn trưa", day));
                defaults.add(new ScheduleItem(0, "14:00", "16:00", "Tiếp tục", day));
                defaults.add(new ScheduleItem(0, "16:00", "18:00", "Thư giãn", day));
                defaults.add(new ScheduleItem(0, "19:00", "21:00", "Gia đình", day));
                break;
        }
        return defaults;
    }
}