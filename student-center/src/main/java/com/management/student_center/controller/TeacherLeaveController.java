package com.management.student_center.controller;

import com.management.student_center.dto.leave.AssignReplacementTeacherDTO;
import com.management.student_center.dto.leave.LeaveAffectedSessionDTO;
import com.management.student_center.dto.leave.PreviewAffectedSessionRequestDTO;
import com.management.student_center.dto.leave.PreviewAffectedSessionResponseDTO;
import com.management.student_center.dto.leave.PreviewReplacementPlanRequestDTO;
import com.management.student_center.dto.leave.ReplacementTeacherResponseDTO;
import com.management.student_center.dto.leave.TeacherLeaveApproveDTO;
import com.management.student_center.dto.leave.TeacherLeaveFilterRequestDTO;
import com.management.student_center.dto.leave.TeacherLeaveRequestDTO;
import com.management.student_center.dto.leave.TeacherLeaveResponseDTO;
import com.management.student_center.dto.teacher.TeacherAbsentResponse;
import com.management.student_center.dto.teacher.WeeklyAbsentTeacherResponse;
import com.management.student_center.entity.LeaveAffectedSession;
import com.management.student_center.entity.Teacher;
import com.management.student_center.entity.TeacherLeave;
import com.management.student_center.entity.User;
import com.management.student_center.repository.TeacherRepository;
import com.management.student_center.service.TeacherLeaveService;
import com.management.student_center.dto.leave.AvailableReplacementTeacherDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/teacher-leaves")
public class TeacherLeaveController {

	private final TeacherLeaveService leaveService;
	private final TeacherRepository teacherRepository; // Thêm dependency

	public TeacherLeaveController(TeacherLeaveService leaveService, TeacherRepository teacherRepository) {
		this.leaveService = leaveService;
		this.teacherRepository = teacherRepository;
	}

	// =========================================================
	// LẤY THÔNG TIN NGƯỜI DÙNG HIỆN TẠI TỪ SECURITY CONTEXT
	// =========================================================

