package com.management.student_center.controller;

import com.management.student_center.dto.StudentAssignmentResponseDTO;
import com.management.student_center.dto.UpdateStudentAssignmentDTO;
import com.management.student_center.entity.StudentAssignment;
import com.management.student_center.service.StudentAssignmentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")   // ⚡ Đổi lại cho đúng với FE
public class StudentAssignmentController {

    private final StudentAssignmentService service;

    public StudentAssignmentController(StudentAssignmentService service) {
        this.service = service;
    }

    // ---------------------------------------------------------
    // POST /v1/api/assign/{assignmentId}
    // ---------------------------------------------------------
    @PostMapping("/assign/{assignmentId}")
    public ResponseEntity<?> assignToStudents(@PathVariable Long assignmentId) {
        int total = service.assignToStudents(assignmentId);
        return ResponseEntity.ok("Gán assignment cho " + total + " học sinh thành công");
    }

    // ---------------------------------------------------------
    // GET /v1/api/by-assignment/{assignmentId}
    // ---------------------------------------------------------
    @GetMapping("/by-assignment/{assignmentId}")
    public ResponseEntity<?> getByAssignment(@PathVariable Long assignmentId) {
        List<StudentAssignmentResponseDTO> data = service.getByAssignmentId(assignmentId);

        return ResponseEntity.ok(Map.of(
            "message", "Lấy danh sách học sinh theo assignment thành công",
            "data", data
        ));
    }


    // ---------------------------------------------------------
    // PUT /v1/api/assign/update/{assignmentId}
    // ---------------------------------------------------------
    @PutMapping("/assign/update/{assignmentId}")
    public ResponseEntity<StudentAssignment> update(
            @PathVariable Long assignmentId,
            @RequestBody UpdateStudentAssignmentDTO dto
    ) {
        return ResponseEntity.ok(service.update(assignmentId, dto));
    }
}

