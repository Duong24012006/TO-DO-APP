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
        templates.add(createNightOwlTemplate());           // 1. CÚ ĐÊM
        templates.add(createMorningPersonTemplate());    // 2. Chuyên buổi sáng
        templates.add(createDeepWorkTemplate());           // 3. Học môn chuyên sâu
        templates.add(createWorkHardPlayHardTemplate()); // 4. Sáng học, tối chơi
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

      // Thứ 2 - Tập trung ôn lý thuyết và làm bài
      List<TimeSlot> monday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:00", "08:30", "Thức dậy & Vệ sinh cá nhân"),
              new TimeSlot("08:30", "10:00", "Ôn lý thuyết cơ bản"),
              new TimeSlot("10:15", "12:00", "Làm bài tập môn A"),
              new TimeSlot("12:00", "13:00", "Ăn trưa & nghỉ"),
              new TimeSlot("13:30", "16:00", "Học lab / thực hành"),
              new TimeSlot("17:00", "18:30", "Nghỉ ngơi & ăn tối"),
              new TimeSlot("19:00", "22:00", "Học môn cần tư duy cao"),
              new TimeSlot("22:30", "00:00", "Ôn bài nhẹ"),
              new TimeSlot("00:30", "02:00", "Làm đề luyện tập"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Thứ 2", monday);

      // Thứ 3 - Học sâu & dự án
      List<TimeSlot> tuesday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:00", "08:30", "Thức dậy & Vệ sinh"),
              new TimeSlot("08:30", "11:30", "Làm dự án / code"),
              new TimeSlot("11:30", "12:30", "Ăn trưa"),
              new TimeSlot("13:00", "15:30", "Nghiên cứu tài liệu chuyên sâu"),
              new TimeSlot("16:00", "18:00", "Học nhóm / thảo luận"),
              new TimeSlot("18:30", "19:30", "Thể thao nhẹ"),
              new TimeSlot("20:00", "22:00", "Ôn tập từ vựng & lý thuyết"),
              new TimeSlot("22:30", "01:00", "Luyện đề"),
              new TimeSlot("01:30", "02:00", "Chuẩn bị ngày mai"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Thứ 3", tuesday);

      // Thứ 4 - Buổi học trên lớp + ôn
      List<TimeSlot> wednesday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:00", "09:00", "Thức dậy & Ăn sáng"),
              new TimeSlot("09:00", "12:00", "Học trên trường / bài giảng"),
              new TimeSlot("12:00", "13:00", "Ăn trưa"),
              new TimeSlot("13:30", "16:00", "Học môn phụ & làm bài"),
              new TimeSlot("16:30", "18:00", "Thực hành / lab"),
              new TimeSlot("18:30", "19:30", "Ăn tối & nghỉ"),
              new TimeSlot("20:00", "22:00", "Ôn tập kiểm tra giữa kỳ"),
              new TimeSlot("22:30", "01:00", "Đọc tài liệu mở rộng"),
              new TimeSlot("01:30", "02:00", "Chuẩn bị ngày mai"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Thứ 4", wednesday);

      // Thứ 5 - Kỹ năng & luyện đề
      List<TimeSlot> thursday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:00", "08:30", "Thức dậy & Vệ sinh"),
              new TimeSlot("08:30", "10:30", "Luyện đề / làm bài tập nặng"),
              new TimeSlot("11:00", "12:30", "Học kỹ năng nghề (Tiếng Anh/Tool)"),
              new TimeSlot("12:30", "13:30", "Ăn trưa"),
              new TimeSlot("14:00", "17:00", "Dự án cá nhân"),
              new TimeSlot("17:30", "19:00", "Thể thao (60 phút)"),
              new TimeSlot("19:30", "22:00", "Ôn tập môn xã hội / ghi nhớ"),
              new TimeSlot("22:30", "01:00", "Tổng hợp & ghi chú"),
              new TimeSlot("01:30", "02:00", "Chuẩn bị giấc ngủ"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Thứ 5", thursday);

      // Thứ 6 - Học nhóm & kiểm tra
      List<TimeSlot> friday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:00", "09:00", "Thức dậy & Ăn sáng"),
              new TimeSlot("09:00", "12:00", "Học nhóm / thuyết trình"),
              new TimeSlot("12:00", "13:00", "Ăn trưa"),
              new TimeSlot("13:30", "16:30", "Chuẩn bị kiểm tra / ôn kỹ"),
              new TimeSlot("17:00", "19:00", "Nghỉ ngơi & xã giao"),
              new TimeSlot("19:30", "21:30", "Học môn trọng tâm"),
              new TimeSlot("22:00", "00:30", "Luyện đề tối"),
              new TimeSlot("01:00", "02:00", "Tổng kết tuần"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Thứ 6", friday);

      // Thứ 7 - Làm bài lớn & thư giãn kết hợp
      List<TimeSlot> saturday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:30", "09:00", "Thức dậy & Thể thao nhẹ"),
              new TimeSlot("09:00", "12:00", "Làm bài lớn / dự án"),
              new TimeSlot("12:00", "13:00", "Ăn trưa"),
              new TimeSlot("13:30", "16:00", "Học tự do / nâng cao"),
              new TimeSlot("16:30", "18:00", "Hoạt động xã hội / nghỉ"),
              new TimeSlot("18:30", "20:00", "Thể thao / gym"),
              new TimeSlot("20:30", "22:30", "Giải trí & gặp bạn bè"),
              new TimeSlot("23:00", "01:00", "Ôn nhẹ & đọc sách"),
              new TimeSlot("01:30", "02:00", "Chuẩn bị ngủ"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Thứ 7", saturday);

      // Chủ Nhật - Kế hoạch tuần, phục hồi, học nhẹ
      List<TimeSlot> sunday = new ArrayList<>(Arrays.asList(
              new TimeSlot("08:00", "08:30", "Thức dậy & Vệ sinh"),
              new TimeSlot("08:30", "10:00", "Thể thao & ăn sáng"),
              new TimeSlot("10:00", "12:00", "Học sâu: dự án cá nhân"),
              new TimeSlot("12:00", "13:00", "Ăn trưa"),
              new TimeSlot("13:30", "15:30", "Lập kế hoạch tuần mới"),
              new TimeSlot("16:00", "18:00", "Học Tiếng Anh linh hoạt"),
              new TimeSlot("18:30", "20:00", "Nghỉ & ăn tối"),
              new TimeSlot("20:30", "22:00", "Giải trí nhẹ"),
              new TimeSlot("22:30", "01:00", "Ôn tập & tổng hợp"),
              new TimeSlot("01:30", "02:00", "Chuẩn bị ngày mai"),
              new TimeSlot("02:00", "", "Đi ngủ")
      ));
      weeklyActivities.put("Chủ Nhật", sunday);

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
        String description = "Ưu tiên tối đa hóa thời gian học cho một \"Môn Chính\" (ví dụ: môn thi cuối kỳ, đồ án tốt nghiệp) bằng cách dành gần như toàn bộ thời gian tự học trong ngày cho nó.";
        List<String> tags = Arrays.asList("8h_sleep", "8h_study", "30m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung cho Thứ 2, 3, 5, 7 ---
        List<TimeSlot> commonSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Thức dậy, Ăn sáng, Vận động nhẹ"),
                new TimeSlot("07:00", "09:00", "Học Môn Chính"),
                new TimeSlot("09:00", "09:30", "Giải lao & vận động nhẹ"),
                new TimeSlot("09:10", "11:40", "Tiếp tục Môn Chính (Luyện đề/Bài tập)"),
                new TimeSlot("11:40", "14:00", "Ăn trưa, Nghỉ ngơi"),
                new TimeSlot("14:00", "15:30", "Học Môn Phụ"),
                new TimeSlot("15:30", "17:30", "Luyện đề / làm chuyên đề khó của môn chính"),
                new TimeSlot("17:30", "19:30", "Ăn tối & Thư giãn (Tái tạo năng lượng)"),
                new TimeSlot("19:30", "21:00", "Tiếp tục Môn Chính (Giải đề trọn vẹn)"),
                new TimeSlot("21:00", "22:30", "Ghi \"Sổ tay kiến thức\" & Review môn khác"),
                new TimeSlot("22:30", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", commonSchedule);
        weeklyActivities.put("Thứ 3", commonSchedule);
        weeklyActivities.put("Thứ 5", commonSchedule);
        weeklyActivities.put("Thứ 7", commonSchedule);

        // --- Lịch trình riêng cho Thứ 4, 6 (có đi học) ---
        List<TimeSlot> schoolDaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Học trên trường (Tiết 1-3)"), // Gộp chung
                new TimeSlot("07:00", "09:00", "Học Môn Chính"),
                new TimeSlot("09:00", "09:30", "Giải lao & vận động nhẹ"),
                new TimeSlot("09:10", "11:40", "Tiếp tục Môn Chính (Luyện đề/Bài tập)"),
                new TimeSlot("11:40", "14:00", "Ăn trưa, Nghỉ ngơi"),
                new TimeSlot("14:00", "15:30", "Học Môn Phụ"),
                new TimeSlot("15:30", "17:30", "Luyện đề / làm chuyên đề khó của môn chính"),
                new TimeSlot("17:30", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Tiếp tục Môn Chính (Giải đề trọn vẹn)"),
                new TimeSlot("21:00", "22:30", "Ghi \"Sổ tay kiến thức\" & Review môn khác"),
                new TimeSlot("22:30", "", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 4", schoolDaySchedule);
        weeklyActivities.put("Thứ 6", schoolDaySchedule);

        // --- Lịch trình riêng cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Thức dậy, Ăn sáng, Vận động nhẹ"),
                new TimeSlot("07:00", "09:00", "Học Môn Chính"),
                new TimeSlot("09:00", "09:30", "Giải lao & vận động nhẹ"),
                new TimeSlot("09:10", "11:40", "Tiếp tục Môn Chính (Luyện đề/Bài tập)"),
                new TimeSlot("11:40", "14:00", "Ăn trưa, Nghỉ ngơi"),
                new TimeSlot("14:00", "15:30", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("15:30", "17:30", "Tổng kết tuần/Giải trí"),
                new TimeSlot("17:30", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Giải trí/Thư giãn"),
                new TimeSlot("21:00", "22:30", "Chuẩn bị đi ngủ"),
                new TimeSlot("22:30", "", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
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
       String description = "Giúp kết hợp phương pháp nhịn ăn gián đoạn `16-8`, tối ưu hóa sự tập trung buổi sáng và đảm bảo phục hồi đầy đủ mà vẫn có thời gian cho thể thao.";
       List<String> tags = Arrays.asList("8h_sleep", "6h_study", "60m_sport", "60m_relax");
       Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

       // Thứ 2 - Focus study morning, eating window midday-evening
       List<TimeSlot> monday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "09:30", "Deep focus study (no food)"),
               new TimeSlot("09:30", "11:30", "Light practice / review"),
               new TimeSlot("11:30", "12:30", "Break & first meal (start eating window)"),
               new TimeSlot("12:30", "14:00", "Power nap / recovery"),
               new TimeSlot("14:00", "17:00", "Project work / coding"),
               new TimeSlot("17:00", "18:00", "Light exercise (walk / stretch)"),
               new TimeSlot("18:00", "20:00", "Second meal & review (end eating window)"),
               new TimeSlot("20:30", "22:00", "Light study / hobby"),
               new TimeSlot("22:30", "", "Sleep"))
       );
       weeklyActivities.put("Thứ 2", monday);

       // Thứ 3 - Morning cardio then study blocks
       List<TimeSlot> tuesday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:00", "07:00", "Cardio / mobility (fasted)"),
               new TimeSlot("07:30", "10:30", "Focused study / lectures"),
               new TimeSlot("10:30", "12:00", "Practice problems"),
               new TimeSlot("12:00", "13:00", "First meal & rest"),
               new TimeSlot("13:30", "15:00", "Power nap"),
               new TimeSlot("15:00", "18:00", "Group work / meetings"),
               new TimeSlot("18:00", "20:00", "Dinner & review"),
               new TimeSlot("20:30", "22:00", "Language practice / reading"),
               new TimeSlot("22:30", "", "Sleep"))
       );
       weeklyActivities.put("Thứ 3", tuesday);

       // Thứ 4 - Lecture day with focused evenings
       List<TimeSlot> wednesday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "09:00", "Review lecture notes (fasted)"),
               new TimeSlot("09:30", "12:00", "Attend classes / labs"),
               new TimeSlot("12:00", "13:00", "First meal & social break"),
               new TimeSlot("13:30", "15:30", "Lab work / assignments"),
               new TimeSlot("16:00", "17:30", "Short training / mobility"),
               new TimeSlot("17:30", "19:30", "Dinner & unwind"),
               new TimeSlot("20:00", "22:00", "Focused revision / practice"),
               new TimeSlot("22:30", "", "Sleep"))
       );
       weeklyActivities.put("Thứ 4", wednesday);

       // Thứ 5 - Exam prep intensive
       List<TimeSlot> thursday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:00", "09:00", "Timed practice / mock tests (fasted)"),
               new TimeSlot("09:30", "11:30", "Error review & notes"),
               new TimeSlot("11:30", "12:30", "First meal & short walk"),
               new TimeSlot("12:30", "14:00", "Power nap"),
               new TimeSlot("14:00", "17:00", "Problem sets / deep work"),
               new TimeSlot("17:30", "18:30", "Stretching / light exercise"),
               new TimeSlot("18:30", "20:00", "Dinner & consolidation"),
               new TimeSlot("20:30", "22:00", "Flashcards / spaced repetition"),
               new TimeSlot("22:30", "", "Sleep"))
       );
       weeklyActivities.put("Thứ 5", thursday);

       // Thứ 6 - Practical & project day
       List<TimeSlot> friday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "09:00", "Tooling / coding practice (fasted)"),
               new TimeSlot("09:30", "11:30", "Apply solutions to project"),
               new TimeSlot("11:30", "12:30", "First meal & brief planning"),
               new TimeSlot("12:30", "14:00", "Power nap"),
               new TimeSlot("14:00", "17:00", "Integration / testing"),
               new TimeSlot("17:00", "18:00", "Social / networking"),
               new TimeSlot("18:00", "20:00", "Dinner & demo prep"),
               new TimeSlot("20:30", "22:00", "Light review / hobby"),
               new TimeSlot("22:30", "", "Sleep"))
       );
       weeklyActivities.put("Thứ 6", friday);

       // Thứ 7 - Active recovery and catch-up
       List<TimeSlot> saturday = new ArrayList<>(Arrays.asList(
               new TimeSlot("08:00", "09:30", "Longer workout / sport (fasted)"),
               new TimeSlot("09:30", "11:30", "Personal projects / learning"),
               new TimeSlot("11:30", "13:00", "Brunch (start eating window)"),
               new TimeSlot("13:30", "16:00", "Creative work / catch-up"),
               new TimeSlot("16:00", "18:00", "Social / outdoors"),
               new TimeSlot("18:00", "20:00", "Dinner & gentle review"),
               new TimeSlot("20:30", "22:30", "Relax / hobbies"),
               new TimeSlot("23:00", "", "Sleep"))
       );
       weeklyActivities.put("Thứ 7", saturday);

       // Chủ Nhật - Recovery, planning, light study
       List<TimeSlot> sunday = new ArrayList<>(Arrays.asList(
               new TimeSlot("08:30", "10:00", "Slow morning, mobility (fasted)"),
               new TimeSlot("10:00", "11:30", "Light reading / reflection"),
               new TimeSlot("11:30", "13:00", "Brunch & family time"),
               new TimeSlot("13:30", "15:30", "Plan next week & set priorities"),
               new TimeSlot("16:00", "18:00", "Easy study / creative practice"),
               new TimeSlot("18:00", "20:00", "Dinner & tidy up"),
               new TimeSlot("20:30", "22:00", "Prepare materials / relax"),
               new TimeSlot("22:30", "", "Sleep"))
       );
       weeklyActivities.put("Chủ Nhật", sunday);

       return new DetailedSchedule(title, description, tags, weeklyActivities);
   }

    // 7. Template "Vừa học vừa làm"
    public static DetailedSchedule createWorkAndLearnTemplate() {
        String title = "Vừa học vừa làm";
        String description = "Template này dành cho sinh viên có quỹ thời gian hạn chế, muốn \\\"học ra học, làm ra làm\\\", tối ưu hóa thời gian học ngắn nhưng hiệu quả, kết hợp làm thêm và phục hồi.";
        List<String> tags = Arrays.asList("8h_sleep", "4h_study", "30m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // Thứ 2 - Tập trung kỹ năng buổi tối
        List<TimeSlot> monday = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Sáng nhanh: vệ sinh, chuẩn bị"),
                new TimeSlot("07:00", "08:00", "Micro study: ôn flashcard / đọc nhanh"),
                new TimeSlot("09:00", "17:00", "Làm việc / học hành chính"),
                new TimeSlot("17:30", "18:00", "Thể dục nhẹ / đi bộ"),
                new TimeSlot("18:00", "19:00", "Ăn tối & nghỉ"),
                new TimeSlot("19:00", "21:00", "Focused study / kỹ năng nghề (2h)"),
                new TimeSlot("21:15", "22:00", "Review ngắn / lên kế hoạch"),
                new TimeSlot("22:30", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 2", monday);

        // Thứ 3 - Làm dự án + học kỹ năng thực tế
        List<TimeSlot> tuesday = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Sáng nhanh & chuẩn bị"),
                new TimeSlot("07:00", "08:00", "Micro study: bài tập nhỏ"),
                new TimeSlot("09:00", "17:00", "Làm việc / học hành chính"),
                new TimeSlot("17:30", "18:00", "Thể dục ngắn"),
                new TimeSlot("18:00", "19:00", "Ăn tối"),
                new TimeSlot("19:00", "21:00", "Project work / portfolio (2h)"),
                new TimeSlot("21:15", "22:00", "Học ngoại ngữ nhẹ"),
                new TimeSlot("22:30", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 3", tuesday);

        // Thứ 4 - Học nhanh buổi sáng + tối ôn
        List<TimeSlot> wednesday = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:00", "07:00", "Micro study buổi sáng (1h)"),
                new TimeSlot("09:00", "17:00", "Làm việc / học hành chính"),
                new TimeSlot("12:30", "13:00", "Lunch review (30m)"),
                new TimeSlot("17:30", "18:00", "Thể dục nhẹ"),
                new TimeSlot("18:00", "19:00", "Ăn tối"),
                new TimeSlot("19:00", "20:30", "Focused study (1.5h)"),
                new TimeSlot("20:45", "22:00", "Skills practice / ứng tuyển"),
                new TimeSlot("22:30", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 4", wednesday);

        // Thứ 5 - Ca làm thêm buổi tối (ví dụ part\-time)
        List<TimeSlot> thursday = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Sáng nhanh"),
                new TimeSlot("07:00", "08:00", "Micro study"),
                new TimeSlot("09:00", "17:00", "Làm việc / học hành chính"),
                new TimeSlot("17:30", "18:00", "Thể dục/di chuyển"),
                new TimeSlot("18:00", "19:00", "Ăn tối"),
                new TimeSlot("19:30", "22:00", "Ca làm thêm / freelance"),
                new TimeSlot("22:30", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 5", thursday);

        // Thứ 6 - Tổng kết tuần & ôn nhẹ
        List<TimeSlot> friday = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:00", "Sáng nhanh"),
                new TimeSlot("07:00", "08:00", "Micro study"),
                new TimeSlot("09:00", "17:00", "Làm việc / học hành chính"),
                new TimeSlot("17:30", "18:00", "Thể dục nhẹ"),
                new TimeSlot("18:00", "19:00", "Ăn tối"),
                new TimeSlot("19:00", "21:00", "Tổng kết tuần, hoàn thiện nhiệm vụ (2h)"),
                new TimeSlot("21:30", "22:30", "Giải trí / thư giãn"),
                new TimeSlot("23:00", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 6", friday);

        // Thứ 7 - Học sâu / làm thêm buổi sáng, phục hồi buổi chiều
        List<TimeSlot> saturday = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "09:00", "Thức dậy & vận động nhẹ"),
                new TimeSlot("09:00", "12:00", "Học sâu / làm dự án (3h)"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & nghỉ"),
                new TimeSlot("14:00", "16:00", "Làm thêm / freelance (2h)"),
                new TimeSlot("16:30", "18:00", "Hoạt động xã hội / thể thao"),
                new TimeSlot("18:30", "20:00", "Ăn tối & thư giãn"),
                new TimeSlot("20:30", "22:30", "Hobby / học nhẹ"),
                new TimeSlot("23:00", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturday);

        // Chủ Nhật - Phục hồi và lập kế hoạch tuần mới
        List<TimeSlot> sunday = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:30", "09:30", "Sáng chậm & vận động"),
                new TimeSlot("09:30", "11:30", "Catch-up: hoàn thiện việc tồn đọng"),
                new TimeSlot("11:30", "13:00", "Brunch & nghỉ"),
                new TimeSlot("13:30", "15:00", "Lập kế hoạch tuần mới"),
                new TimeSlot("15:30", "17:00", "Học nhẹ / ôn tập ngắn"),
                new TimeSlot("17:30", "19:00", "Gia đình / ăn tối"),
                new TimeSlot("19:30", "21:00", "Chuẩn bị tài liệu cho tuần"),
                new TimeSlot("22:00", "", "Ngủ sớm")
        ));
        weeklyActivities.put("Chủ Nhật", sunday);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 8. Template "Năng suất theo khoa học"
   public static DetailedSchedule createScientificProductivityTemplate() {
       String title = "Năng suất theo khoa học";
       String description = "Template này tối ưu hóa 10 giờ học tập năng suất mỗi ngày (kết hợp Tự học + Học trên trường) và cân bằng với thể thao, sở thích cá nhân.";
       List<String> tags = Arrays.asList("6h_sleep", "10h_study", "60m_sport", "60m_relax");
       Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

       // Thứ 2 - Deep work start of week
       List<TimeSlot> monday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "07:00", "Thức dậy & vận động nhẹ"),
               new TimeSlot("07:00", "10:00", "Deep work - Pomodoro blocks (3h)"),
               new TimeSlot("10:00", "10:30", "Giải lao, đi lại"),
               new TimeSlot("10:30", "12:30", "Tiếp tục deep work / bài tập (2h)"),
               new TimeSlot("12:30", "13:30", "Ăn trưa & nghỉ"),
               new TimeSlot("13:30", "15:30", "Học môn phụ / thực hành (2h)"),
               new TimeSlot("15:30", "16:00", "Trà / giải lao ngắn"),
               new TimeSlot("16:00", "18:00", "Làm dự án / áp dụng kiến thức (2h)"),
               new TimeSlot("18:30", "19:30", "Thể thao 60m"),
               new TimeSlot("20:00", "21:00", "Ôn tập nhẹ / flashcards"),
               new TimeSlot("21:30", "22:30", "Thư giãn & chuẩn bị ngày mai"),
               new TimeSlot("22:30", "", "Ngủ")
       ));
       weeklyActivities.put("Thứ 2", monday);

       // Thứ 3 - Class-heavy day with focused evening study
       List<TimeSlot> tuesday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "07:00", "Thức dậy & vận động nhẹ"),
               new TimeSlot("07:00", "09:00", "Ôn bài trước lớp (2h)"),
               new TimeSlot("09:30", "12:00", "Học trên trường / lab"),
               new TimeSlot("12:00", "13:00", "Ăn trưa"),
               new TimeSlot("13:30", "15:30", "Thực hành lab / bài tập"),
               new TimeSlot("15:30", "16:00", "Giải lao"),
               new TimeSlot("16:00", "18:00", "Deep review (2h)"),
               new TimeSlot("18:30", "19:30", "Thể thao 60m"),
               new TimeSlot("20:00", "22:00", "Bloc tối - tập trung theo chủ đề (2h)"),
               new TimeSlot("22:30", "", "Ngủ")
       ));
       weeklyActivities.put("Thứ 3", tuesday);

       // Thứ 4 - Focus on problem solving and spaced repetition
       List<TimeSlot> wednesday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "07:00", "Thức dậy & vận động"),
               new TimeSlot("07:00", "10:00", "Giải đề / problem solving (3h)"),
               new TimeSlot("10:00", "10:30", "Giải lao"),
               new TimeSlot("10:30", "12:30", "Ghi chú & hệ thống kiến thức (2h)"),
               new TimeSlot("12:30", "13:30", "Ăn trưa"),
               new TimeSlot("13:30", "15:00", "Học nhẹ / dự án nhỏ"),
               new TimeSlot("15:30", "17:30", "Ứng dụng & test (2h)"),
               new TimeSlot("18:00", "19:00", "Thể thao 60m"),
               new TimeSlot("19:30", "21:30", "Luyện tập lặp lại theo SRS"),
               new TimeSlot("22:00", "", "Ngủ")
       ));
       weeklyActivities.put("Thứ 4", wednesday);

       // Thứ 5 - Pair programming / group study day
       List<TimeSlot> thursday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "07:00", "Sáng nhanh & kiểm tra to-do"),
               new TimeSlot("07:00", "10:00", "Pair programming / nhóm (3h)"),
               new TimeSlot("10:00", "10:30", "Giải lao"),
               new TimeSlot("10:30", "12:30", "Hoàn thiện deliverables (2h)"),
               new TimeSlot("12:30", "13:30", "Ăn trưa"),
               new TimeSlot("13:30", "15:30", "Học chủ đề mới & ứng dụng"),
               new TimeSlot("16:00", "18:00", "Tổng hợp & documentation (2h)"),
               new TimeSlot("18:30", "19:30", "Thể thao 60m"),
               new TimeSlot("20:00", "22:00", "Ôn tập trọng tâm"),
               new TimeSlot("22:30", "", "Ngủ")
       ));
       weeklyActivities.put("Thứ 5", thursday);

       // Thứ 6 - Sprints & consolidation
       List<TimeSlot> friday = new ArrayList<>(Arrays.asList(
               new TimeSlot("06:30", "07:00", "Thức dậy & kế hoạch sprint ngày"),
               new TimeSlot("07:00", "10:00", "Sprint: hoàn thành nhiệm vụ quan trọng (3h)"),
               new TimeSlot("10:00", "10:30", "Giải lao"),
               new TimeSlot("10:30", "12:30", "Refactor & tối ưu (2h)"),
               new TimeSlot("12:30", "13:30", "Ăn trưa"),
               new TimeSlot("13:30", "15:30", "Tự học mở rộng / đọc paper"),
               new TimeSlot("16:00", "18:00", "Chuẩn bị demo / presentation (2h)"),
               new TimeSlot("18:30", "19:30", "Thể thao 60m"),
               new TimeSlot("20:00", "22:00", "Tổng kết tuần & backlog grooming"),
               new TimeSlot("22:30", "", "Ngủ")
       ));
       weeklyActivities.put("Thứ 6", friday);

       // Thứ 7 - Creative / long-form learning and light recovery
       List<TimeSlot> saturday = new ArrayList<>(Arrays.asList(
               new TimeSlot("08:00", "09:00", "Thức dậy & vận động nhẹ"),
               new TimeSlot("09:00", "12:00", "Learning sprint: course / project (3h)"),
               new TimeSlot("12:00", "13:30", "Ăn trưa & nghỉ"),
               new TimeSlot("14:00", "16:00", "Creative work / side project (2h)"),
               new TimeSlot("16:30", "18:00", "Hoạt động xã hội / recovery"),
               new TimeSlot("18:30", "19:30", "Thể thao 60m"),
               new TimeSlot("20:00", "22:00", "Hobby / đọc mở rộng"),
               new TimeSlot("22:30", "", "Ngủ")
       ));
       weeklyActivities.put("Thứ 7", saturday);

       // Chủ Nhật - Full review & planning
       List<TimeSlot> sunday = new ArrayList<>(Arrays.asList(
               new TimeSlot("08:00", "09:00", "Thức dậy & nhẹ nhàng"),
               new TimeSlot("09:00", "11:00", "Weekly review: notes & progress"),
               new TimeSlot("11:00", "12:30", "Ăn trưa"),
               new TimeSlot("13:00", "15:00", "Plan next week & prioritize"),
               new TimeSlot("15:30", "17:30", "Catch-up / loose ends"),
               new TimeSlot("18:00", "19:00", "Thể thao nhẹ"),
               new TimeSlot("19:30", "21:00", "Chuẩn bị tài liệu & thư giãn"),
               new TimeSlot("21:30", "22:30", "Wind-down routine"),
               new TimeSlot("22:30", "", "Ngủ sớm")
       ));
       weeklyActivities.put("Chủ Nhật", sunday);

       return new DetailedSchedule(title, description, tags, weeklyActivities);
   }

    // 9. Template "Lịch sinh hoạt của Bác Hồ"
   public static DetailedSchedule createUncleHoLifestyleTemplate() {
        String title = "Lịch sinh hoạt của Bác Hồ";
        String description = "Dành cho những sinh viên muốn rèn luyện một lối sống kỷ luật, tập trung cao độ vào công việc và học tập, đồng thời duy trì sức khỏe và sự phát triển cá nhân một cách bền bỉ.";
        List<String> tags = Arrays.asList("8h_sleep", "8h_study", "30m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // Thứ 2 - Thứ 6: Kỷ luật hàng ngày
        List<TimeSlot> weekday = new ArrayList<>(Arrays.asList(
                new TimeSlot("04:30", "05:00", "Thức dậy, vệ sinh, suy ngẫm ngắn"),
                new TimeSlot("05:00", "07:00", "Học tập tập trung (Môn chính)"),
                new TimeSlot("07:00", "07:30", "Ăn sáng đơn giản"),
                new TimeSlot("07:30", "12:00", "Làm việc / học trên lớp / nhiệm vụ thực tế (các block ngắn có giải lao)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & nghỉ ngơi"),
                new TimeSlot("13:00", "15:00", "Học/ứng dụng kiến thức (đọc, ghi chép)"),
                new TimeSlot("15:00", "16:30", "Công tác xã hội / trao đổi nhóm / thực hành"),
                new TimeSlot("17:00", "17:30", "Thể dục nhẹ / đi bộ (30m)"),
                new TimeSlot("18:00", "18:30", "Ăn tối đơn giản"),
                new TimeSlot("19:00", "20:30", "Ôn tập & hoàn thiện nhiệm vụ"),
                new TimeSlot("20:30", "21:30", "Đọc sách trau dồi tư tưởng / suy ngẫm"),
                new TimeSlot("21:30", "22:00", "Chuẩn bị ngày mai, viết nhật ký ngắn"),
                new TimeSlot("22:00", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 2", weekday);
        weeklyActivities.put("Thứ 3", weekday);
        weeklyActivities.put("Thứ 4", weekday);
        weeklyActivities.put("Thứ 5", weekday);
        weeklyActivities.put("Thứ 6", weekday);

        // Thứ 7 - Làm việc cộng đồng, học dự án
        List<TimeSlot> saturday = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "05:30", "Thức dậy & vệ sinh"),
                new TimeSlot("05:30", "07:30", "Công việc cộng đồng / hỗ trợ người khác"),
                new TimeSlot("07:30", "08:00", "Ăn sáng"),
                new TimeSlot("08:00", "12:00", "Dự án lâu dài / học nâng cao"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & nghỉ"),
                new TimeSlot("13:00", "15:00", "Hoạt động thể chất / thể thao (60m)"),
                new TimeSlot("15:30", "17:30", "Học mở rộng / đọc chọn lọc"),
                new TimeSlot("18:00", "19:00", "Ăn tối"),
                new TimeSlot("19:00", "21:00", "Gặp gỡ cộng sự / trao đổi ý tưởng"),
                new TimeSlot("21:30", "22:00", "Chuẩn bị tuần sau"),
                new TimeSlot("22:00", "", "Ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturday);

        // Chủ Nhật - Phục hồi, lập kế hoạch, tự trau dồi
        List<TimeSlot> sunday = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy & vận động nhẹ"),
                new TimeSlot("06:00", "08:00", "Đọc sách tư tưởng, nghiên cứu lịch sử / văn hóa"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & gia đình"),
                new TimeSlot("09:00", "11:00", "Tổng kết tuần, sắp xếp công việc"),
                new TimeSlot("11:30", "13:00", "Ăn trưa & nghỉ"),
                new TimeSlot("13:30", "15:00", "Ôn lại kiến thức chính / viết nhật ký học tập"),
                new TimeSlot("15:30", "17:00", "Hoạt động xã hội nhẹ / thăm hỏi"),
                new TimeSlot("17:30", "18:30", "Thể dục nhẹ"),
                new TimeSlot("19:00", "20:30", "Chuẩn bị tài liệu & kế hoạch tuần mới"),
                new TimeSlot("21:00", "22:00", "Suy ngẫm, thư giãn nhẹ"),
                new TimeSlot("22:00", "", "Ngủ sớm")
        ));
        weeklyActivities.put("Chủ Nhật", sunday);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }
}
