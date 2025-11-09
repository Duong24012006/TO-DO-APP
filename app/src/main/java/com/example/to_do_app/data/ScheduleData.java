package com.example.to_do_app.data;

import com.example.to_do_app.model.DetailedSchedule;
import com.example.to_do_app.model.ScheduleTemplate; // Cần import lớp cha
import com.example.to_do_app.model.TimeSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleData {

    /**
     * Phương thức chính để AddFragment gọi.
     * Nó sẽ tập hợp tất cả các template mẫu lại với nhau.
     */
    public static List<ScheduleTemplate> getSampleTemplates() {
        List<ScheduleTemplate> templates = new ArrayList<>();

        // Giờ đây, thay vì hardcode, chúng ta chỉ cần gọi các phương thức tạo template chi tiết.
        // Vì DetailedSchedule là con của ScheduleTemplate, ta có thể thêm nó trực tiếp vào danh sách.
        templates.add(createStudentTemplate());
        templates.add(createSportsTemplate());
        templates.add(createWeekendTemplate());
        templates.add(createHealthyEatingTemplate());
        templates.add(createDeepSleepTemplate());
        templates.add(createNightOwlTemplate()); // Template "CÚ ĐÊM" bạn đã có

        return templates;
    }

    // --- Các phương thức tạo từng Template chi tiết ---

    // 1. Template "CÚ ĐÊM" (Bạn đã có)
    public static DetailedSchedule createNightOwlTemplate() {
        // 1. Khai báo thông tin cơ bản cho Template (Lớp cha)
        String title = "CÚ ĐÊM";
        String description = "Template này tối đa hóa thời gian thức để học, phù hợp với sinh viên muốn tăng cường học tập.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "60m_sport", "60m_relax");

        // 2. Tạo Map để chứa lịch trình chi tiết cho cả tuần
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình cho Thứ 2 -> Thứ 6 (Giống nhau) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("06:00", "06:30", "Ăn sáng"),
                new TimeSlot("06:30", "10:30", "Học tập/Làm việc"),
                new TimeSlot("10:30", "11:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("11:30", "17:30", "Học tập/Làm việc"),
                new TimeSlot("17:30", "18:30", "Ăn tối & Dọn dẹp"),
                new TimeSlot("18:30", "21:30", "Học ngoại ngữ / Làm việc khác"),
                new TimeSlot("21:30", "23:00", "Đọc sách/Sở thích cá nhân"),
                new TimeSlot("23:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // (Code chi tiết cho Thứ 7 và Chủ Nhật giữ nguyên như file của bạn...)
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                // ... thêm các TimeSlot của Thứ 7
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                // ... thêm các TimeSlot của Chủ Nhật
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        // 4. Tạo và trả về đối tượng DetailedSchedule
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 2. Template "Lịch học cho sinh viên"
    public static DetailedSchedule createStudentTemplate() {
        String title = "Lịch học cho sinh viên";
        String description = "Template này dành cho sinh viên muốn tối đa hóa thời gian học tập nhưng với mức ngủ tối thiểu";
        List<String> tags = Arrays.asList("#HocTap", "#SinhVien", "#4h_study");
        // Ở đây, bạn có thể định nghĩa các TimeSlot chi tiết cho template này, hoặc để rỗng nếu chỉ cần thông tin cơ bản
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 3. Template "Lịch trình thể thao"
    public static DetailedSchedule createSportsTemplate() {
        String title = "Lịch trình thể thao";
        String description = "Tối ưu hóa thời gian tập luyện và phục hồi để đạt hiệu suất cao nhất trong thể thao.";
        List<String> tags = Arrays.asList("#TheThao", "#CoHoi", "#60m_sport");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 4. Template "Lịch trình giải trí cuối tuần"
    public static DetailedSchedule createWeekendTemplate() {
        String title = "Lịch trình giải trí cuối tuần";
        String description = "Dành thời gian để thư giãn, giải trí và nạp lại năng lượng sau một tuần làm việc căng thẳng.";
        List<String> tags = Arrays.asList("#GiaiTri", "#CuoiTuan", "#60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 5. Template "Lịch trình ăn uống lành mạnh"
    public static DetailedSchedule createHealthyEatingTemplate() {
        String title = "Lịch trình ăn uống lành mạnh";
        String description = "Thiết lập một chế độ ăn uống cân bằng và khoa học để cải thiện sức khỏe và vóc dáng.";
        List<String> tags = Arrays.asList("#AnUong", "#SucKhoe", "#SinhVien");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 6. Template "Lịch ngủ sâu"
    public static DetailedSchedule createDeepSleepTemplate() {
        String title = "Lịch ngủ sâu";
        String description = "Tập trung vào chất lượng giấc ngủ để cải thiện năng lượng ngày tiếp theo.";
        List<String> tags = Arrays.asList("#GioNgu", "#8h_sleep");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }
}
