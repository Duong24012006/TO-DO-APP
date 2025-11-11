package com.example.to_do_app.data;

import com.example.to_do_app.model.DetailedSchedule;
import com.example.to_do_app.model.ScheduleTemplate;
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

        // Các template mẫu chi tiết khác
        templates.add(createNightOwlTemplate());           // 1. CÚ ĐÊM
        templates.add(createMorningPersonTemplate());      // 2. Chuyên buổi sáng
        templates.add(createDeepWorkTemplate());           // 3. Học môn chuyên sâu
        templates.add(createWorkHardPlayHardTemplate());   // 4. Sáng học, tối chơi
        templates.add(createSprintWeekTemplate());         // 5. Tối ưu hóa chu kỳ ngủ
        templates.add(createIntermittentFastingTemplate());// 6. Vừa học vừa ăn gián đoạn 16-8
        templates.add(createWorkAndLearnTemplate());       // 7. Vừa học vừa làm
        templates.add(createScientificProductivityTemplate()); // 8. Năng suất theo khoa học
        templates.add(createUncleHoLifestyleTemplate());   // 9. Lịch sinh hoạt của Bác Hồ

        return templates;
    }
    // --- Các phương thức tạo từng Template chi tiết ---
    // 1. Template "CÚ ĐÊM"
    public static DetailedSchedule createNightOwlTemplate() {
        String title = "CÚ ĐÊM";
        String description = "Template này tối đa hóa thời gian thức để học, phù hợp với sinh viên muốn tăng cường học tập.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung cho Thứ 2 -> Thứ 7 ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "08:30", "Thức dậy & Vệ sinh cá nhân"),
                new TimeSlot("08:30", "09:30", "Học nhẹ (Đọc sách, ôn lý thuyết đơn giản, làm bài tập dễ)"),
                new TimeSlot("09:30", "11:30", "Hoàn thành việc khác (việc nhà, chuẩn bị đồ dùng, học trên trường)"),
                new TimeSlot("11:30", "12:30", "Ăn trưa & Chuẩn bị đi học"),
                new TimeSlot("12:30", "15:00", "Ngủ trưa & Ăn trưa"),
                new TimeSlot("15:10", "17:40", "Làm một món vừa sức, cơ bản"),
                new TimeSlot("17:40", "19:00", "Nghỉ ngơi & Ăn Tối"),
                new TimeSlot("19:00", "20:00", "Vận động thể chất (60 phút)"),
                new TimeSlot("20:00", "22:00", "Học môn cần tư duy cao, các môn thuộc khối tự nhiên"),
                new TimeSlot("22:00", "23:00", "Giải lao + Vận động nhẹ (đi lại, nghe nhạc,...)"),
                new TimeSlot("23:00", "01:00", "Ôn tập các môn cần ghi nhớ (Tiếng anh từ vựng, khối xã hội)"),
                new TimeSlot("01:00", "02:00", "Tổng hợp kiến thức trong ngày + làm đề luyện tập"),
                new TimeSlot("02:00", "", "Đi ngủ")
        ));

        // Gán lịch trình này cho các ngày từ Thứ 2 đến Thứ 7
        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);
        weeklyActivities.put("Thứ 7", weekdaySchedule);

        // --- Lịch trình riêng cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "08:30", "Thức dậy & Vệ sinh cá nhân"),
                new TimeSlot("08:30", "09:30", "Ăn sáng & Thể thao nhẹ"),
                new TimeSlot("09:30", "11:30", "Học Sâu: Dự án cá nhân"),
                new TimeSlot("11:30", "12:30", "Ăn trưa & Giải trí"),
                new TimeSlot("12:30", "15:00", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("15:10", "17:40", "Học Tiếng Anh linh hoạt"),
                new TimeSlot("17:40", "19:00", "Nghỉ ngơi & Ăn Tối"),
                new TimeSlot("19:00", "20:00", "Vận động thể chất (60 phút)"),
                new TimeSlot("20:00", "22:00", "Giải trí (Gặp gỡ bạn bè)"),
                new TimeSlot("22:00", "23:00", "Giải lao + Vận động nhẹ (đi lại, nghe nhạc,...)"),
                new TimeSlot("23:00", "01:00", "Ôn tập kiến thức"),
                new TimeSlot("01:00", "02:00", "Tổng hợp kiến thức & Chuẩn bị ngày mai"),
                new TimeSlot("02:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 2. Template "Chuyên buổi sáng"
    public static DetailedSchedule createMorningPersonTemplate() {
        String title = "Chuyên buổi sáng";
        String description = "Dành cho người có năng lượng và tập trung cao nhất vào buổi sáng.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "30m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung cho Thứ 2 -> Thứ 5 ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "06:00", "Thức dậy sớm, Vận động nhẹ"),
                new TimeSlot("06:00", "08:00", "Học môn cần tập trung cao (Môn khó nhất)"),
                new TimeSlot("08:00", "09:00", "Học nhẹ/Ôn bài cũ (môn cần học thuộc lòng)"),
                new TimeSlot("09:10", "11:00", "Tự học/Nghiên cứu"),
                new TimeSlot("11:00", "13:30", "Ăn trưa, Nghỉ ngơi, Ngủ trưa (20-30 phút)"),
                new TimeSlot("13:30", "15:00", "Học môn vừa sức, làm bài tập đơn giản"),
                new TimeSlot("15:10", "17:40", "Học nhóm/Học thêm"),
                new TimeSlot("17:40", "19:30", "Chuẩn bị bữa tối & Ăn tối"),
                new TimeSlot("19:30", "21:00", "Ôn bài nhẹ nhàng (Đọc lại kiến thức, làm 1-2 bài tập nhỏ)"),
                new TimeSlot("21:00", "22:00", "Giải trí/Sở thích (60 phút)"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);

        // --- Lịch trình riêng cho Thứ 6 ---
        List<TimeSlot> fridaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "06:00", "Thức dậy sớm, Vận động nhẹ"),
                new TimeSlot("06:00", "08:00", "Học môn cần tập trung cao (Môn khó nhất)"),
                new TimeSlot("08:00", "09:00", "Học nhẹ/Ôn bài cũ (môn cần học thuộc lòng)"),
                new TimeSlot("09:10", "11:00", "Tự học/Nghiên cứu"),
                new TimeSlot("11:00", "13:30", "Ăn trưa, Nghỉ ngơi, Ngủ trưa (20-30 phút)"),
                new TimeSlot("13:30", "15:00", "Học môn vừa sức, làm bài tập đơn giản"),
                new TimeSlot("15:10", "17:40", "Học nhóm/Học thêm"),
                new TimeSlot("17:40", "19:30", "Chuẩn bị bữa tối & Ăn tối"),
                new TimeSlot("19:30", "21:00", "Ôn bài nhẹ nhàng"),
                new TimeSlot("21:00", "22:00", "Giải trí/Sở thích"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 6", fridaySchedule);

        // --- Lịch trình riêng cho Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "06:00", "Thức dậy sớm, Vận động nhẹ"),
                new TimeSlot("06:00", "08:00", "Học môn cần tập trung cao (Môn khó nhất)"),
                new TimeSlot("08:00", "09:00", "Học nhẹ/Ôn bài cũ (môn cần học thuộc lòng)"),
                new TimeSlot("09:10", "11:00", "Tự học/Nghiên cứu"),
                new TimeSlot("11:00", "13:30", "Ăn trưa, Nghỉ ngơi, Ngủ trưa (20-30 phút)"),
                new TimeSlot("13:30", "15:00", "Học môn vừa sức, làm bài tập đơn giản"),
                new TimeSlot("15:10", "17:40", "Học nhóm/Học thêm"),
                new TimeSlot("17:40", "19:30", "Chuẩn bị bữa tối & Ăn tối"),
                new TimeSlot("19:30", "21:00", "Ôn bài nhẹ nhàng"),
                new TimeSlot("21:00", "22:00", "Giải trí/Sở thích"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình riêng cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "06:00", "Thức dậy sớm, Vận động nhẹ"),
                new TimeSlot("06:00", "08:00", "Thể thao 60p (Tập gym/chạy bộ)"),
                new TimeSlot("08:00", "09:00", "Giải trí/Thư giãn"),
                new TimeSlot("09:10", "11:00", "Học sâu: Dự án/kỹ năng"),
                new TimeSlot("11:00", "13:30", "Ăn trưa, Nghỉ ngơi"),
                new TimeSlot("13:30", "15:00", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("15:10", "17:40", "Hoạt động xã hội/Thư giãn"),
                new TimeSlot("17:40", "19:30", "Chuẩn bị bữa tối & Ăn tối"),
                new TimeSlot("19:30", "21:00", "Giải trí/Thư giãn"),
                new TimeSlot("21:00", "22:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 3. Template "Học môn chuyên sâu"
    public static DetailedSchedule createDeepWorkTemplate() {
        String title = "Học môn chuyên sâu";
        String description = "Ưu tiên tối đa hóa thời gian học cho một \"Môn Chính\" bằng cách dành phần lớn thời gian tự học cho nó.";
        List<String> tags = Arrays.asList("8h_sleep", "8h_study", "30m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // Common deep-work blocks used across multiple days
        List<TimeSlot> deepMorning = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Thức dậy, Ăn sáng nhẹ"),
                new TimeSlot("07:00", "09:00", "Deep work: Môn chính"),
                new TimeSlot("09:00", "09:30", "Giải lao & vận động nhẹ"),
                new TimeSlot("09:30", "11:30", "Deep work: Môn chính (tiếp tục)")
        ));

        List<TimeSlot> midDay = new ArrayList<>(Arrays.asList(
                new TimeSlot("11:30", "13:30", "Ăn trưa & nghỉ ngơi"),
                new TimeSlot("13:30", "15:30", "Học môn phụ / Làm bài tập"),
                new TimeSlot("15:30", "17:30", "Luyện đề / Thực hành")
        ));

        List<TimeSlot> evening = new ArrayList<>(Arrays.asList(
                new TimeSlot("17:30", "19:00", "Ăn tối & Thư giãn"),
                new TimeSlot("19:00", "21:00", "Deep review / Ôn tập"),
                new TimeSlot("21:00", "22:30", "Ghi chú & Lập kế hoạch học ngày mai"),
                new TimeSlot("22:30", "", "Đi ngủ")
        ));

        // Assign schedules to specific weekdays (2..8)
        weeklyActivities.put("Thứ 2", concatenate(deepMorning, midDay, evening));
        weeklyActivities.put("Thứ 3", concatenate(deepMorning, midDay, evening));
        weeklyActivities.put("Thứ 4", concatenate(deepMorning, midDay, evening));
        weeklyActivities.put("Thứ 5", concatenate(deepMorning, midDay, evening));
        weeklyActivities.put("Thứ 6", concatenate(deepMorning, midDay, evening));
        weeklyActivities.put("Thứ 7", concatenate(deepMorning, midDay, evening));

        // Chủ nhật: giảm cường độ, tập trung vào review và lập kế hoạch
        weeklyActivities.put("Chủ Nhật", new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "10:00", "Ôn tập nhẹ / Xử lý lỗi"),
                new TimeSlot("10:00", "12:00", "Dự án cá nhân"),
                new TimeSlot("12:00", "14:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("14:00", "16:00", "Lập kế hoạch Tuần"),
                new TimeSlot("16:00", "18:00", "Giải trí nhẹ"),
                new TimeSlot("18:00", "20:00", "Chuẩn bị tuần mới"),
                new TimeSlot("20:00", "22:00", "Đọc & ôn lại tài liệu"),
                new TimeSlot("22:00", "", "Đi ngủ")
        )));

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    private static List<TimeSlot> concatenate(List<TimeSlot> a, List<TimeSlot> b, List<TimeSlot> c) {
        List<TimeSlot> res = new ArrayList<>();
        if (a != null) res.addAll(a);
        if (b != null) res.addAll(b);
        if (c != null) res.addAll(c);
        return res;
    }

    // 4. Template "Sáng học, tối chơi"
    public static DetailedSchedule createWorkHardPlayHardTemplate() {
        String title = "Sáng học, tối chơi";
        String description = "Template này lý tưởng cho sinh viên muốn \"học ra học, chơi ra chơi\". Dành cho người dậy sớm, ưu tiên học buổi sáng và thể thao buổi tối.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "90m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình cho Thứ 2 ---
        List<TimeSlot> mondaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ (Thiền, Yoga)"),
                new TimeSlot("06:00", "07:00", "Học tập trung cao (Môn khó nhất)"),
                new TimeSlot("07:00", "08:00", "Ăn sáng & Ôn bài nhẹ (Học thuộc)"),
                new TimeSlot("08:00", "11:30", "Tự học (Thư viện)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa, Nghỉ ngơi, Ngủ trưa (20-30 phút)"),
                new TimeSlot("13:30", "15:30", "Tự học (Môn vừa sức) & Ôn bài"),
                new TimeSlot("15:30", "17:30", "Tự học (Ôn bài trong ngày)"),
                new TimeSlot("17:30", "19:00", "THỂ THAO CƯỜNG ĐỘ CAO (90p - Gym/Chạy bộ/Bóng đá)"),
                new TimeSlot("19:00", "20:30", "Ăn tối muộn & Tắm rửa, thư giãn"),
                new TimeSlot("20:30", "21:30", "Giải trí/Sở thích (60 phút - Đọc sách, nghe nhạc)"),
                new TimeSlot("21:30", "22:00", "Review nhanh ngày mai & Chuẩn bị đi ngủ"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", mondaySchedule);

        // --- Lịch trình chung cho Thứ 3 -> Thứ 7 ---
        List<TimeSlot> commonSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("06:00", "07:00", "Học tập trung cao"),
                new TimeSlot("07:00", "08:00", "Ăn sáng & Ôn bài nhẹ"),
                new TimeSlot("08:00", "11:30", "Tự học (Thư viện)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa, Nghỉ ngơi, Ngủ trưa"),
                new TimeSlot("13:30", "15:30", "Tự học (Môn vừa sức) & Ôn bài"),
                new TimeSlot("15:30", "17:30", "Tự học (Ôn bài trong ngày)"),
                new TimeSlot("17:30", "19:00", "THỂ THAO CƯỜNG ĐỘ CAO (90p)"),
                new TimeSlot("19:00", "20:30", "Ăn tối muộn & Tắm rửa, thư giãn"),
                new TimeSlot("20:30", "21:30", "Giải trí/Sở thích (60 phút)"),
                new TimeSlot("21:30", "22:00", "Review nhanh ngày mai & Chuẩn bị đi ngủ"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 3", commonSchedule);
        weeklyActivities.put("Thứ 4", commonSchedule);
        weeklyActivities.put("Thứ 5", commonSchedule);
        weeklyActivities.put("Thứ 6", commonSchedule);
        weeklyActivities.put("Thứ 7", commonSchedule);

        // --- Lịch trình riêng cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("06:00", "07:00", "Học tập trung cao"),
                new TimeSlot("07:00", "08:00", "Ăn sáng & Lên kế hoạch tuần"),
                new TimeSlot("08:00", "11:30", "Hoạt động cá nhân/Gia đình"),
                new TimeSlot("11:30", "13:30", "Ăn trưa, Nghỉ ngơi"),
                new TimeSlot("13:30", "15:30", "Tự học/Hoạt động xã hội"),
                new TimeSlot("15:30", "17:30", "Giải trí/Hoạt động xã hội"),
                new TimeSlot("17:30", "19:00", "THỂ THAO CƯỜNG ĐỘ CAO (90p)"),
                new TimeSlot("19:00", "20:30", "Ăn tối muộn & Tắm rửa, thư giãn"),
                new TimeSlot("20:30", "21:30", "Giải trí/Sở thích (60 phút)"),
                new TimeSlot("21:30", "22:00", "Review nhanh ngày mai & Chuẩn bị đi ngủ"),
                new TimeSlot("22:00", "", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 5. Template "Tối ưu hóa chu kỳ ngủ" - Tên lịch gốc có vẻ không khớp mô tả, tạm dùng tên này
    public static DetailedSchedule createSprintWeekTemplate() {
        String title = "Tối ưu hóa chu kỳ ngủ";
        String description = "Template này dành cho sinh viên cần tối đa hóa thời gian thức trong tuần cho các dự án khẩn cấp hoặc ôn thi nước rút, và chấp nhận dùng cuối tuần để \"ngủ bù\" phục hồi.";
        List<String> tags = Arrays.asList("4h_sleep", "8h_study", "30m_sport", "30m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 6. Template "Vừa học vừa ăn gián đoạn 16-8"
    public static DetailedSchedule createIntermittentFastingTemplate() {
        String title = "Vừa học vừa ăn gián đoạn 16-8";
        String description = "Giúp kết hợp phương pháp nhịn ăn gián đoạn 16-8, tối ưu hóa sự tập trung buổi sáng và đảm bảo phục hồi đầy đủ mà vẫn có thời gian cho thể thao.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 7. Template "Vừa học vừa làm"
    public static DetailedSchedule createWorkAndLearnTemplate() {
        String title = "Vừa học vừa làm";
        String description = "Template này dành cho sinh viên có quỹ thời gian hạn chế, muốn \"học ra học, làm ra làm\", tối ưu hóa 2 giờ học và dành thời gian cho các hoạt động ngoại khóa, làm thêm và kỹ năng.";
        List<String> tags = Arrays.asList("8h_sleep", "4h_study", "30m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 8. Template "Năng suất theo khoa học"
    public static DetailedSchedule createScientificProductivityTemplate() {
        String title = "Năng suất theo khoa học";
        String description = "Template này tối ưu hóa 10 giờ học tập năng suất mỗi ngày (kết hợp Tự học + Học trên trường) và cân bằng với thể thao, sở thích cá nhân.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 9. Template "Lịch sinh hoạt của Bác Hồ"
    public static DetailedSchedule createUncleHoLifestyleTemplate() {
        String title = "Lịch sinh hoạt của Bác Hồ";
        String description = "Dành cho những sinh viên muốn rèn luyện một lối sống kỷ luật, tập trung cao độ vào công việc và học tập, đồng thời duy trì sức khỏe và sự phát triển cá nhân một cách bền bỉ.";
        List<String> tags = Arrays.asList("8h_sleep", "8h_study", "30m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>(); // Thêm chi tiết nếu cần
        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }
}