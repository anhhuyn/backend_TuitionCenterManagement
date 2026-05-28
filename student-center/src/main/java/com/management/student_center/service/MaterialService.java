package com.management.student_center.service;

import com.management.student_center.dto.MaterialDTO;
import com.management.student_center.entity.Material;
import com.management.student_center.entity.Subject;
import com.management.student_center.entity.User;
import com.management.student_center.enums.ActivityActionType;
import com.management.student_center.enums.ActivityTargetType;
import com.management.student_center.repository.MaterialRepository;
import com.management.student_center.repository.SubjectRepository;
import com.management.student_center.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/materials/";

    public MaterialService(MaterialRepository materialRepository, 
                           SubjectRepository subjectRepository,
                           UserRepository userRepository,
                           ActivityLogService activityLogService,
                           CurrentUserService currentUserService) {
        this.materialRepository = materialRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;

        File dir = new File(uploadDir);
        if (!dir.exists())
            dir.mkdirs();
    }

    public List<MaterialDTO> getMaterialsBySubjectId(Long subjectId) {
        List<Material> materials = materialRepository.findBySubjectIdOrderByUploadedAtDesc(subjectId);
        return materials.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public Material createMaterial(String title, Long subjectId, Long userId, MultipartFile file) throws IOException {
        User currentUser = currentUserService.getCurrentUser();
        
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Chưa có file tải lên");

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học"));
        User user = userRepository.findById(userId).orElse(null);

        // Lưu file
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String fileName = System.currentTimeMillis() + "-" + (int) (Math.random() * 1e9) + ext;
        Path path = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        long fileSizeBytes = Files.size(path);
        String fileSizeMB = String.format("%.2f", fileSizeBytes / 1024.0 / 1024.0);

        Material material = new Material();
        material.setTitle(title);
        material.setFileURL("/uploads/materials/" + fileName);
        material.setType(ext.replace(".", "").toLowerCase());
        material.setUploadedAt(LocalDateTime.now());
        material.setSubject(subject);
        material.setUploadedBy(user);

        Material savedMaterial = materialRepository.save(material);

        // LOG ACTIVITY: TẠO TÀI LIỆU
        String description = String.format("đã tải lên tài liệu \"%s\" cho môn học %s (Khối %s)", 
                title, subject.getName(), subject.getGrade());
        String meta = String.format(
                "{\"materialId\":%d,\"title\":\"%s\",\"subjectId\":%d,\"subjectName\":\"%s\",\"subjectGrade\":\"%s\",\"fileType\":\"%s\",\"fileSize\":\"%s MB\",\"fileName\":\"%s\"}",
                savedMaterial.getId(), escapeJson(title), subjectId, escapeJson(subject.getName()), 
                subject.getGrade(), material.getType(), fileSizeMB, escapeJson(fileName));
        
        activityLogService.log(currentUser, ActivityActionType.CREATE, ActivityTargetType.COURSE, 
                subjectId, description, meta);

        return savedMaterial;
    }

    @Transactional
    public Material updateMaterialFile(Long materialId, MultipartFile file, String newTitle) throws IOException {
        User currentUser = currentUserService.getCurrentUser();
        
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));

        // Lưu thông tin cũ để ghi log
        String oldTitle = material.getTitle();
        String oldFileURL = material.getFileURL();
        String oldFileType = material.getType();
        Long subjectId = material.getSubject() != null ? material.getSubject().getId() : null;
        String subjectName = material.getSubject() != null ? material.getSubject().getName() : "Không xác định";
        String subjectGrade = material.getSubject() != null ? material.getSubject().getGrade() : "Không xác định";

        List<String> changes = new ArrayList<>();

        if (file != null && !file.isEmpty()) {
            // Xóa file cũ
            Path oldPath = Paths.get(System.getProperty("user.dir") + material.getFileURL());
            Files.deleteIfExists(oldPath);

            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String fileName = System.currentTimeMillis() + "-" + (int) (Math.random() * 1e9) + ext;
            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            long fileSizeBytes = Files.size(path);
            String fileSizeMB = String.format("%.2f", fileSizeBytes / 1024.0 / 1024.0);

            material.setFileURL("/uploads/materials/" + fileName);
            material.setType(ext.replace(".", "").toLowerCase());
            
            changes.add(String.format("file từ \"%s\" thành \"%s\"", 
                    oldFileURL.substring(oldFileURL.lastIndexOf("/") + 1), fileName));
        }

        if (newTitle != null && !newTitle.isEmpty() && !newTitle.equals(oldTitle)) {
            material.setTitle(newTitle);
            changes.add(String.format("tiêu đề từ \"%s\" thành \"%s\"", oldTitle, newTitle));
        }
        
        material.setUploadedAt(LocalDateTime.now());

        Material updatedMaterial = materialRepository.save(material);

        // LOG ACTIVITY: CẬP NHẬT TÀI LIỆU
        String description;
        if (changes.isEmpty()) {
            description = String.format("đã cập nhật tài liệu \"%s\" của môn học %s (Khối %s) - không có thay đổi", 
                    material.getTitle(), subjectName, subjectGrade);
        } else {
            description = String.format("đã cập nhật tài liệu \"%s\" của môn học %s (Khối %s): %s", 
                    material.getTitle(), subjectName, subjectGrade, String.join(", ", changes));
        }
        
        String meta = String.format(
                "{\"materialId\":%d,\"subjectId\":%d,\"subjectName\":\"%s\",\"oldValues\":{\"title\":\"%s\",\"fileURL\":\"%s\",\"fileType\":\"%s\"},\"newValues\":{\"title\":\"%s\",\"fileURL\":\"%s\",\"fileType\":\"%s\"}}",
                materialId, subjectId, escapeJson(subjectName),
                escapeJson(oldTitle), escapeJson(oldFileURL), oldFileType,
                escapeJson(material.getTitle()), escapeJson(material.getFileURL()), material.getType());
        
        activityLogService.log(currentUser, ActivityActionType.UPDATE, ActivityTargetType.COURSE, 
                subjectId, description, meta);

        return updatedMaterial;
    }

    @Transactional
    public void deleteMaterial(Long materialId) throws IOException {
        User currentUser = currentUserService.getCurrentUser();
        
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));

        // Lưu thông tin để ghi log
        String title = material.getTitle();
        Long subjectId = material.getSubject() != null ? material.getSubject().getId() : null;
        String subjectName = material.getSubject() != null ? material.getSubject().getName() : "Không xác định";
        String subjectGrade = material.getSubject() != null ? material.getSubject().getGrade() : "Không xác định";
        String fileURL = material.getFileURL();
        String fileType = material.getType();

        if (material.getFileURL() != null) {
            Path path = Paths.get(System.getProperty("user.dir") + material.getFileURL());
            Files.deleteIfExists(path);
        }

        materialRepository.delete(material);

        // LOG ACTIVITY: XÓA TÀI LIỆU
        String description = String.format("đã xóa tài liệu \"%s\" của môn học %s (Khối %s)", 
                title, subjectName, subjectGrade);
        String meta = String.format(
                "{\"materialId\":%d,\"title\":\"%s\",\"subjectId\":%d,\"subjectName\":\"%s\",\"subjectGrade\":\"%s\",\"fileURL\":\"%s\",\"fileType\":\"%s\"}",
                materialId, escapeJson(title), subjectId, escapeJson(subjectName), subjectGrade, 
                escapeJson(fileURL), fileType);
        
        activityLogService.log(currentUser, ActivityActionType.DELETE, ActivityTargetType.COURSE, 
                subjectId, description, meta);
    }

    public MaterialDTO convertToDTO(Material m) {
        String fileSize = "Không xác định";
        try {
            Path path = Paths.get(System.getProperty("user.dir") + m.getFileURL());
            fileSize = String.format("%.2f MB", Files.size(path) / 1024.0 / 1024.0);
        } catch (IOException ignored) {
        }

        MaterialDTO dto = new MaterialDTO(m.getId(), m.getTitle(), m.getFileURL(), m.getType(), m.getUploadedAt(),
                m.getSubject() != null ? m.getSubject().getId() : null,
                m.getSubject() != null ? m.getSubject().getName() : null,
                m.getUploadedBy() != null ? m.getUploadedBy().getId() : null,
                m.getUploadedBy() != null ? m.getUploadedBy().getFullName() : null,
                m.getUploadedBy() != null ? m.getUploadedBy().getImage() : null, fileSize);

        if (m.getUploadedBy() != null) {
            MaterialDTO.UserDTO userDto = new MaterialDTO.UserDTO();
            userDto.setId(m.getUploadedBy().getId());
            userDto.setFullName(m.getUploadedBy().getFullName());
            userDto.setEmail(m.getUploadedBy().getEmail());
            dto.setUser(userDto);
        }

        return dto;
    }

    // Helper method to escape JSON
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}