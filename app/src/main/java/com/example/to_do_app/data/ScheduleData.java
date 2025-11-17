package com.example.to_do_app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.to_do_app.model.DetailedSchedule;
import com.example.to_do_app.model.ScheduleTemplate;
import com.example.to_do_app.model.TimeSlot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ScheduleData - sửa lại các phương thức để:
 * - Giữ thứ tự ngày (ưu tiên Thứ 2 -> Chủ Nhật) khi tạo/đọc templates.
 * - Sao chép defensive List khi chuyển các danh sách chung giữa nhiều ngày.
 * - Lưu/đọc custom template với khoá mã hoá tiêu đề an toàn.
 * - Khi load custom template, xây dựng weeklyActivities bằng LinkedHashMap để giữ thứ tự.
 *
 * Lưu ý: DetailedSchedule được sử dụng làm ScheduleTemplate trong getSampleTemplates() - giả định lớp
 * DetailedSchedule extends / implements ScheduleTemplate trong project của bạn (giống cấu trúc ban đầu).
 */
public class ScheduleData {
    private static final String TAG = "ScheduleData";
    private static final String PROFILE_PREFS = "profile_prefs";
    private static final String CUSTOM_TEMPLATE_PREFIX = "custom_template_";

    // Mong muốn thứ tự hiển thị các ngày (nếu template dùng map không có thứ tự).
    private static final List<String> PREFERRED_DAY_ORDER = Arrays.asList(
            "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật"
    );

    /**
     * Phương thức chính để AddFragment gọi.
     * Nó sẽ tập hợp tất cả các template mẫu lại với nhau.
     */
    public static List<ScheduleTemplate> getSampleTemplates() {
        List<ScheduleTemplate> templates = new ArrayList<>();

        // Thêm từng template (giữ nguyên thứ tự muốn hiển thị)
        List<ScheduleTemplate> created = new ArrayList<>();
        created.add(createNightOwlTemplate());           // 1. CÚ ĐÊM
        created.add(createMorningPersonTemplate());      // 2. Chuyên buổi sáng
        created.add(createDeepWorkTemplate());           // 3. Học môn chuyên sâu
        created.add(createWorkHardPlayHardTemplate());   // 4. Sáng học, tối chơi
        created.add(createSprintWeekTemplate());         // 5. Tối ưu hóa chu kỳ ngủ
        created.add(createIntermittentFastingTemplate());// 6. Vừa học vừa ăn gián đoạn 16-8
        created.add(createWorkAndLearnTemplate());       // 7. Vừa học vừa làm
        created.add(createScientificProductivityTemplate());// 8. Năng suất theo khoa học
        created.add(createUncleHoLifestyleTemplate());   // 9. Lịch sinh hoạt của Bác Hồ
        created.add(createEnglishSelfStudyTemplate());   // 10. Lịch tự học tiếng Anh
        created.add(createFiveOneOneMethodTemplate());   // 11. 1 tuần theo phương pháp 5-1-1
        created.add(createWorkingProfessionalTemplate());// 12. Người đi làm
        created.add(createJuniorHighStudentTemplate());  // 13. Học Sinh Cấp 2
        created.add(createExamPrepTemplate());           // 14. Luyện Thi THPT cho học sinh cấp 3
        created.add(createCreativeFlowTemplate());       // 15. "Sáng Tạo"
        created.add(createHealingRetreatTemplate());     // 16. Hồi sức chữa lành
        created.add(createUbermanSleepTemplate());       // 17. Ngủ Đa Pha (Uberman)
        created.add(createSiestaSleepTemplate());        // 18. Giấc Ngủ Siesta
        created.add(createDualCoreSleepTemplate());      // 19. Ngủ Lõi Đôi
        created.add(createStudentAthleteTemplate());     // 20. Vận động viên ham học


        // Defensive normalize each template:
        for (ScheduleTemplate t : created) {
            if (t == null) continue;
            // Nếu DetailedSchedule cung cấp getWeeklyActivities(), chuyển map về LinkedHashMap + defensive copy lists
            if (t instanceof DetailedSchedule) {
                DetailedSchedule ds = (DetailedSchedule) t;
                Map<String, List<TimeSlot>> weekly = ds.getWeeklyActivities();
                if (weekly != null && !weekly.isEmpty()) {
                    LinkedHashMap<String, List<TimeSlot>> ordered = new LinkedHashMap<>();

                    // First, add preferred day order if present
                    for (String day : PREFERRED_DAY_ORDER) {
                        if (weekly.containsKey(day)) {
                            List<TimeSlot> original = weekly.get(day);
                            ordered.put(day, original == null ? new ArrayList<>() : new ArrayList<>(original));
                        }
                    }
                    // Then add any leftover keys in insertion order from original map
                    for (Map.Entry<String, List<TimeSlot>> e : weekly.entrySet()) {
                        String k = e.getKey();
                        if (!ordered.containsKey(k)) {
                            List<TimeSlot> original = e.getValue();
                            ordered.put(k, original == null ? new ArrayList<>() : new ArrayList<>(original));
                        }
                    }

                    // Replace weeklyActivities in DetailedSchedule if setter exists, else rely on constructor copy:
                    try {
                        ds.setWeeklyActivities(ordered);
                    } catch (NoSuchMethodError | Exception ex) {
                        // If DetailedSchedule is immutable or has no setter, try to create a new DetailedSchedule instance
                        try {
                            DetailedSchedule copy = new DetailedSchedule(
                                    ds.getTitle(),
                                    ds.getDescription(),
                                    ds.getTags() == null ? new ArrayList<>() : new ArrayList<>(ds.getTags()),
                                    ordered
                            );
                            // Replace t in templates list by copy
                            t = copy;
                        } catch (Exception e2) {
                            Log.w(TAG, "Could not replace DetailedSchedule with ordered copy: " + ds.getTitle(), e2);
                        }
                    }
                }
            }
            templates.add(t);
        }

        return templates;
    }

