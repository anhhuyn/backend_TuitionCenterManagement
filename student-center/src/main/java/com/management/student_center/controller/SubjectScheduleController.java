package com.management.student_center.controller;

import com.management.student_center.dto.CreateSubjectScheduleRequest;
import com.management.student_center.dto.SessionDTO;
import com.management.student_center.service.SubjectScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/api") 
public class SubjectScheduleController {

    private final SubjectScheduleService subjectScheduleService;

    public SubjectScheduleController(SubjectScheduleService subjectScheduleService) {
        this.subjectScheduleService = subjectScheduleService;
    }

    @GetMapping("/schedule/{subjectId}")
    public ResponseEntity<?> getScheduleBySubjectId(@PathVariable("subjectId") Long subjectId) {
        try {
            if (subjectId == null) {
                Map<String, Object> body = Map.of("message", "Thiếu subjectId");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }

            List<SessionDTO> sessions = subjectScheduleService.getScheduleBySubjectId(subjectId);
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Lấy thời khóa biểu thành công");
            body.put("sessions", sessions);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Có lỗi xảy ra khi lấy thời khóa biểu", "error", e.getMessage()));
        }
    }


    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionById(@PathVariable("sessionId") Long sessionId) {
        try {
            if (sessionId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Thiếu sessionId"));
            }
            SessionDTO session = subjectScheduleService.getSessionById(sessionId);
            return ResponseEntity.ok(Map.of("session", session));
        } catch (NoSuchElementException nse) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", nse.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi server"));
        }
    }
    
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable("sessionId") Long sessionId) {
        try {
            if (sessionId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Thiếu sessionId"));
            }
            Map<String, Object> result = subjectScheduleService.deleteSession(sessionId);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Xoá buổi học thành công");
            body.put("data", result);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", iae.getMessage()));
        } catch (NoSuchElementException nse) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", nse.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra khi xoá buổi học", "error", e.getMessage()));
        }
    }

    @PostMapping("/subject-schedules")
    public ResponseEntity<?> createSubjectSchedule(@RequestBody CreateSubjectScheduleRequest req) {
        try {
            Map<String, Object> result = subjectScheduleService.createSubjectSchedule(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Tạo lịch học và sinh sessions thành công",
                            "data", result
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra khi tạo lịch học", "error", e.getMessage()));
        }
    }

}
