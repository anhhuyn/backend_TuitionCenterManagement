package com.management.student_center.service;

import com.management.student_center.dto.CreateAnnouncementRequest;
import com.management.student_center.dto.UpdateAnnouncementRequest;
import com.management.student_center.entity.Announcement;
import com.management.student_center.entity.User;
import com.management.student_center.repository.AnnouncementRepository;
import com.management.student_center.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final UserRepository userRepo;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/announcements/";

    public AnnouncementService(AnnouncementRepository announcementRepo, UserRepository userRepo) {
        this.announcementRepo = announcementRepo;
        this.userRepo = userRepo;

        // Tạo thư mục nếu chưa có
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
    }

    // CREATE ANNOUNCEMENT + MULTIPLE ATTACHMENTS
    public Announcement createAnnouncement(
            CreateAnnouncementRequest req,
            MultipartFile imageFile,
            List<MultipartFile> attachmentFiles
    ) throws IOException {

        User admin = userRepo.findById(req.getAdminId())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Announcement a = new Announcement();
        a.setAdmin(admin);
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = saveFile(imageFile);
            a.setImageURL("/uploads/announcements/" + fileName);
        } else if (req.getImageURL() != null) {
            a.setImageURL(req.getImageURL());
        }

        if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
            List<String> attachmentURLs = attachmentFiles.stream()
                    .map(file -> {
                        try {
                            String fileName = saveFile(file);
                            return "/uploads/announcements/" + fileName;
                        } catch (IOException e) {
                            throw new RuntimeException("Attachment upload failed");
                        }
                    })
                    .toList();

            a.setAttachments(attachmentURLs);
        }

        String status = req.getStatus() != null
                ? req.getStatus().trim().toLowerCase()
                : "active";

        if (!status.equals("active") && !status.equals("inactive") && !status.equals("draft")) {
            throw new RuntimeException("Invalid status. Valid values: active, inactive, draft");
        }
        a.setStatus(status);

        return announcementRepo.save(a);
    }

    // UPDATE ANNOUNCEMENT + MULTIPLE ATTACHMENTS
    public Announcement updateAnnouncement(
            Long id,
            UpdateAnnouncementRequest req,
            MultipartFile imageFile,
            List<MultipartFile> attachmentFiles
    ) throws IOException {

        Announcement a = announcementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getContent() != null) a.setContent(req.getContent());

        if (req.getStatus() != null) {
            String newStatus = req.getStatus().trim().toLowerCase();
            if (!newStatus.equals("active") && !newStatus.equals("inactive") && !newStatus.equals("draft")) {
                throw new RuntimeException("Invalid status");
            }
            a.setStatus(newStatus);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            deletePhysicalFile(a.getImageURL());

            String newFile = saveFile(imageFile);
            a.setImageURL("/uploads/announcements/" + newFile);
        }

        if (attachmentFiles != null && !attachmentFiles.isEmpty()) {

            if (a.getAttachments() != null) {
                for (String oldFileURL : a.getAttachments()) {
                    deletePhysicalFile(oldFileURL);
                }
            }

            List<String> newAttachments = attachmentFiles.stream()
                    .map(file -> {
                        try {
                            String fileName = saveFile(file);
                            return "/uploads/announcements/" + fileName;
                        } catch (IOException e) {
                            throw new RuntimeException("Attachment update failed");
                        }
                    })
                    .toList();

            a.setAttachments(newAttachments);
        }

        return announcementRepo.save(a);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf("."))
                : "";

        String fileName = System.currentTimeMillis() + "-" + (int)(Math.random() * 1e9) + ext;
        Path path = Paths.get(uploadDir + fileName);

        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private void deletePhysicalFile(String url) {
        if (url == null || url.startsWith("http")) return;

        try {
            String filename = Paths.get(url).getFileName().toString();
            Path filePath = Paths.get(uploadDir, filename);

            if (Files.exists(filePath)) Files.delete(filePath);

        } catch (Exception e) {
            System.err.println("Không thể xóa file: " + e.getMessage());
        }
    }


    public Page<Announcement> getAnnouncements(int page, int size) {
    	Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
    	return announcementRepo.findAll(pageable);

    }

    public Announcement getById(Long id) {
        return announcementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
    }

    public void deleteAnnouncement(Long id) {
        Announcement a = getById(id);

        deletePhysicalFile(a.getImageURL());

        if (a.getAttachments() != null) {
            for (String url : a.getAttachments()) {
                deletePhysicalFile(url);
            }
        }

        announcementRepo.deleteById(id);
    }

    public List<Announcement> getAll() {
        return announcementRepo.findAll();
    }
}