	private User getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof User) {
			return (User) auth.getPrincipal();
		}
		throw new RuntimeException("Không tìm thấy thông tin người dùng");
	}

	private Long getCurrentUserId() {
		return getCurrentUser().getId();
	}

	// Lấy teacherId thực tế (không phải userId)
	private Long getCurrentTeacherId() {
		User user = getCurrentUser();
		Teacher teacher = teacherRepository.findByUserInfoId(user.getId())
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên tương ứng với người dùng"));
		return teacher.getId();
	}

	// Trong file TeacherLeaveController.java, tìm phương thức getCurrentUserRole()
	private String getCurrentUserRole() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String roleCode = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("TEACHER");
		// Map role code sang tên phù hợp với logic trong service
		if ("R0".equals(roleCode)) {
			return "ADMIN";
		} else if ("R1".equals(roleCode)) {
			return "TEACHER";
		} else {
			return "STUDENT";
		}
	}

	// =========================================================
	// TẠO ĐƠN NGHỈ MỚI
	// =========================================================

	@PostMapping
	public ResponseEntity<Map<String, Object>> createLeaveRequest(@RequestBody TeacherLeaveRequestDTO dto) {
		try {
			TeacherLeaveResponseDTO responseDTO = leaveService.createLeaveRequest(getCurrentUserId(), dto);
			return successResponse("Tạo đơn nghỉ thành công", responseDTO, HttpStatus.CREATED);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// LẤY DANH SÁCH ĐƠN NGHỈ (PHÂN TRANG + LỌC)
	// =========================================================

	@GetMapping
	public ResponseEntity<Map<String, Object>> getLeaveRequests(@ModelAttribute TeacherLeaveFilterRequestDTO filter) {
		try {
			// Validate page/size trong filter
			if (filter.getPage() == null || filter.getPage() < 1) {
				filter.setPage(1);
			}
			if (filter.getSize() == null || filter.getSize() < 1) {
				filter.setSize(10);
			}
			if (filter.getSize() > 100) { // Giới hạn để tránh abuse
				filter.setSize(100);
			}
			Page<TeacherLeaveResponseDTO> leavePage = leaveService.getLeaveRequests(getCurrentUserId(),
					getCurrentUserRole(), filter);

			Map<String, Object> pagination = new HashMap<>();
			pagination.put("currentPage", leavePage.getNumber() + 1);
			pagination.put("totalPages", leavePage.getTotalPages());
			pagination.put("totalItems", leavePage.getTotalElements());
			pagination.put("pageSize", leavePage.getSize());

			Map<String, Object> response = new HashMap<>();
			response.put("errCode", 0);
			response.put("message", "OK");
			response.put("data", leavePage.getContent());
			response.put("pagination", pagination);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// LẤY CHI TIẾT ĐƠN NGHỈ
	// =========================================================

	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getLeaveRequestDetail(@PathVariable Long id) {
		try {
			TeacherLeaveResponseDTO dto = leaveService.getLeaveRequestById(id, getCurrentUserId(),
					getCurrentUserRole());
			return successResponse("OK", dto, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	// =========================================================
	// DUYỆT / TỪ CHỐI ĐƠN NGHỈ
	// =========================================================

	@PutMapping("/{id}/status")
	public ResponseEntity<Map<String, Object>> approveLeaveRequest(@PathVariable Long id,
			@RequestBody TeacherLeaveApproveDTO dto) {
		try {
			TeacherLeaveResponseDTO updated = leaveService.approveLeaveRequest(id, getCurrentUserId(), dto);
			return successResponse("Cập nhật trạng thái thành công", updated, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// HỦY ĐƠN NGHỈ (GIÁO VIÊN HỦY KHI ĐANG CHỜ)
	// =========================================================

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> cancelLeaveRequest(@PathVariable Long id) {
		try {
			leaveService.cancelLeaveRequest(id, getCurrentUserId());
			return successResponse("Hủy đơn nghỉ thành công", null, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// THỐNG KÊ SỐ ĐƠN ĐANG CHỜ
	// =========================================================

	@GetMapping("/statistics/pending")
	public ResponseEntity<Map<String, Object>> countPendingLeaves() {
		try {
			long pendingCount = leaveService.countPendingLeaves();
			Map<String, Object> data = new HashMap<>();
			data.put("pendingCount", pendingCount);
			return successResponse("OK", data, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// =========================================================
	// LẤY DANH SÁCH SESSION BỊ ẢNH HƯỞNG CỦA MỘT ĐƠN NGHỈ
	// =========================================================

	@GetMapping("/{leaveId}/affected-sessions")
	public ResponseEntity<Map<String, Object>> getAffectedSessions(@PathVariable Long leaveId) {
		try {
			List<LeaveAffectedSessionDTO> sessions = leaveService.getAffectedSessionsByLeave(leaveId);
			return successResponse("OK", sessions, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// ADMIN GÁN GIÁO VIÊN THAY THẾ CHO MỘT SESSION
	// =========================================================

	@PutMapping("/affected-sessions/{affectedSessionId}/assign")
	public ResponseEntity<Map<String, Object>> assignReplacementTeacher(@PathVariable Long affectedSessionId,
			@RequestBody AssignReplacementTeacherDTO dto) {
		try {
			leaveService.assignReplacementTeacher(affectedSessionId, dto);
			return successResponse("Gán giáo viên thay thế thành công", null, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// GIÁO VIÊN THAY THẾ PHẢN HỒI (ACCEPT / REJECT)
	// =========================================================

	@PutMapping("/affected-sessions/{affectedSessionId}/response")
	public ResponseEntity<Map<String, Object>> replacementTeacherResponse(@PathVariable Long affectedSessionId,
			@RequestBody ReplacementTeacherResponseDTO dto) {
		try {
			// Lấy teacherId thực tế (không phải userId)
			Long teacherId = getCurrentTeacherId();
			System.out.println("🔵 Teacher ID from token: " + teacherId);
			leaveService.replacementTeacherResponse(affectedSessionId, teacherId.intValue(), dto);
			return successResponse("Phản hồi thành công", null, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// HELPER RESPONSE
	// =========================================================

	private ResponseEntity<Map<String, Object>> successResponse(String message, Object data, HttpStatus status) {
		Map<String, Object> response = new HashMap<>();
		response.put("errCode", 0);
		response.put("message", message);
		response.put("data", data);
		return ResponseEntity.status(status).body(response);
	}

	private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
		Map<String, Object> response = new HashMap<>();
		response.put("errCode", status.value());
		response.put("message", message);
		return ResponseEntity.status(status).body(response);
	}

	@PostMapping("/preview-affected-sessions")
	public ResponseEntity<Map<String, Object>> previewAffectedSessions(
			@RequestBody PreviewAffectedSessionRequestDTO dto) {
		try {
			List<PreviewAffectedSessionResponseDTO> result = leaveService.previewAffectedSessions(getCurrentUserId(),
					dto);
			return successResponse("OK", result, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/affected-sessions/{affectedSessionId}/available-teachers")
	public ResponseEntity<Map<String, Object>> getAvailableTeachers(@PathVariable Long affectedSessionId) {
		try {
			List<AvailableReplacementTeacherDTO> teachers = leaveService
					.getAvailableReplacementTeachers(affectedSessionId);

			return successResponse("OK", teachers, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	// =========================================================
	// PREVIEW GIÁO VIÊN THAY THẾ KHI CHƯA APPROVE
	// =========================================================

	@GetMapping("/preview-available-teachers")
	public ResponseEntity<Map<String, Object>> previewAvailableTeachers(@RequestParam Long sessionId,
			@RequestParam Long leaveId) {
		try {
			List<AvailableReplacementTeacherDTO> teachers = leaveService.getAvailableReplacementTeachers(sessionId,
					leaveId);
			return successResponse("OK", teachers, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PostMapping("/preview-replacement-plan")
	public ResponseEntity<Map<String, Object>> previewReplacementPlan(
			@RequestBody PreviewReplacementPlanRequestDTO dto) {
		try {
			List<PreviewAffectedSessionResponseDTO> result = leaveService.previewReplacementPlan(getCurrentUserId(),
					dto);
			return successResponse("OK", result, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping("/affected-sessions/{affectedSessionId}/cancel")
	public ResponseEntity<Map<String, Object>> cancelAffectedSession(@PathVariable Long affectedSessionId) {
		try {
			leaveService.cancelAffectedSession(affectedSessionId);
			return successResponse("Đã hủy session bị ảnh hưởng", null, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/absent-this-week")
	public WeeklyAbsentTeacherResponse getAbsentTeachersThisWeek() {

		List<TeacherAbsentResponse> teachers = leaveService.getAbsentTeachersThisWeek();

		return new WeeklyAbsentTeacherResponse(teachers.size(), teachers);
	}

	// =========================================================
	// LẤY DANH SÁCH SESSION ĐƯỢC ASSIGN DẠY THAY
	// =========================================================

	@GetMapping("/replacement-sessions")
	public ResponseEntity<Map<String, Object>> getReplacementSessions() {
		try {
			List<LeaveAffectedSessionDTO> sessions = leaveService.getReplacementSessions(getCurrentUserId());
			return successResponse("OK", sessions, HttpStatus.OK);
		} catch (Exception e) {
			return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
}