    // ----------------- Encoding helpers -----------------
    private static String encodeTitleForKey(String title) {
        if (title == null) return "";
        try {
            // use StandardCharsets to avoid UnsupportedEncodingException in modern Android
            return URLEncoder.encode(title, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // fallback: replace spaces and non-visible chars
            return title.replaceAll("\\s+", "_");
        } catch (Exception e) {
            return title.replaceAll("\\s+", "_");
        }
    }

    private static String decodeTitleFromKeyPart(String keyPart) {
        if (keyPart == null) return null;
        try {
            return URLDecoder.decode(keyPart, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return keyPart;
        } catch (Exception ex) {
            return keyPart;
        }
    }

    // ----------------- Load / Save / List all templates -----------------

    public static List<ScheduleTemplate> getAllTemplates(Context ctx) {
        final String TAG = "ScheduleData";
        List<ScheduleTemplate> result = new ArrayList<>();

        // 1) Add default templates first
        try {
            List<ScheduleTemplate> defaults = getSampleTemplates();
            if (defaults != null) result.addAll(defaults);
        } catch (Exception ex) {
            Log.w(TAG, "getSampleTemplates() failed", ex);
        }

        // Build a set of normalized titles already present (defaults)
        Set<String> seenTitles = new HashSet<>();
        for (ScheduleTemplate t : result) {
            if (t == null) continue;
            String title = null;
            try {
                if (t instanceof DetailedSchedule) title = ((DetailedSchedule) t).getTitle();
                else {
                    // try generic getTitle() if ScheduleTemplate defines it
                    title = ((ScheduleTemplate) t).getTitle();
                }
            } catch (Exception ex) {
                // ignore
            }
            if (title == null) continue;
            seenTitles.add(title.trim().toLowerCase());
        }

        // 2) Load custom templates from SharedPreferences (support encoded keys + legacy raw keys)
        if (ctx != null) {
            try {
                SharedPreferences prefs = ctx.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
                Map<String, ?> all = prefs.getAll();
                if (all != null && !all.isEmpty()) {
                    // iterate keys in stable order by copying to list (SharedPreferences map iteration order is unspecified)
                    List<String> keys = new ArrayList<>(all.keySet());
                    for (String key : keys) {
                        if (key == null) continue;
                        if (!key.startsWith(CUSTOM_TEMPLATE_PREFIX)) continue;

                        String keyPart = key.substring(CUSTOM_TEMPLATE_PREFIX.length());
                        String title = decodeTitleFromKeyPart(keyPart);
                        if (title == null || title.trim().isEmpty()) {
                            // fallback: if decode produced empty, try raw keyPart
                            title = keyPart;
                        }

                        String norm = title.trim().toLowerCase();
                        if (seenTitles.contains(norm)) {
                            // skip if a default (or earlier custom) already supplies this title
                            continue;
                        }

                        try {
                            DetailedSchedule ds = loadCustomTemplateByPrefKey(ctx, key);
                            if (ds != null) {
                                // ensure weeklyActivities uses LinkedHashMap + defensive copies (load method already does but double-check)
                                Map<String, List<TimeSlot>> weekly = ds.getWeeklyActivities();
                                if (weekly != null) {
                                    LinkedHashMap<String, List<TimeSlot>> ordered = new LinkedHashMap<>();
                                    for (String day : PREFERRED_DAY_ORDER) {
                                        if (weekly.containsKey(day)) ordered.put(day, new ArrayList<>(weekly.get(day)));
                                    }
                                    for (Map.Entry<String, List<TimeSlot>> e : weekly.entrySet()) {
                                        if (!ordered.containsKey(e.getKey())) ordered.put(e.getKey(), new ArrayList<>(e.getValue()));
                                    }
                                    try {
                                        ds.setWeeklyActivities(ordered);
                                    } catch (Exception ignore) { /* ignore if no setter */ }
                                }
                                result.add(ds);
                                seenTitles.add(norm);
                            } else {
                                Log.w(TAG, "loadCustomTemplateByPrefKey returned null for prefKey=" + key);
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Failed to load custom template for prefKey=" + key, ex);
                        }
                    }
                }
            } catch (Exception ex) {
                Log.w(TAG, "Error reading custom templates from prefs", ex);
            }
        }

        return result;
    }

    public static void saveCustomTemplate(Context ctx, DetailedSchedule template) {
        if (ctx == null || template == null || template.getTitle() == null) return;
        try {
            JSONObject root = new JSONObject();
            root.put("title", template.getTitle());
            root.put("description", template.getDescription() == null ? "" : template.getDescription());

            JSONArray tagsArr = new JSONArray();
            List<String> tags = template.getTags();
            if (tags != null) {
                for (String t : tags) tagsArr.put(t);
            }
            root.put("tags", tagsArr);

            // Use ordered weekly object: preferred day order first, then any extras
            JSONObject weeklyObj = new JSONObject();
            Map<String, List<TimeSlot>> weekly = template.getWeeklyActivities();
            if (weekly != null) {
                // first add preferred days in order if present
                for (String day : PREFERRED_DAY_ORDER) {
                    if (weekly.containsKey(day)) {
                        JSONArray arr = timeSlotsToJsonArray(weekly.get(day));
                        weeklyObj.put(day, arr);
                    }
                }
                // add leftover keys
                for (Map.Entry<String, List<TimeSlot>> entry : weekly.entrySet()) {
                    String dayKey = entry.getKey();
                    if (weeklyObj.has(dayKey)) continue;
                    JSONArray arr = timeSlotsToJsonArray(entry.getValue());
                    weeklyObj.put(dayKey, arr);
                }
            }
            root.put("weekly", weeklyObj);

            SharedPreferences prefs = ctx.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            String encodedKey = CUSTOM_TEMPLATE_PREFIX + encodeTitleForKey(template.getTitle());

            // Save under encoded key (safer). Also remove any legacy raw key for the same title.
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(encodedKey, root.toString());

            // remove legacy raw key if exists
            String legacyKey = CUSTOM_TEMPLATE_PREFIX + template.getTitle();
            if (!legacyKey.equals(encodedKey) && prefs.contains(legacyKey)) {
                editor.remove(legacyKey);
            }

            editor.apply();
            Log.d(TAG, "Saved custom template: " + template.getTitle() + " -> prefKey=" + encodedKey);
        } catch (JSONException ex) {
            Log.e(TAG, "saveCustomTemplate error", ex);
        } catch (Exception ex) {
            Log.w(TAG, "saveCustomTemplate failed", ex);
        }
    }

    private static JSONArray timeSlotsToJsonArray(List<TimeSlot> slots) {
        JSONArray arr = new JSONArray();
        if (slots != null) {
            for (TimeSlot ts : slots) {
                try {
                    JSONObject tsObj = new JSONObject();
                    tsObj.put("start", ts.getStartTime() == null ? "" : ts.getStartTime());
                    tsObj.put("end", ts.getEndTime() == null ? "" : ts.getEndTime());
                    tsObj.put("activity", ts.getActivityName() == null ? "" : ts.getActivityName());
                    arr.put(tsObj);
                } catch (JSONException ignore) { }
            }
        }
        return arr;
    }

    /**
     * Load by the full SharedPreferences key (supports encoded storage key and legacy raw keys).
     * Ensures weeklyActivities is returned as a LinkedHashMap following PREFERRED_DAY_ORDER when possible.
     */
    public static DetailedSchedule loadCustomTemplateByPrefKey(Context ctx, String prefKey) {
        if (ctx == null || prefKey == null) return null;
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(prefKey, null);
            if (json == null) return null;

            JSONObject root = new JSONObject(json);
            String ttitle = root.optString("title", null);
            String desc = root.optString("description", "");

            List<String> tags = new ArrayList<>();
            JSONArray tagsArr = root.optJSONArray("tags");
            if (tagsArr != null) {
                for (int i = 0; i < tagsArr.length(); i++) tags.add(tagsArr.optString(i));
            }

            // Build weekly activities into LinkedHashMap to preserve desired order
            LinkedHashMap<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();
            JSONObject weeklyObj = root.optJSONObject("weekly");
            if (weeklyObj != null) {
                // First, add preferred days if present
                for (String day : PREFERRED_DAY_ORDER) {
                    if (weeklyObj.has(day)) {
                        JSONArray arr = weeklyObj.optJSONArray(day);
                        weeklyActivities.put(day, jsonArrayToTimeSlots(arr));
                    }
                }
                // Then add any remaining keys as they appear in names() if available
                JSONArray names = weeklyObj.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String dayKey = names.optString(i);
                        if (dayKey == null || dayKey.isEmpty()) continue;
                        if (weeklyActivities.containsKey(dayKey)) continue;
                        JSONArray arr = weeklyObj.optJSONArray(dayKey);
                        weeklyActivities.put(dayKey, jsonArrayToTimeSlots(arr));
                    }
                } else {
                    // Fallback: iterate keys via iterating object (older Android may not preserve order)
                    // but still add keys not in preferred order
                    @SuppressWarnings("unchecked")
                    java.util.Iterator<String> it = weeklyObj.keys();
                    while (it.hasNext()) {
                        String dayKey = it.next();
                        if (weeklyActivities.containsKey(dayKey)) continue;
                        JSONArray arr = weeklyObj.optJSONArray(dayKey);
                        weeklyActivities.put(dayKey, jsonArrayToTimeSlots(arr));
                    }
                }
            }

            // If JSON didn't contain title, derive from prefKey (decode)
            if (ttitle == null || ttitle.trim().isEmpty()) {
                String keyPart = prefKey.substring(CUSTOM_TEMPLATE_PREFIX.length());
                ttitle = decodeTitleFromKeyPart(keyPart);
            }

            return new DetailedSchedule(ttitle, desc, tags, weeklyActivities);
        } catch (JSONException ex) {
            Log.e(TAG, "loadCustomTemplateByPrefKey parse error", ex);
            return null;
        } catch (Exception ex) {
            Log.w(TAG, "loadCustomTemplateByPrefKey failed", ex);
            return null;
        }
    }

    private static List<TimeSlot> jsonArrayToTimeSlots(JSONArray arr) {
        List<TimeSlot> slots = new ArrayList<>();
        if (arr == null) return slots;
        for (int j = 0; j < arr.length(); j++) {
            JSONObject tsObj = arr.optJSONObject(j);
            if (tsObj == null) continue;
            String s = tsObj.optString("start", "");
            String e = tsObj.optString("end", "");
            String a = tsObj.optString("activity", "");
            slots.add(new TimeSlot(s, e, a));
        }
        return slots;
    }

    // ---------- Placeholder factory methods ----------
    // The real project already has these methods (createNightOwlTemplate, ...)
    // They are referenced above in getSampleTemplates(). If you want, I can also:
    // - modify each createXTemplate() to ensure they use LinkedHashMap and defensive copies,
    // - or I can edit them in a follow-up message if you paste their current implementations.
    //
    // For now we assume those factory methods exist elsewhere in the codebase.

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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("02:00", "07:30", "Đi ngủ")
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
                new TimeSlot("22:00", "24:00", "Đi ngủ")
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
                new TimeSlot("22:00", "05:00", "Đi ngủ")
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
                new TimeSlot("22:00", "05:00", "Đi ngủ")
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
                new TimeSlot("22:00", "05:00", "Đi ngủ")
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
                new TimeSlot("22:30", "06:00", "Đi ngủ")
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
                new TimeSlot("22:30", "06:00", "Đi ngủ")
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
                new TimeSlot("22:30", "06:00", "Đi ngủ")
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
                new TimeSlot("22:00", "05:00", "Đi ngủ")
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
                new TimeSlot("22:00", "05:00", "Đi ngủ")
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
                new TimeSlot("22:00", "05:00", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }


