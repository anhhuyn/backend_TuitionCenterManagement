package com.management.student_center.service;

import com.management.student_center.dto.AttendanceResponseDTO;
import com.management.student_center.entity.*;
import com.management.student_center.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    private final SessionRepository sessionRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final AttendanceStudentRepository attendanceStudentRepository;
    private final StudentRepository studentRepository;
    
    public AttendanceService(
            SessionRepository sessionRepository,
            StudentSubjectRepository studentSubjectRepository,
            AttendanceStudentRepository attendanceStudentRepository,
            StudentRepository studentRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.studentSubjectRepository = studentSubjectRepository;
        this.attendanceStudentRepository = attendanceStudentRepository;
        this.studentRepository = studentRepository;
    }

    // Lấy danh sách điểm danh
    public AttendanceResponseDTO getAttendanceBySubject(Long subjectId) {

        List<Session> sessions = sessionRepository.findBySubject_IdOrderBySessionDateAsc(subjectId);

        List<StudentSubject> studentSubjects = studentSubjectRepository.findBySubject_Id(subjectId);

        AttendanceResponseDTO response = new AttendanceResponseDTO();

        // map sessions
        var sessionDTOs = sessions.stream()
                .map(s -> new AttendanceResponseDTO.SessionDTO(
                        s.getId(),
                        s.getSessionDate(),
                        s.getStartTime(),
                        s.getEndTime()))
                .toList();

        // map students + attendances
        var students = studentSubjects.stream().map(ss -> {
            Student student = ss.getStudent();

            List<AttendanceStudent> attendances =
                    attendanceStudentRepository.findAllByStudentAndSessionIn(student, sessions);

            var attendanceItems = attendances.stream()
                    .map(a -> new AttendanceResponseDTO.AttendanceItem(
                            a.getSession().getId(),
                            a.getStatus(),
                            a.getNote()
                    ))
                    .toList();

            return new AttendanceResponseDTO.StudentAttendanceDTO(
                    student.getId(),
                    student.getUserInfo().getFullName(),
                    attendanceItems
            );
        }).toList();
        

        response.setSubjectId(subjectId);
        response.setSessions(sessionDTOs);
        response.setStudents(students);

        return response;
    }

    // cập nhật trạng thái điểm danh
    public String updateStatus(Long studentId, Long sessionId, String status) {
    	 System.out.println("DEBUG: updateStatus called with studentId=" + studentId + ", sessionId=" + sessionId + ", status=" + status);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        AttendanceStudent existing = attendanceStudentRepository
                .findBySessionAndStudent(session, student)
                .orElse(null);

        if (existing != null) {
            existing.setStatus(status);
            attendanceStudentRepository.save(existing);
            return "Cập nhật trạng thái điểm danh thành công";
        }

        AttendanceStudent newAttendance = new AttendanceStudent();
        newAttendance.setStudent(student);
        newAttendance.setSession(session);
        newAttendance.setStatus(status);
        attendanceStudentRepository.save(newAttendance);

        return "Thêm mới trạng thái điểm danh thành công";
    }

    // cập nhật ghi chú
    public String updateNote(Long studentId, Long sessionId, String note) {
    	  System.out.println("DEBUG: updateNote called with studentId=" + studentId + ", sessionId=" + sessionId + ", note=" + note);
    	  List<Student> allStudents = studentRepository.findAll();
    	  System.out.println("DEBUG: All student IDs in DB: " + allStudents.stream().map(Student::getId).toList());


        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        AttendanceStudent existing = attendanceStudentRepository
                .findBySessionAndStudent(session, student)
                .orElse(null);

        if (existing != null) {
            existing.setNote(note);
            attendanceStudentRepository.save(existing);
            return "Cập nhật ghi chú thành công";
        }

        AttendanceStudent newAttendance = new AttendanceStudent();
        newAttendance.setStudent(student);
        newAttendance.setSession(session);
        newAttendance.setNote(note);
        attendanceStudentRepository.save(newAttendance);

        return "Thêm mới ghi chú thành công";
    }
}
