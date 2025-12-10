package com.management.student_center.controller;

import com.management.student_center.dto.payment.PaymentRequest; // <--- Import file mới
import com.management.student_center.entity.TeacherPayment;
import com.management.student_center.service.TeacherPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/payments")
public class TeacherPaymentController {

	private final TeacherPaymentService paymentService;

	public TeacherPaymentController(TeacherPaymentService paymentService) {
		this.paymentService = paymentService;
	}

	/**
	 * Tạo bảng lương cho cả tháng
	 */
	// Trong TeacherPaymentController.java

	@PostMapping("/create")
	public ResponseEntity<Map<String, Object>> createPayments(@RequestParam int month, @RequestParam int year,
			@RequestParam(required = false, defaultValue = "") String notes) {
		try {
			List<TeacherPayment> payments = paymentService.createTeacherPayments(month, year, notes);

			Map<String, Object> response = new HashMap<>();
			response.put("errCode", 0);
			response.put("message", "Tạo bảng lương thành công!");
			response.put("data", payments);

			return ResponseEntity.status(HttpStatus.CREATED).body(response);

		} catch (RuntimeException e) {
			// Bắt riêng lỗi nghiệp vụ (Ví dụ: Đã tồn tại)
			if (e.getMessage().contains("đã được tạo trước đó")) {
				Map<String, Object> response = new HashMap<>();
				response.put("errCode", 409); // Mã riêng cho trùng lặp
				response.put("message", e.getMessage());
				// Trả về 409 Conflict thay vì 400 Bad Request
				return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
			}
			// Các lỗi Runtime khác
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	/**
	 * Thanh toán lương cho 1 giáo viên Sử dụng PaymentRequest đã tách file riêng
	 */
	@PostMapping("/pay")
	public ResponseEntity<Map<String, Object>> paySalary(@RequestBody PaymentRequest request) {
		try {
			TeacherPayment result = paymentService.payTeacherSalary(request.getTeacherId(), request.getMonth(),
					request.getYear());

			Map<String, Object> response = new HashMap<>();
			response.put("errCode", 0);
			response.put("message", "Thanh toán thành công!");
			response.put("data", result);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	// ... Các API GET (list, detail) giữ nguyên như cũ ...

	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getListByMonth(@RequestParam int month, @RequestParam int year) {
		try {
			List<TeacherPayment> list = paymentService.getPaymentsByMonth(month, year);
			Map<String, Object> response = new HashMap<>();
			response.put("errCode", 0);
			response.put("message", "Lấy danh sách thành công");
			response.put("data", list);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@GetMapping("/detail")
	public ResponseEntity<Map<String, Object>> getDetail(@RequestParam Long teacherId, @RequestParam int month,
			@RequestParam int year) {
		try {
			TeacherPayment result = paymentService.getTeacherSalaryDetail(teacherId, month, year);
			if (result == null) {
				Map<String, Object> response = new HashMap<>();
				response.put("errCode", 1);
				response.put("message", "Không tìm thấy dữ liệu lương.");
				return ResponseEntity.ok(response);
			}
			Map<String, Object> response = new HashMap<>();
			response.put("errCode", 0);
			response.put("message", "OK");
			response.put("data", result);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	private ResponseEntity<Map<String, Object>> createErrorResponse(Exception e) {
		e.printStackTrace();
		Map<String, Object> response = new HashMap<>();
		response.put("errCode", 1);
		response.put("message", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
}