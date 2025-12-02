package com.management.student_center.controller;

import com.management.student_center.dto.CreateAnnouncementRequest;
import com.management.student_center.dto.UpdateAnnouncementRequest;
import com.management.student_center.entity.Announcement;
import com.management.student_center.service.AnnouncementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/announcements")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping
    public Announcement create(
            @RequestPart("data") CreateAnnouncementRequest req,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) throws IOException {
        return service.createAnnouncement(req, imageFile, attachments);
    }

    @GetMapping
    public Page<Announcement> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getAnnouncements(page, size);
    }

    @GetMapping("/{id}")
    public Announcement getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Announcement update(
            @PathVariable Long id,
            @RequestPart("data") UpdateAnnouncementRequest req,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) throws IOException {
        return service.updateAnnouncement(id, req, imageFile, attachments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteAnnouncement(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("Error deleting announcement: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "warning", "Không thể xóa file (có thể không tồn tại)"
            ));
        }
    }
}
