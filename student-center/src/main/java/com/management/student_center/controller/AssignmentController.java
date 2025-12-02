package com.management.student_center.controller;

import com.management.student_center.dto.AssignmentDTO;
import com.management.student_center.service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class AssignmentController {

	private final AssignmentService assignmentService;

	public AssignmentController(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	@GetMapping("/assignments/subject/{subjectId}")
	public ResponseEntity<?> getAssignmentsBySubject(@PathVariable Long subjectId) {
		List<AssignmentDTO> data = assignmentService.getAssignmentsBySubject(subjectId);
		return ResponseEntity.ok(Map.of("message", "Lấy danh sách assignment theo môn thành công", "data", data));
	}

	@PostMapping("/assignments")
	public ResponseEntity<?> createAssignment(@RequestParam Long sessionId, @RequestParam String title,
			@RequestParam String description, @RequestParam java.time.LocalDateTime dueDate,
			@RequestParam(required = false) MultipartFile file) throws Exception {
		AssignmentDTO assignment = assignmentService.createAssignment(sessionId, title, description, dueDate, file);
		return ResponseEntity.ok(Map.of("message", "Tạo assignment thành công", "data", assignment));
	}

	@PutMapping("/assignments/{assignmentId}")
	public ResponseEntity<?> updateAssignment(@PathVariable Long assignmentId,
			@RequestParam(required = false) String title, @RequestParam(required = false) String description,
			@RequestParam(required = false) java.time.LocalDateTime dueDate,
			@RequestParam(required = false) MultipartFile file) throws Exception {
		AssignmentDTO updated = assignmentService.updateAssignment(assignmentId, title, description, dueDate, file);
		return ResponseEntity.ok(Map.of("message", "Cập nhật assignment thành công", "data", updated));
	}

	@DeleteMapping("/assignments/{assignmentId}")
	public ResponseEntity<?> deleteAssignment(@PathVariable Long assignmentId) throws Exception {
		assignmentService.deleteAssignment(assignmentId);
		return ResponseEntity.ok(Map.of("message", "Xóa assignment thành công"));
	}
}
