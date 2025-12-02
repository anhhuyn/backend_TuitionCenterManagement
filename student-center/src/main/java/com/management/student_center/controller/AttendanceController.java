package com.management.student_center.controller;

import com.management.student_center.dto.AttendanceResponseDTO;
import com.management.student_center.service.AttendanceService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/subject/{subjectId}/attendance")
    public ResponseEntity<?> getAttendance(@PathVariable Long subjectId) {
        AttendanceResponseDTO data = attendanceService.getAttendanceBySubject(subjectId);
        return ResponseEntity.ok(Map.of(
            "message", "Lấy danh sách học sinh và điểm danh thành công",
            "data", data
        ));
    }

 // PUT /v1/api/attendance/status
    @PutMapping("/attendance/status")
    public ResponseEntity<?> updateStatus(@RequestBody StatusRequest req) {
        // đổi thứ tự: studentId trước, sessionId sau
        String message = attendanceService.updateStatus(req.studentId, req.sessionId, req.status);
        return ResponseEntity.ok(message);
    }

    // PUT /v1/api/attendance/note
    @PutMapping("/attendance/note")
    public ResponseEntity<?> updateNote(@RequestBody NoteRequest req) {
        String message = attendanceService.updateNote(req.studentId, req.sessionId, req.note);
        return ResponseEntity.ok(message);
    }


    public record StatusRequest(Long sessionId, Long studentId, String status) {}
    public record NoteRequest(Long sessionId, Long studentId, String note) {}
}

