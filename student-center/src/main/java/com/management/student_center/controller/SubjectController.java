package com.management.student_center.controller;

import com.management.student_center.dto.CreateSubjectRequest;
import com.management.student_center.dto.SubjectDTO;
import com.management.student_center.dto.UpdateSubjectRequest;
import com.management.student_center.dto.student.StudentStatisticDTO;
import com.management.student_center.dto.subject.SubjectStatisticsDTO;
import com.management.student_center.entity.Subject;
import com.management.student_center.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/v1/api")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;
    
    @GetMapping("/subjects/teacher/{userId}")
    public Map<String, Object> getSubjectsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String status
    ) {
        return subjectService.getSubjectsByUserId(page, limit, status, userId);
    }
    
 // ------------------- GET ALL SUBJECTS (NO PAGINATION) -------------------
    @GetMapping("/subjects/all")
    public Map<String, Object> getAllSubjectsNoPaging(
            @RequestParam(required = false) String status
    ) {
        return subjectService.getAllSubjectsNoPaging(status);
    }

    @GetMapping("/subjects")
    public Map<String, Object> getSubjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String status
    ) {
        return subjectService.getAllSubjects(page, limit, status);
    }

    @GetMapping("/subjects/{id}")
    public Map<String, Object> getSubjectById(@PathVariable Long id) {
        SubjectDTO dto = subjectService.getSubjectById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dto);
        return response;
    }

    // ------------------- DELETE SUBJECT -------------------
    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<?> deleteSubject(@PathVariable Long id) {
        try {
            subjectService.deleteSubject(id);
            return ResponseEntity.ok(
                Map.of(
                    "success", true,
                    "message", "Xóa môn học thành công."
                )
            );
        } catch (IllegalStateException e) {

            String message;

            switch (e.getMessage()) {
                case "TEACHER_UNPAID":
                    message = "Không thể xóa môn học vì vẫn còn lương giáo viên chưa thanh toán.";
                    break;
                case "STUDENT_UNPAID":
                    message = "Không thể xóa môn học vì vẫn còn học sinh chưa thanh toán học phí.";
                    break;
                default:
                    message = "Không thể xóa môn học.";
            }

            return ResponseEntity
                .badRequest()
                .body(
                    Map.of(
                        "success", false,
                        "code", e.getMessage(),
                        "message", message
                    )
                );
        }
    }
    
    @PutMapping("/subjects/{id}")
    public ResponseEntity<?> updateSubject(
            @PathVariable Long id,
            @RequestBody UpdateSubjectRequest updatedData
    ) {
        try {
            subjectService.updateSubject(id, updatedData);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Cập nhật môn học thành công."
                    )
            );

        } catch (IllegalStateException e) {

            String message;

            switch (e.getMessage()) {
                case "TEACHER_UNPAID_CHANGE":
                    message = "Không thể thay đổi giáo viên vì vẫn còn lương chưa thanh toán.";
                    break;
                default:
                    message = "Cập nhật môn học thất bại.";
            }

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success", false,
                                    "code", e.getMessage(),
                                    "message", message
                            )
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message", e.getMessage()
                            )
                    );
        }
    }

    @PostMapping("/subjects")
    public Map<String, Object> createSubject(
            @ModelAttribute CreateSubjectRequest request
    ) {
    	System.out.println("maxStudents = " + request.getMaxStudents());
        System.out.println("sessionsPerWeek = " + request.getSessionsPerWeek());
        System.out.println("price = " + request.getPrice());
        Map<String, Object> response = new HashMap<>();
        try {
            Subject subject = subjectService.createSubject(request);
            response.put("success", true);
            response.put("message", "Tạo môn học mới thành công.");
            response.put("data", subject);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
    
    @GetMapping("/subjects/statistics")
    public SubjectStatisticsDTO getSubjectStatistics() {
    	return subjectService.getSubjectStatistics();
    }

}
