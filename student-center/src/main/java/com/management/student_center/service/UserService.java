package com.management.student_center.service;

import com.management.student_center.dto.UpdateProfileRequest;
import com.management.student_center.entity.User;
import com.management.student_center.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OtpService otpService; // service OTP đã tạo trước đó

    private final Map<Long, String> pendingEmailUpdates = new HashMap<>();

    public UserService(UserRepository userRepository, OtpService otpService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    public Map<String, Object> updateUserProfile(Long userId, UpdateProfileRequest dto) {
        User user = getUserById(userId);

        user.setFullName(dto.fullName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setGender(dto.gender());
        

        Map<String, Object> result = new HashMap<>();

        if (dto.email() != null && !dto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.email())) {
                throw new RuntimeException("Email đã tồn tại trong hệ thống.");
            }

            try {
                otpService.sendEmailChangeOtp(dto.email());
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi gửi OTP: " + e.getMessage(), e);
            }

            pendingEmailUpdates.put(userId, dto.email());

            result.put("requireOtp", true);
            result.put("message", "OTP đã được gửi tới email mới.");
        }

        userRepository.save(user);
        result.put("message", "Thông tin đã được cập nhật.");
        result.put("user", user);

        return result;
    }

    public Map<String, Object> verifyEmailOtp(Long userId, String otp) {
        String email = pendingEmailUpdates.get(userId);
        if (email == null) throw new RuntimeException("Không có email đang chờ xác minh.");

        try {
            otpService.verifyOtp(email, otp);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        User user = getUserById(userId);
        user.setEmail(email);
        userRepository.save(user);

        pendingEmailUpdates.remove(userId);

        return Map.of("message", "Email đã được cập nhật thành công.");
    }

    public Map<String, Object> updateUserImage(Long userId, MultipartFile file) throws IOException {
        User user = getUserById(userId);

        // Xóa ảnh cũ nếu có
        if (user.getImage() != null) {
            Path oldImagePath = Paths.get(System.getProperty("user.dir"), "uploads", "avatars")
                    .resolve(Paths.get(user.getImage()).getFileName());
            Files.deleteIfExists(oldImagePath);
        }

        // Lưu ảnh mới
        String fileName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars"); // tuyệt đối
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir); // tạo cả thư mục cha nếu chưa có
        }
        Path filePath = uploadDir.resolve(fileName);
        file.transferTo(filePath.toFile());

        user.setImage("/uploads/avatars/" + fileName);
        userRepository.save(user);

        return Map.of(
                "message", "Ảnh đại diện đã được cập nhật.",
                "image", user.getImage()
        );
    }

}
