package com.management.student_center.controller;

import com.management.student_center.dto.TeacherBasicDTO;
import com.management.student_center.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.management.student_center.dto.PaginatedResponseDTO;
import com.management.student_center.dto.teacher.CreateEmployeeDTO;
import com.management.student_center.dto.teacher.TeacherDTO;
import com.management.student_center.dto.teacher.TeacherStatisticsDTO;
import com.management.student_center.dto.teacher.UpdateEmployeeDTO;
import com.management.student_center.entity.User;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/teachers/basic")
    public ResponseEntity<Map<String, Object>> getTeacherBasicList() {
        try {
            List<TeacherBasicDTO> data = teacherService.getTeacherBasicList();
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "OK");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("errCode", 500);
            error.put("message", "Có lỗi xảy ra từ phía máy chủ!");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/teachers")
    public ResponseEntity<Map<String, Object>> getAllTeachers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String gender) {
        try {
            // Gom filter lại
            Map<String, String> filters = new HashMap<>();
            if (name != null) filters.put("name", name);
            if (specialty != null) filters.put("specialty", specialty);
            if (gender != null) filters.put("gender", gender);

            PaginatedResponseDTO<TeacherDTO> serviceResponse = teacherService.getAllTeachers(page, limit, filters);

            // Gói response theo format chuẩn
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "OK");
            response.put("data", serviceResponse.data);
            response.put("pagination", serviceResponse.pagination);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    /**
     * === Tương đương handleCreateNewEmployee ===
     * Tạo nhân viên/giáo viên mới (dùng multipart)
     */
    @PostMapping(value = "/teachers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createNewEmployee(
            @ModelAttribute CreateEmployeeDTO employeeDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            User newUser = teacherService.createNewEmployee(employeeDTO, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Thêm nhân viên mới thành công!");
            response.put("newId", newUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return createErrorResponse(e, 400); // 400 Bad Request
        }
    }

    /**
     * === Tương đương handleUpdateEmployee ===
     * Cập nhật thông tin (dùng multipart)
     * (Chúng ta dùng @PathVariable {id} cho chuẩn RESTful)
     */
    @PutMapping(value = "/teachers/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable Long id,
            @ModelAttribute UpdateEmployeeDTO employeeDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            teacherService.updateEmployeeData(id, employeeDTO, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Cập nhật thông tin giáo viên thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createErrorResponse(e, 400); // 400 Bad Request
        }
    }

    /**
     * === Tương đương handleDeleteEmployee ===
     * Xóa 1 nhân viên
     */
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(@PathVariable Long id) {
        try {
            teacherService.deleteEmployee(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Xóa nhân viên thành công!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    /**
     * === Tương đương handleDeleteMultipleTeachers ===
     * Xóa nhiều giáo viên
     */
    @PostMapping("/teachers/delete-multiple")
    public ResponseEntity<Map<String, Object>> deleteMultipleTeachers(
            @RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            if (ids == null || ids.isEmpty()) {
                throw new RuntimeException("Danh sách ID không hợp lệ!");
            }
            
            teacherService.deleteMultipleTeachers(ids);
            
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Đã xóa " + ids.size() + " giáo viên thành công!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e, 400); // 400 Bad Request
        }
    }

    /**
     * === Tương đương handleExportTeachersExcel ===
     * Xuất file Excel
     */
    @GetMapping("/teachers/export")
    public ResponseEntity<?> exportTeachersExcel(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String gender) {
        try {
            // Gom filter
            Map<String, String> filters = new HashMap<>();
            if (name != null) filters.put("name", name);
            if (specialty != null) filters.put("specialty", specialty);
            if (gender != null) filters.put("gender", gender);

            byte[] buffer = teacherService.exportTeachersToExcel(filters);

            // Set headers để trình duyệt tải file
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=danh-sach-giao-vien.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Kiểu file nhị phân
                    .body(buffer);

        } catch (Exception e) {
            // Nếu có lỗi, trả về JSON lỗi
            return createErrorResponse(e);
        }
    }
    
    @GetMapping("/teachers/statistics")
    public TeacherStatisticsDTO getTeacherStatistics() {
        return teacherService.getTeacherStatistics();
    }
    
    // === Hàm helper tạo lỗi (giống code cũ của bạn) ===
    private ResponseEntity<Map<String, Object>> createErrorResponse(Exception e) {
        return createErrorResponse(e, 500);
    }
    
    private ResponseEntity<Map<String, Object>> createErrorResponse(Exception e, int statusCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("errCode", statusCode);
        error.put("message", e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra từ phía máy chủ!");
        return ResponseEntity.status(statusCode).body(error);
    }
}
