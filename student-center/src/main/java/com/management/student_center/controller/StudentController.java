package com.management.student_center.controller;

import com.management.student_center.dto.PaginatedResponseDTO;
import com.management.student_center.dto.student.CreateStudentDTO;
import com.management.student_center.dto.student.StudentDTO;
import com.management.student_center.dto.student.StudentGroupResponseDTO;
import com.management.student_center.dto.student.StudentStatisticDTO;
import com.management.student_center.entity.User;
import com.management.student_center.service.StudentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * === handleGetAllStudents ===
     * Lấy danh sách học sinh (Filter: name, grade, schoolName, gender, subject)
     */
    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>> getAllStudents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String subject) { // Thêm filter subject
        try {
            Map<String, String> filters = new HashMap<>();
            if (name != null) filters.put("name", name);
            if (grade != null) filters.put("grade", grade);
            if (schoolName != null) filters.put("schoolName", schoolName);
            if (gender != null) filters.put("gender", gender);
            if (subject != null) filters.put("subject", subject);

            PaginatedResponseDTO<StudentDTO> serviceResponse = studentService.getAllStudents(page, limit, filters);

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
    
    @GetMapping("/students/group-by-school")
    public ResponseEntity<Map<String, Object>> getStudentsGroupBySchool(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String gender) {

        try {
            Map<String, String> filters = new HashMap<>();
            if (name != null) filters.put("name", name);
            if (grade != null) filters.put("grade", grade);
            if (schoolName != null) filters.put("schoolName", schoolName);
            if (gender != null) filters.put("gender", gender);

            StudentGroupResponseDTO serviceResponse =
                    studentService.getAllStudentsGroupBySchool(filters);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "OK");
            response.put("totalStudents", serviceResponse.getTotalStudents());
            response.put("data", serviceResponse.getStudentsBySchool());
            response.put("totalStudentsBySchool", serviceResponse.getTotalStudentsBySchool());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }


    /**
     * === handleGetStudentById ===
     * Lấy chi tiết 1 học sinh
     */
    @GetMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> getStudentById(@PathVariable Long id) {
        try {
            StudentDTO studentDTO = studentService.getStudentById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "OK");
            response.put("data", studentDTO);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e, 404); // 404 Not Found nếu không thấy
        }
    }

    /**
     * === handleCreateNewStudent ===
     * Tạo mới (Multipart: form-data)
     */
    @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createNewStudent(
            @ModelAttribute CreateStudentDTO studentDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            User newStudent = studentService.createNewStudent(studentDTO, file);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Thêm học sinh mới thành công!");
            response.put("newId", newStudent.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return createErrorResponse(e, 400);
        }
    }

    /**
     * === handleUpdateStudent ===
     * Cập nhật (Multipart: form-data)
     */
    @PutMapping(value = "/students/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable Long id,
            @ModelAttribute CreateStudentDTO studentDTO, // Dùng chung DTO Create cho Update
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            studentService.updateStudent(id, studentDTO, file);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Cập nhật thông tin học sinh thành công!");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e, 400);
        }
    }

    /**
     * === handleDeleteStudent ===
     * Xóa 1 học sinh
     */
    @DeleteMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Xóa học viên thành công!");
            
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Học sinh còn nợ học phí → CONFLICT 409
            return createErrorResponse(e, 409);
        } catch (RuntimeException e) {
            // Không tìm thấy học sinh, lỗi nghiệp vụ → NOT FOUND 404
            return createErrorResponse(e, 404);
        } catch (Exception e) {
            // Lỗi khác → BAD REQUEST 400 hoặc INTERNAL SERVER ERROR 500
            return createErrorResponse(e, 400);
        }
    }


    /**
     * === handleDeleteMultipleStudents ===
     * Xóa nhiều
     */
    @PostMapping("/students/delete-multiple")
    public ResponseEntity<Map<String, Object>> deleteMultipleStudents(
            @RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            if (ids == null || ids.isEmpty()) {
                throw new RuntimeException("Danh sách ID học viên không hợp lệ!");
            }

            studentService.deleteMultipleStudents(ids);

            Map<String, Object> response = new HashMap<>();
            response.put("errCode", 0);
            response.put("message", "Đã xóa " + ids.size() + " học viên thành công!");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse(e, 400);
        }
    }

    /**
     * === handleExportStudentsExcel ===
     * Xuất Excel
     */
    @GetMapping("/students/export")
    public ResponseEntity<?> exportStudentsExcel(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String gender) {
        try {
            Map<String, String> filters = new HashMap<>();
            if (name != null) filters.put("name", name);
            if (grade != null) filters.put("grade", grade);
            if (schoolName != null) filters.put("schoolName", schoolName);
            if (gender != null) filters.put("gender", gender);

            byte[] buffer = studentService.exportStudentsToExcel(filters);

            if (buffer == null || buffer.length == 0) {
                 return createErrorResponse(new RuntimeException("Không có dữ liệu để xuất!"), 404);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=danh-sach-hoc-vien.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(buffer);

        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    @GetMapping("/students/statistics")
    public StudentStatisticDTO getStudentStatistics() {
    	return studentService.getStudentStatistics();
    }

    // === Helper Functions ===
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