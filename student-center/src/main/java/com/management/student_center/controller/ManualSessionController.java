package com.management.student_center.controller;

import com.management.student_center.dto.ManualSessionRequest;
import com.management.student_center.dto.SessionDTO;
import com.management.student_center.service.SubjectScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/v1/api")
public class ManualSessionController {

    private final SubjectScheduleService subjectScheduleService;

    public ManualSessionController(SubjectScheduleService subjectScheduleService) {
        this.subjectScheduleService = subjectScheduleService;
    }

    @PostMapping("/manual-session")
    public ResponseEntity<?> addManualSession(@RequestBody ManualSessionRequest req) {
        try {
            SessionDTO session = subjectScheduleService.addManualSession(req);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo session thành công");
            response.put("data", session);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (NoSuchElementException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tạo session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/session/{sessionId}")
    public ResponseEntity<?> updateSession(@PathVariable Long sessionId,
                                           @RequestBody ManualSessionRequest req) {
        try {
            SessionDTO session = subjectScheduleService.updateSession(sessionId, req);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật buổi học thành công");
            response.put("data", session);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (NoSuchElementException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi cập nhật buổi học: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}