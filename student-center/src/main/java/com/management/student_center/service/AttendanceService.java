package com.management.student_center.service;

import com.management.student_center.dto.AttendanceResponseDTO;
import com.management.student_center.dto.AttendanceStudentDTO;
import com.management.student_center.dto.TodayAttendanceDTO;
import com.management.student_center.entity.*;
import com.management.student_center.repository.*;
import com.management.student_center.enums.ActivityActionType;
import com.management.student_center.enums.ActivityTargetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final SessionRepository sessionRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final AttendanceStudentRepository attendanceStudentRepository;
    private final StudentRepository studentRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;

    public AttendanceService(
            SessionRepository sessionRepository,
            StudentSubjectRepository studentSubjectRepository,
            AttendanceStudentRepository attendanceStudentRepository,
            StudentRepository studentRepository,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService
    ) {
        this.sessionRepository = sessionRepository;
        this.studentSubjectRepository = studentSubjectRepository;
        this.attendanceStudentRepository = attendanceStudentRepository;
        this.studentRepository = studentRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
    }
    
    public List<AttendanceStudentDTO> getAbsentOrLateStudentsInDateRange(LocalDate startDate, LocalDate endDate) {
        List<AttendanceStudent> records = attendanceStudentRepository.findAbsentOrLateBetweenDates(startDate, endDate);

        // Map sang DTO có thêm thông tin lớp học
        return records.stream()
                .map(a -> new AttendanceStudentDTO(
                        a.getStudent().getId(),
                        a.getStudent().getUserInfo().getFullName(),
                        a.getStudent().getUserInfo().getEmail(),
                        a.getStudent().getGrade(),
                        a.getStudent().getSchoolName(),
                        a.getStatus(),
                        a.getNote(),
                        a.getSession().getSessionDate(),
                        a.getSession().getSubject().getId(),
                        a.getSession().getSubject().getName()
                ))
                .collect(Collectors.toList());
    }
    
    public AttendanceResponseDTO getAttendanceBySubject(Long subjectId) {
        // 1. Lấy tất cả sessions
        List<Session> sessions = sessionRepository.findBySubject_IdOrderBySessionDateAsc(subjectId);
        
        if (sessions.isEmpty()) {
            AttendanceResponseDTO response = new AttendanceResponseDTO();
            response.setSubjectId(subjectId);
            response.setSessions(Collections.emptyList());
            response.setStudents(Collections.emptyList());
            return response;
        }
        
        // 2. Lấy TẤT CẢ student subjects (kể cả đã xóa)
        List<StudentSubject> allStudentSubjects = studentSubjectRepository.findBySubject_Id(subjectId);
        
        // 3. Group student subjects theo studentId, sắp xếp theo thời gian
        Map<Long, List<StudentSubject>> studentSubjectsByStudentId = allStudentSubjects.stream()
            .collect(Collectors.groupingBy(ss -> ss.getStudent().getId()));
        
        // 4. Với mỗi student, xử lý các giai đoạn đăng ký khác nhau
        Map<Long, Student> studentMap = new HashMap<>();
        Map<Long, List<StudentSubject>> studentPeriodsMap = new HashMap<>();
        
        for (Map.Entry<Long, List<StudentSubject>> entry : studentSubjectsByStudentId.entrySet()) {
            Long studentId = entry.getKey();
            List<StudentSubject> periods = entry.getValue();
            
            // Sắp xếp theo thời gian enrollmentDate
            periods.sort(Comparator.comparing(StudentSubject::getEnrollmentDate));
            
            // Lấy student từ bản ghi đầu tiên
            if (!periods.isEmpty()) {
                studentMap.put(studentId, periods.get(0).getStudent());
            }
            
            // Xử lý các giai đoạn: ghép các khoảng thời gian
            List<StudentSubject> mergedPeriods = new ArrayList<>();
            
            for (StudentSubject period : periods) {
                LocalDate start = period.getEnrollmentDate();
                LocalDate end = period.getDeletedAt() != null ? period.getDeletedAt() : LocalDate.MAX;
                
                // Nếu danh sách rỗng, thêm trực tiếp
                if (mergedPeriods.isEmpty()) {
                    mergedPeriods.add(period);
                    continue;
                }
                
                // Kiểm tra xem có overlap hoặc liền kề với period cuối không
                StudentSubject lastPeriod = mergedPeriods.get(mergedPeriods.size() - 1);
                LocalDate lastEnd = lastPeriod.getDeletedAt() != null ? lastPeriod.getDeletedAt() : LocalDate.MAX;
                
                // Nếu period mới bắt đầu trước hoặc ngay sau khi period cũ kết thúc
                if (!start.isAfter(lastEnd)) {
                    // Merge: kéo dài deletedAt nếu cần
                    if (end.isAfter(lastEnd)) {
                        if (lastPeriod.getDeletedAt() == null) {
                            // Không thể merge vì period cũ chưa kết thúc
                            mergedPeriods.add(period);
                        } else {
                            // Cập nhật deletedAt của period cũ
                            // Note: StudentSubject là entity, không nên modify trực tiếp trong logic này
                            // Tốt nhất là tạo DTO riêng cho period
                        }
                    }
                } else {
                    mergedPeriods.add(period);
                }
            }
            
            studentPeriodsMap.put(studentId, mergedPeriods);
        }
        
        // 5. Lấy tất cả attendance records
        List<AttendanceStudent> allAttendances = attendanceStudentRepository.findAllBySessionIn(sessions);
        Map<String, AttendanceStudent> attendanceMap = new HashMap<>();
        for (AttendanceStudent attendance : allAttendances) {
            String key = attendance.getStudent().getId() + "_" + attendance.getSession().getId();
            attendanceMap.put(key, attendance);
        }
        
        // 6. Xây dựng DTO cho từng học sinh
        List<AttendanceResponseDTO.StudentAttendanceDTO> studentDTOs = new ArrayList<>();
        
        for (Map.Entry<Long, List<StudentSubject>> entry : studentPeriodsMap.entrySet()) {
            Long studentId = entry.getKey();
            Student student = studentMap.get(studentId);
            List<StudentSubject> periods = entry.getValue();
            
            // Tìm period active tại mỗi session và xây dựng attendanceItems
            List<AttendanceResponseDTO.AttendanceItem> attendanceItems = new ArrayList<>();
            LocalDate currentEnrollmentDate = null;
            LocalDate currentDeletedAt = null;
            
            for (Session session : sessions) {
                Long sessionId = session.getId();
                LocalDate sessionDate = session.getSessionDate();
                
                // Tìm period phù hợp cho session này
                StudentSubject activePeriod = null;
                for (StudentSubject period : periods) {
                    LocalDate startDate = period.getEnrollmentDate();
                    LocalDate endDate = period.getDeletedAt() != null ? period.getDeletedAt() : LocalDate.MAX;
                    
                    if (!sessionDate.isBefore(startDate) && sessionDate.isBefore(endDate)) {
                        activePeriod = period;
                        break;
                    }
                }
                
                String key = studentId + "_" + sessionId;
                AttendanceStudent attendance = attendanceMap.get(key);
                
                if (activePeriod == null) {
                    // Không có period nào active ở session này
                    // Kiểm tra xem có period nào bắt đầu sau không
                    boolean hasFuturePeriod = periods.stream()
                        .anyMatch(p -> p.getEnrollmentDate().isAfter(sessionDate));
                    
                    if (hasFuturePeriod) {
                        attendanceItems.add(new AttendanceResponseDTO.AttendanceItem(
                            sessionId, "not_enrolled_yet", "Chưa đăng ký học tại thời điểm này"
                        ));
                    } else {
                        attendanceItems.add(new AttendanceResponseDTO.AttendanceItem(
                            sessionId, "completed", "Đã hoàn thành khóa học trước đó"
                        ));
                    }
                } else {
                    // Có period active
                    if (attendance != null) {
                        attendanceItems.add(new AttendanceResponseDTO.AttendanceItem(
                            sessionId, attendance.getStatus(), attendance.getNote()
                        ));
                    } else {
                        attendanceItems.add(new AttendanceResponseDTO.AttendanceItem(
                            sessionId, "pending", "Chưa điểm danh"
                        ));
                    }
                }
            }
            
            // Lấy enrollmentDate và deletedAt hiện tại (period cuối cùng)
            StudentSubject lastPeriod = periods.get(periods.size() - 1);
            LocalDate enrollmentDate = lastPeriod.getEnrollmentDate();
            LocalDate deletedAt = lastPeriod.getDeletedAt();
            
            AttendanceResponseDTO.StudentAttendanceDTO studentDTO = 
                new AttendanceResponseDTO.StudentAttendanceDTO(
                    studentId,
                    student.getUserInfo().getFullName(),
                    attendanceItems,
                    enrollmentDate,
                    deletedAt
                );
            
            studentDTOs.add(studentDTO);
        }
        
        // 7. Đóng gói response
        AttendanceResponseDTO response = new AttendanceResponseDTO();
        response.setSubjectId(subjectId);
        response.setSessions(sessions.stream()
            .map(s -> new AttendanceResponseDTO.SessionDTO(
                s.getId(), s.getSessionDate(), s.getStartTime(), s.getEndTime()))
            .collect(Collectors.toList()));
        response.setStudents(studentDTOs);
        
        return response;
    }

    @Transactional
    public String updateStatus(Long studentId, Long sessionId, String status) {
        // Validate status
        if (!Arrays.asList("present", "late", "absent").contains(status)) {
            throw new IllegalArgumentException("Status không hợp lệ. Chấp nhận: present, late, absent");
        }
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
        
        if ("canceled".equals(session.getStatus())) {
            throw new IllegalStateException("Không thể điểm danh cho buổi học đã bị hủy");
        }
        
        // Lấy user hiện tại đang thực hiện hành động
        User currentUser = currentUserService.getCurrentUser();
        
        String oldStatus = null;
        AttendanceStudent existing = attendanceStudentRepository
                .findBySessionAndStudent(session, student)
                .orElse(null);
        
        if (existing != null) {
            oldStatus = existing.getStatus();
            existing.setStatus(status);
            attendanceStudentRepository.save(existing);
        } else {
            AttendanceStudent newAttendance = new AttendanceStudent();
            newAttendance.setStudent(student);
            newAttendance.setSession(session);
            newAttendance.setStatus(status);
            attendanceStudentRepository.save(newAttendance);
        }
        
        updateSessionStatusBasedOnAttendance(session);
        
        // Ghi log điểm danh
        logAttendanceAction(
            currentUser,
            existing != null ? ActivityActionType.UPDATE : ActivityActionType.CREATE,
            student,
            session,
            status,
            oldStatus
        );
        
        return existing != null ? 
            "Cập nhật trạng thái điểm danh thành công" : 
            "Thêm mới trạng thái điểm danh thành công";
    }

    private void updateSessionStatusBasedOnAttendance(Session session) {
        if ("completed".equals(session.getStatus()) || "canceled".equals(session.getStatus())) {
            return;
        }
        
        List<AttendanceStudent> attendances = attendanceStudentRepository.findBySession(session);
        
        boolean hasPresentOrLate = attendances.stream()
                .anyMatch(a -> "present".equals(a.getStatus()) || "late".equals(a.getStatus()));
        
        if (hasPresentOrLate) {
            session.setStatus("completed");
            sessionRepository.save(session);
        }
    }

    // cập nhật ghi chú
    @Transactional
    public String updateNote(Long studentId, Long sessionId, String note) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        // Lấy user hiện tại đang thực hiện hành động
        User currentUser = currentUserService.getCurrentUser();
        
        AttendanceStudent existing = attendanceStudentRepository
                .findBySessionAndStudent(session, student)
                .orElse(null);
        
        String oldNote = null;
        if (existing != null) {
            oldNote = existing.getNote();
            existing.setNote(note);
            attendanceStudentRepository.save(existing);
        } else {
            AttendanceStudent newAttendance = new AttendanceStudent();
            newAttendance.setStudent(student);
            newAttendance.setSession(session);
            newAttendance.setNote(note);
            attendanceStudentRepository.save(newAttendance);
        }
        
        // Ghi log cập nhật ghi chú
        logNoteAction(
            currentUser,
            existing != null ? ActivityActionType.UPDATE : ActivityActionType.CREATE,
            student,
            session,
            note,
            oldNote
        );
        
        return existing != null ? "Cập nhật ghi chú thành công" : "Thêm mới ghi chú thành công";
    }
    
    @Transactional(readOnly = true)
    public TodayAttendanceDTO getAttendanceByDate(Long subjectId, LocalDate date) {

        // 1. tìm session theo ngày truyền vào
        Session session = sessionRepository
                .findBySubjectIdAndSessionDate(subjectId, date)
                .orElse(null);

        if (session == null) {
            return null; // hoặc throw exception
        }

        // 2. teacher attendance
        String teacherStatus = session.getTeacherAttendances()
                .stream()
                .findFirst()
                .map(TeacherAttendance::getStatus)
                .orElse("not_marked");

        // 3. student attendance
        List<AttendanceStudent> list = session.getAttendanceStudents();

        long total = list.size();
        long present = list.stream()
                .filter(a -> "present".equals(a.getStatus()))
                .count();

        long absent = list.stream()
                .filter(a -> "absent".equals(a.getStatus()))
                .count();

        long late = list.stream()
                .filter(a -> "late".equals(a.getStatus()))
                .count();

        // 4. map DTO
        TodayAttendanceDTO dto = new TodayAttendanceDTO();
        dto.setSessionId(session.getId());
        dto.setDate(date.toString()); // dùng date truyền vào
        dto.setTeacherStatus(teacherStatus);

        dto.setTotalStudents(total);
        dto.setPresentStudents(present);
        dto.setAbsentStudents(absent);
        dto.setLateStudents(late);

        return dto;
    }
    
    // =========================
    // LOGGING METHODS
    // =========================
    
    private void logAttendanceAction(
            User user,
            ActivityActionType actionType,
            Student student,
            Session session,
            String newStatus,
            String oldStatus
    ) {
        String statusText = getStatusVietnamese(newStatus);
        String actionText = actionType == ActivityActionType.CREATE ? "đã điểm danh" : "đã cập nhật điểm danh";
        
        String description = String.format(
            "%s học sinh %s với trạng thái: %s",
            actionText,
            student.getUserInfo().getFullName(),
            statusText
        );
        
        String meta = String.format(
            """
            {
                "student_id": %d,
                "student_name": "%s",
                "session_id": %d,
                "session_date": "%s",
                "subject": "%s",
                "new_status": "%s",
                "old_status": %s,
                "status_text": "%s"
            }
            """,
            student.getId(),
            student.getUserInfo().getFullName(),
            session.getId(),
            session.getSessionDate().toString(),
            session.getSubject() != null ? session.getSubject().getName() : "Unknown",
            newStatus,
            oldStatus != null ? "\"" + oldStatus + "\"" : "null",
            statusText
        );
        
        activityLogService.log(
            user,
            actionType,
            ActivityTargetType.STUDENT,
            student.getId(),
            description,
            meta
        );
    }
    
    private void logNoteAction(
            User user,
            ActivityActionType actionType,
            Student student,
            Session session,
            String newNote,
            String oldNote
    ) {
        String actionText = actionType == ActivityActionType.CREATE ? "đã thêm ghi chú" : "đã cập nhật ghi chú";
        
        String description = String.format(
            "%s cho học sinh %s: %s",
            actionText,
            student.getUserInfo().getFullName(),
            newNote != null && newNote.length() > 50 ? newNote.substring(0, 50) + "..." : newNote
        );
        
        String meta = String.format(
            """
            {
                "student_id": %d,
                "student_name": "%s",
                "session_id": %d,
                "session_date": "%s",
                "subject": "%s",
                "new_note": "%s",
                "old_note": %s
            }
            """,
            student.getId(),
            student.getUserInfo().getFullName(),
            session.getId(),
            session.getSessionDate().toString(),
            session.getSubject() != null ? session.getSubject().getName() : "Unknown",
            newNote != null ? escapeJson(newNote) : "",
            oldNote != null ? "\"" + escapeJson(oldNote) + "\"" : "null"
        );
        
        activityLogService.log(
            user,
            actionType,
            ActivityTargetType.STUDENT,
            student.getId(),
            description,
            meta
        );
    }
    
    private String getStatusVietnamese(String status) {
        switch (status) {
            case "present": return "có mặt";
            case "late": return "đi muộn";
            case "absent": return "vắng mặt";
            case "pending": return "chưa điểm danh";
            default: return status;
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}