    // 5. Template "Tối ưu hóa chu kỳ ngủ" -
    public static DetailedSchedule createSprintWeekTemplate() {
        String title = "Tối ưu hóa chu kỳ ngủ";
        String description = "Template này dành cho sinh viên cần tối đa hóa thời gian thức trong tuần cho các dự án khẩn cấp hoặc ôn thi nước rút, và chấp nhận dùng cuối tuần để \"ngủ bù\" phục hồi.";
        List<String> tags = Arrays.asList("4h_sleep", "8h_study", "30m_sport", "30m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // Lịch trình chung từ Thứ 2 đến Thứ 5
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "05:30", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("05:30", "08:00", "Học Sâu Cường độ cao (Môn khó)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Học nhẹ"),
                new TimeSlot("09:00", "11:30", "Tự học (Làm bài tập)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "17:30", "Học trên trường / Tự học"),
                new TimeSlot("17:30", "19:00", "Ngủ để phục hồi năng lượng (90 phút)"),
                new TimeSlot("19:00", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "20:30", "Thể thao nhẹ (30p) & Giải trí (30p)"),
                new TimeSlot("21:00", "00:00", "Học Sâu Tối đa (3 giờ)"),
                new TimeSlot("00:00", "01:00", "Tổng hợp bài học & Chuẩn bị đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);

        // Thứ 6 - Lịch trình có thay đổi nhỏ
        List<TimeSlot> friday = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:00", "05:30", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("05:30", "08:00", "Học Sâu Cường độ cao (Môn khó)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Học nhẹ"),
                new TimeSlot("09:00", "11:30", "Tự học (Làm bài tập)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "17:30", "Học trên trường / Tự học"),
                new TimeSlot("17:30", "19:00", "Ngủ để phục hồi năng lượng (90 phút)"),
                new TimeSlot("19:00", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "20:30", "Giải trí (60p)"), // Khác biệt
                new TimeSlot("21:00", "00:00", "Học Sâu Tối đa (3 giờ)"),
                new TimeSlot("00:00", "01:00", "Tổng hợp bài học & Chuẩn bị đi ngủ")
        ));
        weeklyActivities.put("Thứ 6", friday);

        // Thứ 7 - Ngủ bù và học tập
        List<TimeSlot> saturday = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "09:00", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("09:00", "11:30", "Tự học (Ôn tập/Học bù)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "17:30", "Tự học (Làm dự án)"),
                new TimeSlot("17:30", "19:00", "Thể thao (60-90p)"),
                new TimeSlot("19:00", "20:30", "Ăn tối, Thư giãn & Giải trí"),
                new TimeSlot("20:30", "22:00", "Học nhẹ / Đọc sách"),
                new TimeSlot("22:00", "08:00", "Bắt đầu ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturday);

        // Chủ Nhật - Ngủ bù và phục hồi
        List<TimeSlot> sunday = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "09:00", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("09:00", "11:30", "Giải trí / Hoạt động xã hội"),
                new TimeSlot("11:30", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "17:30", "Tự học nhẹ / Hoạt động xã hội"),
                new TimeSlot("17:30", "19:00", "Thể thao (60-90p)"),
                new TimeSlot("19:00", "20:30", "Ăn tối, Thư giãn & Giải trí"),
                new TimeSlot("20:30", "22:00", "Chuẩn bị cho tuần mới"),
                new TimeSlot("22:00", "08:00", "Bắt đầu ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sunday);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 6. Template "Vừa học vừa ăn gián đoạn 16-8"
    public static DetailedSchedule createIntermittentFastingTemplate() {
        String title = "Vừa học vừa ăn gián đoạn 16-8";
        String description = "Giúp kết hợp phương pháp nhịn ăn gián đoạn 16-8, tối ưu hóa sự tập trung buổi sáng và đảm bảo phục hồi đầy đủ mà vẫn có thời gian cho thể thao.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung từ Thứ 2 đến Thứ 7 ---
        List<TimeSlot> commonSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, Vệ sinh, Uống nước (Nhịn ăn)"),
                new TimeSlot("08:00", "09:00", "Học môn cần tập trung cao"),
                new TimeSlot("09:00", "11:30", "Tự học/Nghiên cứu"),
                new TimeSlot("11:30", "13:30", "BỮA ĂN ĐẦU TIÊN (11:30) & Nghỉ trưa"),
                new TimeSlot("13:30", "17:30", "Học trên trường / Tự học"),
                new TimeSlot("17:30", "19:00", "Thể thao (60-90p) (Tập trước bữa tối)"),
                new TimeSlot("19:00", "20:00", "BỮA ĂN CUỐI NGÀY (19:30)"),
                new TimeSlot("20:00", "22:00", "Giải trí/Sở thích"),
                new TimeSlot("22:00", "23:00", "Đọc sách/Thư giãn nhẹ & Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", commonSchedule);
        weeklyActivities.put("Thứ 3", commonSchedule);
        weeklyActivities.put("Thứ 4", commonSchedule);
        weeklyActivities.put("Thứ 5", commonSchedule);
        weeklyActivities.put("Thứ 6", commonSchedule);
        weeklyActivities.put("Thứ 7", commonSchedule);

        // --- Lịch trình riêng cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, Vệ sinh, Uống nước"),
                new TimeSlot("08:00", "09:00", "Thể thao buổi sáng"),
                new TimeSlot("09:00", "11:30", "Tự học/Dự án"),
                new TimeSlot("11:30", "13:30", "BỮA ĂN ĐẦU TIÊN (11:30) & Nghỉ trưa"),
                new TimeSlot("13:30", "17:30", "Lập Kế hoạch Tuần mới & Thư giãn"),
                new TimeSlot("17:30", "19:00", "Ăn vặt nhẹ (Trong khung giờ ăn)"),
                new TimeSlot("19:00", "20:00", "BỮA ĂN CUỐI NGÀY (19:30)"),
                new TimeSlot("20:00", "22:00", "Giải trí/Sở thích"),
                new TimeSlot("22:00", "23:00", "Đọc sách/Thư giãn nhẹ & Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }


    // 7. Template "Vừa học vừa làm"
    public static DetailedSchedule createWorkAndLearnTemplate() {
        String title = "Vừa học vừa làm";
        String description = "Template này dành cho sinh viên có quỹ thời gian hạn chế, muốn \"học ra học, làm ra làm\", tối ưu hóa 2 giờ học và dành thời gian cho các hoạt động ngoại khóa, làm thêm và kỹ năng.";
        List<String> tags = Arrays.asList("8h_sleep", "4h_study", "30m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung từ Thứ 2 đến Thứ 7 ---
        List<TimeSlot> commonSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, Vệ sinh, Ăn sáng, Vận động nhẹ"),
                new TimeSlot("08:00", "11:30", "Học trên trường/ Làm thêm"),
                new TimeSlot("11:30", "13:30", "Ăn trưa & Nghỉ trưa"),
                new TimeSlot("13:30", "18:00", "Học trên trường / Làm thêm / CLB"),
                new TimeSlot("18:00", "19:00", "Thể thao & Ăn tối"),
                new TimeSlot("19:00", "20:00", "Làm việc khác & Thư giãn"),
                new TimeSlot("20:00", "21:30", "Học ngoại ngữ"),
                new TimeSlot("21:30", "23:30", "HỌC SÂU 2H (Môn trên trường)"),
                new TimeSlot("23:30", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", commonSchedule);
        weeklyActivities.put("Thứ 3", commonSchedule);
        weeklyActivities.put("Thứ 4", commonSchedule);
        weeklyActivities.put("Thứ 5", commonSchedule);
        weeklyActivities.put("Thứ 6", commonSchedule);
        weeklyActivities.put("Thứ 7", commonSchedule);

        // --- Lịch trình riêng cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, Vệ sinh, Ăn sáng, Vận động nhẹ"),
                new TimeSlot("08:00", "11:30", "Giải trí/Sở thích"),
                new TimeSlot("11:30", "13:30", "Ăn trưa & Nghỉ trưa"),
                new TimeSlot("13:30", "18:00", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("18:00", "19:00", "Thể thao & Ăn tối"),
                new TimeSlot("19:00", "20:00", "Làm việc khác & Thư giãn"),
                new TimeSlot("20:00", "21:30", "Học ngoại ngữ"),
                new TimeSlot("21:30", "23:30", "Giải trí/Sở thích"),
                new TimeSlot("23:30", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 8. Template "Năng suất theo khoa học"
    public static DetailedSchedule createScientificProductivityTemplate() {
        String title = "Năng suất theo khoa học";
        String description = "Template này tối ưu hóa gần 10 giờ học tập năng suất mỗi ngày (kết hợp Tự học + Học trên trường) và cân bằng với thể thao, sở thích cá nhân.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung (Thứ 2, 5, 7, Chủ Nhật) ---
        List<TimeSlot> deepWorkMorningSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "05:45", "Thức dậy, Vệ sinh, Giãn cơ, Uống nước"),
                new TimeSlot("05:45", "06:15", "Đọc sách / Học từ vựng"),
                new TimeSlot("06:15", "07:00", "Ăn sáng & thể thao nhẹ"),
                new TimeSlot("07:00", "11:30", "TỰ HỌC SÂU (Sáng) (4.5h)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa, Ngủ trưa (30p), Thư giãn"),
                new TimeSlot("13:30", "17:00", "Học trên trường / Tự học"),
                new TimeSlot("17:00", "18:30", "Đi dạo / Chạy bộ"),
                new TimeSlot("18:30", "19:30", "Nấu cơm, Ăn tối, Dọn dẹp"),
                new TimeSlot("19:30", "21:30", "Tổng ôn kiến thức đã học trong ngày"),
                new TimeSlot("21:30", "22:30", "Sở thích cá nhân"),
                new TimeSlot("22:30", "23:00", "Skincare & Chuẩn bị ngủ"),
                new TimeSlot("23:00", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", deepWorkMorningSchedule); // Thứ 2 có lịch Pomodoro, nhưng cấu trúc chung giống
        weeklyActivities.put("Thứ 5", deepWorkMorningSchedule);
        weeklyActivities.put("Thứ 7", deepWorkMorningSchedule);
        weeklyActivities.put("Chủ Nhật", deepWorkMorningSchedule);


        // --- Lịch trình cho các ngày học chính trên trường (Thứ 3, 4, 6) ---
        List<TimeSlot> onCampusSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "05:45", "Thức dậy, Vệ sinh, Giãn cơ"),
                new TimeSlot("05:45", "06:15", "Đọc sách / Học từ vựng"),
                new TimeSlot("06:15", "07:00", "Ăn sáng & thể thao nhẹ"),
                new TimeSlot("07:00", "11:30", "Học trên trường (Tiết 1-3) & Tự học (9:10-11:40)"),
                new TimeSlot("11:30", "13:30", "Ăn trưa, Ngủ trưa (30p)"),
                new TimeSlot("13:30", "17:00", "Học trên trường / Tự học"),
                new TimeSlot("17:00", "18:30", "Đi dạo / Chạy bộ"),
                new TimeSlot("18:30", "19:30", "Nấu cơm, Ăn tối, Dọn dẹp"),
                new TimeSlot("19:30", "21:30", "Tổng ôn kiến thức đã học trong ngày"),
                new TimeSlot("21:30", "22:30", "Sở thích cá nhân"),
                new TimeSlot("22:30", "23:00", "Skincare & Chuẩn bị ngủ"),
                new TimeSlot("23:00", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 3", onCampusSchedule);
        weeklyActivities.put("Thứ 4", onCampusSchedule);
        weeklyActivities.put("Thứ 6", onCampusSchedule);


        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    public static DetailedSchedule createUncleHoLifestyleTemplate() {
        String title = "Lịch sinh hoạt của Bác Hồ";
        String description = "Dành cho những sinh viên muốn rèn luyện một lối sống kỷ luật, tập trung cao độ vào công việc và học tập, đồng thời duy trì sức khỏe và sự phát triển cá nhân một cách bền bỉ.";
        List<String> tags = Arrays.asList("8h_sleep", "8h_study", "30m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new HashMap<>();

        // --- Lịch trình chung từ Thứ 2 đến Thứ 6 ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ (Đi bộ/Thể dục)"),
                new TimeSlot("06:00", "06:30", "Ăn sáng"),
                new TimeSlot("06:30", "10:30", "Học tập/Làm việc"),
                new TimeSlot("10:30", "11:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("11:30", "17:30", "Học tập/Làm việc"),
                new TimeSlot("17:30", "18:30", "Ăn tối & Dọn dẹp"),
                new TimeSlot("18:30", "21:30", "Học ngoại ngữ / Làm việc khác"),
                new TimeSlot("21:30", "23:00", "Đọc sách / Phát triển bản thân"),
                new TimeSlot("23:00", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình cho Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("06:00", "06:30", "Ăn sáng"),
                new TimeSlot("06:30", "10:30", "Tự học chuyên sâu"),
                new TimeSlot("10:30", "11:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("11:30", "17:30", "Tự học/Dự án cá nhân"),
                new TimeSlot("17:30", "18:30", "Ăn tối & Dọn dẹp"),
                new TimeSlot("18:30", "21:30", "Học ngoại ngữ / Làm việc khác"),
                new TimeSlot("21:30", "23:00", "Đọc sách / Phát triển bản thân"),
                new TimeSlot("23:00", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình cho Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Vận động nhẹ"),
                new TimeSlot("06:00", "06:30", "Ăn sáng"),
                new TimeSlot("06:30", "10:30", "Làm việc nhà/Hoạt động xã hội"),
                new TimeSlot("10:30", "11:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("11:30", "17:30", "Hoạt động gia đình/Bạn bè"),
                new TimeSlot("17:30", "18:30", "Ăn tối & Dọn dẹp"),
                new TimeSlot("18:30", "21:30", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("21:30", "23:00", "Đọc sách / Phát triển bản thân"),
                new TimeSlot("23:00", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 10. Template "Lịch tự học tiếng Anh"
    public static DetailedSchedule createEnglishSelfStudyTemplate() {
        String title = "Lịch tự học tiếng Anh";
        String description = "Ưu tiên việc học Tiếng Anh như một \"môn học chính\", chia đều các kỹ năng trong tuần và dành cuối tuần để luyện đề tổng hợp.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order

        // --- Lịch trình chung trong tuần (Thứ 2, 4, 6) ---
        List<TimeSlot> weekdaySchedule1 = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "08:30", "Học Sâu (1h): Từ vựng & Ngữ pháp"),
                new TimeSlot("08:30", "11:30", "Học trên trường/Làm việc khác (Linh hoạt)"),
                new TimeSlot("11:30", "13:00", "Ăn trưa & Nghỉ ngơi/Ngủ trưa"),
                new TimeSlot("13:00", "15:00", "Kỹ Năng Đọc (Reading) (2h)"),
                new TimeSlot("15:00", "17:00", "Học trên trường/Làm việc khác (Linh hoạt)"),
                new TimeSlot("17:00", "18:00", "Thể thao (60 phút)"),
                new TimeSlot("18:00", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Kỹ Năng Nghe (Listening) (1.5h)"),
                new TimeSlot("21:00", "22:30", "Học trên trường/Làm việc khác"),
                new TimeSlot("22:30", "23:00", "Ôn lại Từ vựng (15p) & Chuẩn bị ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 2", weekdaySchedule1);

        // --- Lịch trình cho Thứ 3, 5 ---
        List<TimeSlot> weekdaySchedule2 = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "08:30", "Học Sâu (1h): Từ vựng & Ngữ pháp"),
                new TimeSlot("08:30", "11:30", "Học trên trường/Làm việc khác (Linh hoạt)"),
                new TimeSlot("11:30", "13:00", "Ăn trưa & Nghỉ ngơi/Ngủ trưa"),
                new TimeSlot("13:00", "15:00", "Kỹ Năng Viết (Writing) (2h)"),
                new TimeSlot("15:00", "17:00", "Học trên trường/Làm việc khác (Linh hoạt)"),
                new TimeSlot("17:00", "18:00", "Thể thao (60 phút)"),
                new TimeSlot("18:00", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Kỹ Năng Nói (Speaking) (1.5h)"),
                new TimeSlot("21:00", "22:30", "Học trên trường/Làm việc khác"),
                new TimeSlot("22:30", "23:00", "Ôn lại Từ vựng (15p) & Chuẩn bị ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 3", weekdaySchedule2);
        weeklyActivities.put("Thứ 4", weekdaySchedule1); // Re-use
        weeklyActivities.put("Thứ 5", weekdaySchedule2); // Re-use
        weeklyActivities.put("Thứ 6", weekdaySchedule1); // Re-use

        // --- Lịch trình cho Thứ 7 (Luyện Đề) ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "08:30", "Học Sâu (1h): Từ vựng & Ngữ pháp"),
                new TimeSlot("08:30", "11:30", "THI THỬ (3h) (Full Mock Test Reading & Listening)"),
                new TimeSlot("11:30", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "15:00", "Kỹ Năng Nói (Speaking) (Luyện với bạn/tutor)"),
                new TimeSlot("15:00", "17:00", "Kỹ Năng Viết (Writing) (Viết Task 2)"),
                new TimeSlot("17:00", "18:00", "Thể thao (60 phút)"),
                new TimeSlot("18:00", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Nghe Thụ Động (Xem phim T.Anh)"),
                new TimeSlot("21:00", "22:30", "Giải trí"),
                new TimeSlot("22:30", "23:00", "Ôn lại Từ vựng (15p) & Chuẩn bị ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình cho Chủ Nhật (Tổng Kết) ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "08:30", "Nghe Thụ Động (Podcast/News)"),
                new TimeSlot("08:30", "11:30", "HỌC SÂU (3h) (Sửa lỗi sai T7, học từ vựng)"),
                new TimeSlot("11:30", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "15:00", "Lập Kế hoạch T.Anh Tuần Mới"),
                new TimeSlot("15:00", "17:00", "Giải trí/Thư giãn"),
                new TimeSlot("17:00", "18:00", "Thể thao (60 phút)"),
                new TimeSlot("18:00", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Tổng kết tuần (Học môn khác)"),
                new TimeSlot("21:00", "22:30", "Giải trí"),
                new TimeSlot("22:30", "23:00", "Ôn lại Từ vựng (15p) & Chuẩn bị ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 11. Template "1 tuần theo phương pháp 5-1-1"
    public static DetailedSchedule createFiveOneOneMethodTemplate() {
        String title = "1 tuần theo phương pháp 5-1-1";
        String description = "Dành cho người muốn tối đa hóa 7 ngày/tuần:\n5 ngày học tập/làm việc cường độ cao\n1 ngày nghỉ ngơi hoàn toàn\n1 ngày lập kế hoạch/phát triển cá nhân.";
        List<String> tags = Arrays.asList("8h_sleep", "8h_study", "60m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình 5 ngày học/làm cường độ cao (Thứ 2 -> Thứ 6) ---
        List<TimeSlot> highIntensityDay = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, vệ sinh, Ăn sáng"),
                new TimeSlot("08:00", "12:00", "HỌC TẬP CƯỜNG ĐỘ CAO (Ca 1 - 4h)"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Nghỉ ngơi/Ngủ trưa (90p)"),
                new TimeSlot("13:30", "17:30", "HỌC TẬP CƯỜNG ĐỘ CAO (Ca 2 - 4h)"),
                new TimeSlot("17:30", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:00", "Thư giãn (90p)"),
                new TimeSlot("21:00", "22:30", "Học nhẹ/Làm việc khác (Linh hoạt)"),
                new TimeSlot("22:30", "23:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ (8h)")
        ));

        // Tinh chỉnh cho Thứ 2
        List<TimeSlot> mondaySchedule = new ArrayList<>(highIntensityDay);
        mondaySchedule.set(1, new TimeSlot("08:00", "12:00", "HỌC TẬP CƯỜNG ĐỘ CAO (Ca 1 - 4h) (Pomodoro)"));
        mondaySchedule.set(4, new TimeSlot("17:30", "18:30", "Thể thao (60p) (Giải tỏa căng thẳng)"));
        mondaySchedule.set(6, new TimeSlot("19:30", "21:00", "Thư giãn (90p) (Sở thích, Gia đình)"));
        weeklyActivities.put("Thứ 2", mondaySchedule);

        weeklyActivities.put("Thứ 3", highIntensityDay);
        weeklyActivities.put("Thứ 4", highIntensityDay);
        weeklyActivities.put("Thứ 5", highIntensityDay);
        weeklyActivities.put("Thứ 6", highIntensityDay);

        // --- Lịch trình ngày nghỉ ngơi (Thứ 7) ---
        List<TimeSlot> restDay = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "08:00", "Dậy tự nhiên (Không báo thức), Ăn sáng"),
                new TimeSlot("08:00", "12:00", "GIẢI TRÍ TỐI ĐA (Xem phim, chơi game, sở thích)"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Thư giãn"),
                new TimeSlot("13:30", "17:30", "HOẠT ĐỘNG XÃ HỘI (Gặp gỡ bạn bè, gia đình)"),
                new TimeSlot("17:30", "18:30", "Thể thao nhẹ/Đi dạo (60p)"),
                new TimeSlot("18:30", "19:30", "Ăn tối & Thư giãn"),
                new TimeSlot("19:30", "21:00", "Giải trí (Tiếp)"),
                new TimeSlot("21:00", "22:30", "Giải trí (Tiếp)"),
                new TimeSlot("22:30", "23:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "08:00", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Thứ 7", restDay);

        // --- Lịch trình ngày lập kế hoạch (Chủ Nhật) ---
        List<TimeSlot> planDay = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, vệ sinh, Ăn sáng"),
                new TimeSlot("08:00", "12:00", "Lập Kế hoạch Tuần mới (2h) & Phát triển Cá nhân (2h)"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "17:30", "Hoạt động Xã hội/Gia đình (Linh hoạt)"),
                new TimeSlot("17:30", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:00", "Thư giãn (90p)"),
                new TimeSlot("21:00", "22:30", "Chuẩn bị cho Tuần mới (Soạn tài liệu, sách vở)"),
                new TimeSlot("22:30", "23:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Chủ Nhật", planDay);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 12. Template "Người đi làm"
    public static DetailedSchedule createWorkingProfessionalTemplate() {
        String title = "Người đi làm";
        String description = "Template tối ưu cho người đi làm 8 tiếng/ngày, cân bằng giữa công việc, học thêm kỹ năng mới (ví dụ: ngoại ngữ, lập trình) vào buổi tối, thể thao và cuộc sống cá nhân.";
        List<String> tags = Arrays.asList("8h_sleep", "4h_study", "60m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình ngày làm việc trong tuần (Thứ 2 -> Thứ 6) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "08:00", "Di chuyển đi làm"),
                new TimeSlot("08:00", "12:00", "LÀM VIỆC (8 tiếng)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "17:00", "LÀM VIỆC (8 tiếng) (Tiếp)"),
                new TimeSlot("17:00", "18:30", "Di chuyển về & Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:30", "HỌC TẬP (Ca Tối - 2h)"),
                new TimeSlot("21:30", "23:00", "Thư giãn (90p)"),
                new TimeSlot("23:00", "06:30", "Đi ngủ (8h)")
        ));
        // Tinh chỉnh cho Thứ 2, vì có ghi chú (Gia đình, Sở thích)
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(8, new TimeSlot("21:30", "23:00", "Thư giãn (90p) (Gia đình, Sở thích)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình cuối tuần (Thứ 7 & Chủ Nhật) ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "12:00", "Học Kỹ năng (Ca 1 - 1.5h) (ví dụ: Ngoại ngữ) & Học Kỹ năng (Tiếp)"), // Gộp 2 dòng
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "17:00", "HỌC SÂU (Ca 2 - 4h) (Dự án, Luyện đề)"),
                new TimeSlot("17:00", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:30", "Thư giãn (90p) (Gặp gỡ bạn bè)"),
                new TimeSlot("21:30", "23:00", "Giải trí (Tiếp)"),
                new TimeSlot("23:00", "06:30", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "12:00", "Học Kỹ năng (Ca 1 - 1.5h) & Học Kỹ năng (Tiếp)"), // Gộp 2 dòng
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "17:00", "HỌC SÂU (Ca 2 - 4h) (Dự án, Sửa lỗi)"),
                new TimeSlot("17:00", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:30", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("21:30", "23:00", "Thư giãn (90p)"),
                new TimeSlot("23:00", "06:30", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    //13. Template "Học Sinh Cấp 2"
    public static DetailedSchedule createJuniorHighStudentTemplate() {
        String title = "Học sinh cấp 2";
        String description = "Lịch trình khoa học cho học sinh cấp 2, tập trung vào việc học trên trường, hoàn thành bài tập về nhà, phát triển thể chất và khám phá sở thích, đảm bảo ngủ đủ giấc.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "60m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình ngày đi học (Thứ 2 -> Thứ 6) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "12:00", "Học tại trường"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "16:30", "Học tại trường (Ca Chiều)"),
                new TimeSlot("16:30", "17:30", "Thể thao"),
                new TimeSlot("17:30", "19:00", "Tắm rửa & Ăn tối cùng gia đình"),
                new TimeSlot("19:00", "21:00", "Hoàn thành Bài tập (2h)"),
                new TimeSlot("21:00", "22:30", "Giải trí/Sở thích (90p)"),
                new TimeSlot("22:30", "06:30", "Đi ngủ (8h)")
        ));
        // Tinh chỉnh cho Thứ 2 vì có ghi chú
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(7, new TimeSlot("21:00", "22:30", "Giải trí/Sở thích (90p) (Đọc truyện, Âm nhạc)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("06:30", "07:30", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("07:30", "12:00", "Tự học/Học thêm (Ca Sáng) (Ôn tập kiến thức tuần)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "16:30", "Tự học/Học thêm (Ca Chiều) (Làm bài tập cuối tuần)"),
                new TimeSlot("16:30", "17:30", "Thể thao"),
                new TimeSlot("17:30", "19:00", "Tắm rửa & Ăn tối cùng gia đình"),
                new TimeSlot("19:00", "21:00", "Hoàn thành Bài tập (2h)"),
                new TimeSlot("21:00", "22:30", "Giải trí/Sở thích (90p)"),
                new TimeSlot("22:30", "06:30", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:30", "08:30", "Dậy tự nhiên, Ăn sáng"), // Thời gian dậy khác
                new TimeSlot("08:30", "12:00", "Hoạt động Ngoại khóa/Gia đình"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "16:30", "Hoạt động Gia đình/Bạn bè"),
                new TimeSlot("16:30", "17:30", "Thể thao"),
                new TimeSlot("17:30", "19:00", "Tắm rửa & Ăn tối cùng gia đình"),
                new TimeSlot("19:00", "21:00", "Chuẩn bị bài/sách vở cho tuần mới"),
                new TimeSlot("21:00", "22:30", "Giải trí/Sở thích (90p)"),
                new TimeSlot("22:30", "07:30", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 14. Template "Luyện Thi THPT cho học sinh cấp 3"
    public static DetailedSchedule createExamPrepTemplate() {
        String title = "Luyện Thi THPT cho học sinh cấp 3";
        String description = "Template cường độ cao dành cho học sinh lớp 12, tập trung tối đa thời gian cho 3-4 môn thi đại học, cân bằng thể thao tối thiểu để giữ sức bền ôn thi.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "30m_sport", "30m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình chung các ngày trong tuần (Thứ 2 -> Thứ 6) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh"),
                new TimeSlot("06:00", "07:30", "HỌC SÂU (Môn 1: Toán/Lý/Hóa)"),
                new TimeSlot("07:30", "08:00", "Ăn sáng & Chuẩn bị đi học"),
                new TimeSlot("08:00", "12:00", "Học tại trường/Học thêm"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Ngủ trưa"),
                new TimeSlot("13:30", "17:00", "Học tại trường/Học thêm"),
                new TimeSlot("17:00", "17:30", "Thể thao"),
                new TimeSlot("17:30", "19:00", "Di chuyển về, Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "23:00", "HỌC SÂU (Môn 2 & Môn 3)"),
                new TimeSlot("23:00", "23:30", "Thư giãn (30p) & Chuẩn bị đi ngủ"),
                new TimeSlot("23:30", "05:30", "Đi ngủ")
        ));

        // Tinh chỉnh cho Thứ 2
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(0, new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh (Không snooze)"));
        mondaySchedule.set(3, new TimeSlot("08:00", "12:00", "Học tại trường/Học thêm (Tập trung 100%)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh"),
                new TimeSlot("06:00", "07:30", "HỌC SÂU (Môn 1: Toán/Lý/Hóa)"),
                new TimeSlot("07:30", "08:00", "Ăn sáng"),
                new TimeSlot("08:00", "12:00", "LUYỆN ĐỀ THẬT (3.5h) (Tổ hợp 3 môn)"), // 8:00-11:30 is 3.5h
                new TimeSlot("12:00", "13:30", "Ăn trưa & Ngủ trưa"),
                new TimeSlot("13:30", "17:00", "HỌC SÂU (Ca 3 - 3.5h) (Môn 3: Anh/Văn)"),
                new TimeSlot("17:00", "17:30", "Thể thao"),
                new TimeSlot("17:30", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "23:00", "HỌC SÂU (Ca 4 - 3h) (Tổng kết, học thuộc)"), // 19:00-22:00 is 3h
                new TimeSlot("23:00", "23:30", "Thư giãn (30p) & Chuẩn bị đi ngủ"),
                new TimeSlot("23:30", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh"),
                new TimeSlot("06:00", "07:30", "HỌC SÂU (Ca 1 - 2h) (Môn 2)"), // 6:00-8:00 is 2h
                new TimeSlot("07:30", "08:00", "Ăn sáng"),
                new TimeSlot("08:00", "12:00", "HỌC SÂU (Ca 2 - 3.5h) (Sửa lỗi đề T7)"), // 8:00-11:30 is 3.5h
                new TimeSlot("12:00", "13:30", "Ăn trưa & Ngủ trưa"),
                new TimeSlot("13:30", "17:00", "HỌC SÂU (Ca 3 - 3.5h) (Tổng ôn lý thuyết)"),
                new TimeSlot("17:00", "17:30", "Thể thao"),
                new TimeSlot("17:30", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "23:00", "Tổng kết tuần & Lập kế hoạch tuần mới"),
                new TimeSlot("23:00", "23:30", "Thư giãn (30p) & Chuẩn bị đi ngủ"),
                new TimeSlot("23:30", "05:30", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }



    // 15. Template "Sáng Tạo"
    public static DetailedSchedule createCreativeFlowTemplate() {
        String title = "Sáng Tạo";
        String description = "Dành cho sinh viên làm đồ án, thiết kế, hoặc các môn sáng tạo. Ưu tiên các khối \"deep work\" (làm việc sâu) 3-4 tiếng không gián đoạn, thay vì học lặt vặt.";
        List<String> tags = Arrays.asList("8h_sleep", "6h_study", "30m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình chung các ngày trong tuần (Thứ 2 -> Thứ 6) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("08:00", "08:30", "Thể thao nhẹ (30p)"),
                new TimeSlot("08:30", "12:00", "DEEP WORK (Ca 1)"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "16:30", "DEEP WORK (Ca 2)"),
                new TimeSlot("16:30", "18:30", "Học trên trường/Làm việc khác"),
                new TimeSlot("18:30", "20:00", "Ăn tối & Thư giãn"),
                new TimeSlot("20:00", "22:30", "Thư giãn (90p) & Sở thích sáng tạo"),
                new TimeSlot("22:30", "23:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ (8h)")
        ));

        // Tinh chỉnh cho Thứ 2
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(1, new TimeSlot("08:00", "08:30", "Thể thao nhẹ (30p) (Đi bộ, Giãn cơ)"));
        mondaySchedule.set(2, new TimeSlot("08:30", "12:00", "DEEP WORK (Ca 1) (Thiết kế, Lập trình, Viết lách)"));
        mondaySchedule.set(3, new TimeSlot("12:00", "13:30", "Ăn trưa & Nghỉ ngơi (Nghe nhạc, Đi dạo)"));
        mondaySchedule.set(5, new TimeSlot("16:30", "18:30", "Học trên trường/Làm việc khác (Linh hoạt)"));
        mondaySchedule.set(7, new TimeSlot("20:00", "22:30", "Thư giãn (90p) & Sở thích sáng tạo (Đọc sách, Vẽ, Chơi nhạc)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:00", "08:00", "Thức dậy, Vệ sinh, Ăn sáng"),
                new TimeSlot("08:00", "08:30", "Thể thao nhẹ (30p)"),
                new TimeSlot("08:30", "12:00", "DEEP WORK (Ca 1)"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "16:30", "DEEP WORK (Ca 2)"),
                new TimeSlot("16:30", "18:30", "Hoạt động xã hội"),
                new TimeSlot("18:30", "20:00", "Ăn tối & Thư giãn"),
                new TimeSlot("20:00", "22:30", "Giải trí/Bạn bè"),
                new TimeSlot("22:30", "23:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "07:00", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("08:00", "09:00", "Dậy tự nhiên, Ăn sáng"),
                new TimeSlot("09:00", "09:30", "Thể thao nhẹ (30p)"),
                new TimeSlot("09:30", "12:00", "Giải trí/Sở thích"),
                new TimeSlot("12:00", "13:30", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:30", "16:30", "Lập Kế hoạch Tuần mới (Lên ý tưởng)"),
                new TimeSlot("16:30", "18:30", "Hoạt động xã hội/Gia đình"),
                new TimeSlot("18:30", "20:00", "Ăn tối & Thư giãn"),
                new TimeSlot("20:00", "22:30", "Thư giãn (90p)"),
                new TimeSlot("22:30", "23:00", "Chuẩn bị đi ngủ"),
                new TimeSlot("23:00", "08:00", "Đi ngủ (8h)")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 16. Template "Hồi sức chữa lành"
    public static DetailedSchedule createHealingRetreatTemplate() {
        String title = "Hồi sức chữa lành";
        String description = "Dành cho sinh viên đang trong giai đoạn kiệt sức (burnout). Ưu tiên tối đa việc phục hồi tinh thần, học tập ở mức tối thiểu (4h) và tập trung vào thể thao nhẹ, thiền định.";
        List<String> tags = Arrays.asList("8h_sleep", "4h_study", "60m_sport", "90m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình chung các ngày trong tuần (Thứ 2 -> Thứ 6) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:30", "08:30", "Thức dậy & Ăn sáng"),
                new TimeSlot("08:30", "10:00", "Thư giãn (Nghe nhạc, Thiền, Viết nhật ký)"),
                new TimeSlot("10:00", "12:00", "Học Nhẹ (Ca 1)"),
                new TimeSlot("12:00", "14:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("14:00", "16:00", "Học Nhẹ (Ca 2)"),
                new TimeSlot("16:00", "17:00", "Làm việc khác"),
                new TimeSlot("17:00", "18:00", "Thể thao Phục Hồi (Đi bộ, Yoga, Giãn cơ)"),
                new TimeSlot("18:00", "19:30", "Tắm rửa & Ăn tối"),
                new TimeSlot("19:30", "23:00", "THƯ GIÃN TUYỆT ĐỐI (Sở thích, Gia đình)"),
                new TimeSlot("23:00", "07:30", "Đi ngủ")
        ));

        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:30", "08:30", "Thức dậy & Ăn sáng"),
                new TimeSlot("08:30", "10:00", "Thư giãn (Nghe nhạc, Thiền, Viết nhật ký)"),
                new TimeSlot("10:00", "12:00", "Học Nhẹ (Ca 1)"),
                new TimeSlot("12:00", "14:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("14:00", "16:00", "Sở thích cá nhân"),
                new TimeSlot("16:00", "17:00", "Làm việc khác"),
                new TimeSlot("17:00", "18:00", "Thể thao Phục Hồi (Đi bộ, Yoga, Giãn cơ)"),
                new TimeSlot("18:00", "19:30", "Tắm rửa & Ăn tối"),
                new TimeSlot("19:30", "23:00", "THƯ GIÃN TUYỆT ĐỐI (Sở thích, Gia đình)"),
                new TimeSlot("23:00", "07:30", "Đi ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("07:30", "08:30", "Thức dậy & Ăn sáng"),
                new TimeSlot("08:30", "10:00", "Thư giãn (Nghe nhạc, Thiền, Viết nhật ký)"),
                new TimeSlot("10:00", "12:00", "Hoạt động Gia đình/Bạn bè"),
                new TimeSlot("12:00", "14:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("14:00", "16:00", "Sở thích cá nhân"),
                new TimeSlot("16:00", "17:00", "Làm việc khác"),
                new TimeSlot("17:00", "18:00", "Thể thao Phục Hồi (Đi bộ, Yoga, Giãn cơ)"),
                new TimeSlot("18:00", "19:30", "Tắm rửa & Ăn tối"),
                new TimeSlot("19:30", "23:00", "THƯ GIÃN TUYỆT ĐỐI (Sở thích, Gia đình)"),
                new TimeSlot("23:00", "07:30", "Đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }


    // 17. Template "Ngủ Đa Pha (Uberman)"
    public static DetailedSchedule createUbermanSleepTemplate() {
        String title = "Ngủ Đa Pha (Uberman)";
        String description = "Dựa trên phương pháp ngủ Uberman, chia 24 giờ thành các giấc ngủ ngắn (naps) 20-30 phút, không có giấc ngủ lõi, để tối đa hóa thời gian thức.";
        List<String> tags = Arrays.asList("4h_sleep", "8h_study", "30m_sport", "30m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình Thứ 2 -> Thứ 6 ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("00:00", "00:20", "NGỦ (Nap 1 - 20p)"),
                new TimeSlot("00:20", "04:00", "HỌC SÂU (Ca 1)"),
                new TimeSlot("04:00", "04:20", "NGỦ (Nap 2 - 20p)"),
                new TimeSlot("04:20", "08:00", "HỌC SÂU (Ca 2 - 3.5h)"),
                new TimeSlot("08:00", "08:20", "NGỦ (Nap 3 - 20p)"),
                new TimeSlot("08:20", "12:00", "Làm việc khác/Ăn (Bữa 1) / Thể thao"),
                new TimeSlot("12:00", "14:00", "NGỦ (Nap 4 - 20p) & Làm việc khác"),
                new TimeSlot("14:00", "16:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("16:00", "16:20", "NGỦ (Nap 5 - 20p)"),
                new TimeSlot("16:20", "20:00", "Làm việc khác/Ăn (Bữa 2)"),
                new TimeSlot("20:00", "20:20", "NGỦ (Nap 6 - 20p)"),
                new TimeSlot("20:20", "00:00", "HỌC TẬP (Ca 3) / Làm việc khác")
        ));
        weeklyActivities.put("Thứ 2", weekdaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);
        weeklyActivities.put("Thứ 6", weekdaySchedule);

        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("00:00", "00:20", "NGỦ (Nap 1 - 20p)"),
                new TimeSlot("00:20", "04:00", "HỌC SÂU (Ca 1)"),
                new TimeSlot("04:00", "04:20", "NGỦ (Nap 2 - 20p)"),
                new TimeSlot("04:20", "08:00", "HỌC SÂU (Ca 2 - 3.5h)"),
                new TimeSlot("08:00", "08:20", "NGỦ (Nap 3 - 20p)"),
                new TimeSlot("08:20", "12:00", "Làm việc khác/Ăn (Bữa 1) / Thể thao"),
                new TimeSlot("12:00", "14:00", "NGỦ (Nap 4 - 20p) & Làm việc khác"),
                new TimeSlot("14:00", "16:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("16:00", "16:20", "NGỦ (Nap 5 - 20p)"),
                new TimeSlot("16:20", "20:00", "Làm việc khác/Ăn (Bữa 2)"),
                new TimeSlot("20:00", "20:20", "NGỦ (Nap 6 - 20p)"),
                new TimeSlot("20:20", "00:00", "HỌC TẬP (Ca 3) / Làm việc khác")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("00:00", "00:20", "NGỦ (Nap 1 - 20p)"),
                new TimeSlot("00:20", "04:00", "HỌC SÂU (Ca 1)"),
                new TimeSlot("04:00", "04:20", "NGỦ (Nap 2 - 20p)"),
                new TimeSlot("04:20", "08:00", "HỌC SÂU (Ca 2 - 3.5h)"),
                new TimeSlot("08:00", "08:20", "NGỦ (Nap 3 - 20p)"),
                new TimeSlot("08:20", "12:00", "Hoạt động Xã hội/Gia đình"),
                new TimeSlot("12:00", "14:00", "NGỦ HỒI PHỤC (90 PHÚT) & Ăn trưa"),
                new TimeSlot("14:00", "16:00", "Giải trí/Sở thích"),
                new TimeSlot("16:00", "16:20", "(Thức) Thể thao (60p)"), // Note: The image implies no nap here.
                new TimeSlot("16:20", "20:00", "Thư giãn/Ăn tối (18:00)"),
                new TimeSlot("20:00", "20:20", "NGỦ (Nap 4 - 20p)"),
                new TimeSlot("20:20", "00:00", "Lập kế hoạch tuần (Chuẩn bị cho T2)")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 18. Template "Giấc Ngủ Siesta"
    public static DetailedSchedule createSiestaSleepTemplate() {
        String title = "Giấc Ngủ Siesta";
        String description = "Kết hợp giấc ngủ đêm ngắn (khoảng 5-6 tiếng) với một giấc ngủ trưa dài có chủ đích (khoảng 90 phút) để tối đa hóa sự tỉnh táo và hiệu suất học tập vào buổi chiều.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "60m_sport", "60m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình chung các ngày trong tuần (Thứ 2 -> Thứ 5) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("01:00", "05:30", "NGỦ LÕI (4.5h)"),
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Uống nước"),
                new TimeSlot("06:00", "08:00", "HỌC SÂU (Ca 1 - 2h)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Thư giãn"),
                new TimeSlot("09:00", "12:00", "Học trên trường/Tự học (Ca 2 - 3h)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa"),
                new TimeSlot("13:00", "14:30", "NGỦ SIESTA (90 PHÚT)"),
                new TimeSlot("14:30", "17:30", "HỌC SÂU (Ca 3 - 3h)"),
                new TimeSlot("17:30", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:30", "Học trên trường/Làm việc khác"),
                new TimeSlot("21:30", "22:30", "Thư giãn (60p)"),
                new TimeSlot("22:30", "00:30", "Học trên trường/Làm việc khác"),
                new TimeSlot("00:30", "01:00", "Chuẩn bị đi ngủ")
        ));

        // Tinh chỉnh cho Thứ 2
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(2, new TimeSlot("06:00", "08:00", "HỌC SÂU (Ca 1 - 2h) (Giờ vàng sáng sớm)"));
        mondaySchedule.set(7, new TimeSlot("14:30", "17:30", "HỌC SÂU (Ca 3 - 3h) (Tỉnh táo sau Siesta)"));
        mondaySchedule.set(11, new TimeSlot("21:30", "22:30", "Thư giãn (60p) (Sở thích)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);


        // --- Lịch trình Thứ 6 & Thứ 7 ---
        List<TimeSlot> lateWeekSchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("01:00", "05:30", "NGỦ LÕI (4.5h)"),
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Uống nước"),
                new TimeSlot("06:00", "08:00", "HỌC SÂU (Ca 1 - 2h)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Thư giãn"),
                new TimeSlot("09:00", "12:00", "Học trên trường/Tự học (Ca 2 - 3h)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa"),
                new TimeSlot("13:00", "14:30", "NGỦ SIESTA (90 PHÚT)"),
                new TimeSlot("14:30", "17:30", "HỌC SÂU (Ca 3 - 3h)"),
                new TimeSlot("17:30", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:30", "Hoạt động xã hội/Bạn bè"),
                new TimeSlot("21:30", "22:30", "Giải trí (Tiếp)"),
                new TimeSlot("22:30", "00:30", "Giải trí (Tiếp)"),
                new TimeSlot("00:30", "01:00", "Chuẩn bị đi ngủ")
        ));
        weeklyActivities.put("Thứ 6", lateWeekSchedule);
        weeklyActivities.put("Thứ 7", lateWeekSchedule);


        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("01:00", "05:30", "NGỦ LÕI (4.5h)"),
                new TimeSlot("05:30", "06:00", "Thức dậy, Vệ sinh, Uống nước"),
                new TimeSlot("06:00", "08:00", "HỌC SÂU (Ca 1 - 2h)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Thư giãn"),
                new TimeSlot("09:00", "12:00", "Làm việc khác/Giải trí"),
                new TimeSlot("12:00", "13:00", "Ăn trưa"),
                new TimeSlot("13:00", "14:30", "NGỦ SIESTA (90 PHÚT)"),
                new TimeSlot("14:30", "17:30", "Lập Kế hoạch Tuần mới (2h)"),
                new TimeSlot("17:30", "18:30", "Thể thao (60p)"),
                new TimeSlot("18:30", "19:30", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:30", "21:30", "Hoạt động xã hội/Gia đình"),
                new TimeSlot("21:30", "22:30", "Giải trí (Tiếp)"),
                new TimeSlot("22:30", "00:30", "Giải trí (Tiếp)"),
                new TimeSlot("00:30", "01:00", "Chuẩn bị đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 19. Template "Ngủ Lõi Đôi"
    public static DetailedSchedule createDualCoreSleepTemplate() {
        String title = "Ngủ Lõi Đôi";String description = "Dựa trên phương pháp ngủ Biphasic cường độ cao, ngủ một lõi chính 3 tiếng ban đêm và một giấc \"siêu phục hồi\" 90 phút buổi chiều, tổng cộng 4.5h ngủ.";
        List<String> tags = Arrays.asList("4h_sleep", "8h_study", "30m_sport", "30m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình chung các ngày trong tuần (Thứ 2 -> Thứ 5) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("02:00", "05:00", "NGỦ LÕI CHÍNH (3h)"),
                new TimeSlot("05:00", "05:30", "Thức dậy, Vệ sinh"),
                new TimeSlot("05:30", "08:00", "HỌC SÂU (Ca 1 - 2.5h)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Chuẩn bị"),
                new TimeSlot("09:00", "12:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("12:00", "13:00", "Ăn trưa"),
                new TimeSlot("13:00", "14:30", "NGỦ HỒI PHỤC (90 PHÚT)"),
                new TimeSlot("14:30", "17:30", "HỌC SÂU (Ca 2 - 3h)"),
                new TimeSlot("17:30", "18:00", "Thể thao (30p)"),
                new TimeSlot("18:00", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "21:30", "HỌC SÂU (Ca 3 - 2.5h)"),
                new TimeSlot("21:30", "22:00", "Thư giãn (30p)"),
                new TimeSlot("22:00", "01:30", "Học trên trường/Làm việc khác"),
                new TimeSlot("01:30", "02:00", "Chuẩn bị đi ngủ")
        ));

        // Tinh chỉnh cho Thứ 2
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(2, new TimeSlot("05:30", "08:00", "HỌC SÂU (Ca 1 - 2.5h) (Giờ vàng sáng sớm)"));
        mondaySchedule.set(4, new TimeSlot("09:00", "12:00", "Học trên trường/Làm việc khác (Linh hoạt)"));
        mondaySchedule.set(7, new TimeSlot("14:30", "17:30", "HỌC SÂU (Ca 2 - 3h) (Tỉnh táo sau ngủ)"));
        mondaySchedule.set(8, new TimeSlot("17:30", "18:00", "Thể thao (30p) (Vận động nhẹ)"));
        mondaySchedule.set(10, new TimeSlot("19:00", "21:30", "HỌC SÂU (Ca 3 - 2.5h) (Học môn thuộc lòng)"));
        mondaySchedule.set(12, new TimeSlot("22:00", "01:30", "Học trên trường/Làm việc khác (Linh hoạt)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);

        // --- Lịch trình Thứ 6 ---
        List<TimeSlot> fridaySchedule = new ArrayList<>(weekdaySchedule);
        fridaySchedule.set(12, new TimeSlot("22:00", "01:30", "Hoạt động xã hội/Giải trí"));
        weeklyActivities.put("Thứ 6", fridaySchedule);


        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("02:00", "05:00", "NGỦ LÕI CHÍNH (3h)"),
                new TimeSlot("05:00", "05:30", "Thức dậy, Vệ sinh"),
                new TimeSlot("05:30", "08:00", "HỌC SÂU (Ca 1 - 2.5h)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Chuẩn bị"),
                new TimeSlot("09:00", "12:00", "LUYỆN ĐỀ (3h)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa"),
                new TimeSlot("13:00", "14:30", "NGỦ HỒI PHỤC (90 PHÚT)"),
                new TimeSlot("14:30", "17:30", "SỬA ĐỀ (3h)"),
                new TimeSlot("17:30", "18:00", "Thể thao (30p)"),
                new TimeSlot("18:00", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "21:30", "Học nhẹ/Giải trí"),
                new TimeSlot("21:30", "22:00", "Thư giãn (30p)"),
                new TimeSlot("22:00", "01:30", "Giải trí"),
                new TimeSlot("01:30", "02:00", "Chuẩn bị đi ngủ")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("02:00", "05:00", "NGỦ LÕI CHÍNH (3h)"),
                new TimeSlot("05:00", "05:30", "Thức dậy, Vệ sinh"),
                new TimeSlot("05:30", "08:00", "HỌC SÂU (Ca 1 - 2.5h)"),
                new TimeSlot("08:00", "09:00", "Ăn sáng & Chuẩn bị"),
                new TimeSlot("09:00", "12:00", "Học nhẹ/Sở thích"),
                new TimeSlot("12:00", "13:00", "Ăn trưa"),
                new TimeSlot("13:00", "14:30", "NGỦ HỒI PHỤC (90 PHÚT)"),
                new TimeSlot("14:30", "17:30", "Lập Kế hoạch Tuần mới"),
                new TimeSlot("17:30", "18:00", "Thể thao (30p)"),
                new TimeSlot("18:00", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "21:30", "Giải trí (30p)"),
                new TimeSlot("21:30", "22:00", "Thư giãn (30p)"),
                new TimeSlot("22:00", "01:30", "Giải trí"),
                new TimeSlot("01:30", "02:00", "Chuẩn bị đi ngủ")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }

    // 20. Template "Vận động viên ham học"
    public static DetailedSchedule createStudentAthleteTemplate() {
        String title = "Vận động viên ham học";
        String description = "Lịch trình khắc nghiệt cho vận động viên/sinh viên thi đấu, cân bằng 8 giờ học tập và 90 phút tập luyện cường độ cao mỗi ngày, cắt giảm tối đa thời gian thư giãn.";
        List<String> tags = Arrays.asList("6h_sleep", "8h_study", "90m_sport", "30m_relax");
        Map<String, List<TimeSlot>> weeklyActivities = new LinkedHashMap<>();

        // --- Lịch trình chung các ngày trong tuần (Thứ 2 -> Thứ 5) ---
        List<TimeSlot> weekdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "08:00", "TỰ HỌC (Ca 1 - 2.5h)"),
                new TimeSlot("08:00", "08:30", "Ăn sáng & Chuẩn bị"),
                new TimeSlot("08:30", "12:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "16:00", "TỰ HỌC (Ca 2 - 3h)"),
                new TimeSlot("16:00", "17:30", "THỂ THAO (90p)"),
                new TimeSlot("17:30", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "21:30", "TỰ HỌC (Ca 3 - 2.5h)"),
                new TimeSlot("21:30", "23:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("23:00", "23:30", "Thư giãn (30p)"),
                new TimeSlot("23:30", "05:30", "Đi ngủ (6h)")
        ));

        // Tinh chỉnh cho Thứ 2
        List<TimeSlot> mondaySchedule = new ArrayList<>(weekdaySchedule);
        mondaySchedule.set(0, new TimeSlot("05:30", "08:00", "TỰ HỌC (Ca 1 - 2.5h) (Môn 1: Môn khó)"));
        mondaySchedule.set(2, new TimeSlot("08:30", "12:00", "Học trên trường/Làm việc khác (Linh hoạt)"));
        mondaySchedule.set(4, new TimeSlot("13:00", "16:00", "TỰ HỌC (Ca 2 - 3h) (Môn 2)"));
        mondaySchedule.set(5, new TimeSlot("16:00", "17:30", "THỂ THAO (90p) (Tập luyện cường độ cao)"));
        mondaySchedule.set(6, new TimeSlot("17:30", "19:00", "Tắm rửa, Ăn tối (Tập trung dinh dưỡng)"));
        mondaySchedule.set(7, new TimeSlot("19:00", "21:30", "TỰ HỌC (Ca 3 - 2.5h) (Môn 3)"));
        mondaySchedule.set(9, new TimeSlot("23:00", "23:30", "Thư giãn (30p) (Tối thiểu)"));

        weeklyActivities.put("Thứ 2", mondaySchedule);
        weeklyActivities.put("Thứ 3", weekdaySchedule);
        weeklyActivities.put("Thứ 4", weekdaySchedule);
        weeklyActivities.put("Thứ 5", weekdaySchedule);

        // --- Lịch trình Thứ 6 ---
        List<TimeSlot> fridaySchedule = new ArrayList<>(weekdaySchedule);
        fridaySchedule.set(8, new TimeSlot("21:30", "23:00", "Hoạt động xã hội (Giới hạn)"));
        weeklyActivities.put("Thứ 6", fridaySchedule);

        // --- Lịch trình Thứ 7 ---
        List<TimeSlot> saturdaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "08:00", "TỰ HỌC (Ca 1 - 2.5h)"),
                new TimeSlot("08:00", "08:30", "Ăn sáng & Chuẩn bị"),
                new TimeSlot("08:30", "12:00", "THI ĐẤU/LUYỆN TẬP NẶNG"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "16:00", "(Tiếp tục Thi đấu/Di chuyển)"),
                new TimeSlot("16:00", "17:30", "Thể thao nhẹ/Phục hồi (90p)"),
                new TimeSlot("17:30", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "21:30", "TỰ HỌC (Ca 3 - 2.5h) (Học nhẹ/Ôn bài)"),
                new TimeSlot("21:30", "23:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("23:00", "23:30", "Thư giãn (30p)"),
                new TimeSlot("23:30", "05:30", "Đi ngủ (6h)")
        ));
        weeklyActivities.put("Thứ 7", saturdaySchedule);

        // --- Lịch trình Chủ Nhật ---
        List<TimeSlot> sundaySchedule = new ArrayList<>(Arrays.asList(
                new TimeSlot("05:30", "08:00", "TỰ HỌC (Ca 1 - 2.5h)"),
                new TimeSlot("08:00", "08:30", "Ăn sáng & Chuẩn bị"),
                new TimeSlot("08:30", "12:00", "TỰ HỌC (Ca 2 - 3.5h)"),
                new TimeSlot("12:00", "13:00", "Ăn trưa & Nghỉ ngơi"),
                new TimeSlot("13:00", "16:00", "(Tiếp tục Ca 2) & Lập kế hoạch tuần"),
                new TimeSlot("16:00", "17:30", "Thể thao nhẹ/Phục hồi (90p)"),
                new TimeSlot("17:30", "19:00", "Tắm rửa, Ăn tối"),
                new TimeSlot("19:00", "21:30", "TỰ HỌC (Ca 3 - 2.5h) (Sửa lỗi/Ôn bài)"),
                new TimeSlot("21:30", "23:00", "Học trên trường/Làm việc khác"),
                new TimeSlot("23:00", "23:30", "Thư giãn (30p)"),
                new TimeSlot("23:30", "05:30", "Đi ngủ (6h)")
        ));
        weeklyActivities.put("Chủ Nhật", sundaySchedule);

        return new DetailedSchedule(title, description, tags, weeklyActivities);
    }
}