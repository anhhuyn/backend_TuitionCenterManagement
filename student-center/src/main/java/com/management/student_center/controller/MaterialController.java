package com.management.student_center.controller;

import com.management.student_center.dto.MaterialDTO;
import com.management.student_center.entity.Material;
import com.management.student_center.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/materials/subject/{subjectId}")
    public ResponseEntity<?> getMaterialsBySubject(@PathVariable Long subjectId) {
        try {
            List<MaterialDTO> materials = materialService.getMaterialsBySubjectId(subjectId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Lấy danh sách tài liệu thành công");
            res.put("data", materials);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi server: " + e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/materials")
    public ResponseEntity<?> uploadMaterial(@RequestParam String title,
                                            @RequestParam Long subjectId,
                                            @RequestParam Long userId,
                                            @RequestParam("file") MultipartFile file) {
        try {
            Material material = materialService.createMaterial(title, subjectId, userId, file);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Tải lên tài liệu thành công");
            MaterialDTO dto = materialService.convertToDTO(material);
            res.put("data", dto);
            return ResponseEntity.ok(res);
        } catch (IOException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/materials/{materialId}")
    public ResponseEntity<?> updateMaterial(@PathVariable Long materialId,
                                            @RequestParam(value = "file", required = false) MultipartFile file,
                                            @RequestParam(value = "title", required = false) String title) {
        try {
            Material material = materialService.updateMaterialFile(materialId, file, title);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Cập nhật tài liệu thành công");
            MaterialDTO dto = materialService.convertToDTO(material);
            res.put("data", dto);
            return ResponseEntity.ok(res);
        } catch (IOException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long materialId) {
        try {
            materialService.deleteMaterial(materialId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Xóa tài liệu thành công");
            res.put("data", null);
            return ResponseEntity.ok(res);
        } catch (IOException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}