package com.management.student_center.controller;

// Import DTO vừa tạo
import com.management.student_center.dto.tuition.TuitionPaymentRequest; 
import com.management.student_center.entity.StudentTuition;
import com.management.student_center.service.StudentTuitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/tuitions")
@CrossOrigin(origins = "*") 
public class StudentTuitionController {

    private final StudentTuitionService tuitionService;

    public StudentTuitionController(StudentTuitionService tuitionService) {
        this.tuitionService = tuitionService;
    }

    /**
     * 1. 🟢 Tạo hóa đơn học phí cho cả tháng
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTuitions(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "") String notes) {
        try {
            List<StudentTuition> tuitions = tuitionService.createTuitions(month, year, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Tạo hóa đơn thành công cho " + tuitions.size() + " học sinh.");
            response.put("data", tuitions);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    /**
     * 2. 🔵 Lấy danh sách hóa đơn theo tháng
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getListByMonth(@RequestParam int month, @RequestParam int year) {
        try {
            List<StudentTuition> list = tuitionService.getTuitionsByMonth(month, year);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Lấy danh sách thành công");
            response.put("data", list);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    /**
     * 3. 🟡 Xem chi tiết hóa đơn của 1 học sinh
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getDetail(
            @RequestParam Long studentId,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            StudentTuition result = tuitionService.getTuitionDetail(studentId, month, year);

            if (result == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("errCode", 1);
                response.put("message", "Không tìm thấy dữ liệu học phí.");
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

    /**
     * 4. 🟣 Thanh toán học phí
     * Sử dụng DTO đã tách file: TuitionPaymentRequest
     */
    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> payTuition(@RequestBody TuitionPaymentRequest request) {
        try {
            StudentTuition result = tuitionService.payTuition(
                    request.getStudentId(),
                    request.getMonth(),
                    request.getYear()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Thanh toán học phí thành công!");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    // Hàm phụ trợ trả lỗi
    private ResponseEntity<Map<String, Object>> createErrorResponse(Exception e) {
        e.printStackTrace(); 
        Map<String, Object> response = new HashMap<>();
        
        if (e.getMessage() != null && e.getMessage().contains("đã được tạo")) {
            response.put("errCode", 409); 
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        response.put("errCode", 1);
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}