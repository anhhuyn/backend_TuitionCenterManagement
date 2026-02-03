package com.management.student_center.controller;

import com.management.student_center.dto.ChangePasswordRequest;
import com.management.student_center.dto.OtpRequest;
import com.management.student_center.dto.ResetPasswordRequest;
import com.management.student_center.dto.VerifyOtpRequest;
import com.management.student_center.entity.User;
import com.management.student_center.service.OtpService;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class ForgotPasswordController {

    private final OtpService otpService;

    public ForgotPasswordController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest request) {
        try {
            otpService.sendPasswordResetOtp(request.email());
            return ResponseEntity.ok().body(Map.of("success", true, "message", "OTP đã được gửi đến email của bạn."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            otpService.verifyOtp(request.email(), request.otp());
            return ResponseEntity.ok().body(Map.of("success", true, "message", "OTP hợp lệ."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            otpService.resetPassword(request.email(), request.otp(), request.newPassword());
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Mật khẩu đã được đặt lại thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
        @AuthenticationPrincipal User user,
        @RequestBody ChangePasswordRequest request
    ) {
        try {
            otpService.changePassword(
                user.getId(),
                request.currentPassword(),
                request.newPassword()
            );
            return ResponseEntity.ok(
                Map.of("message", "Đổi mật khẩu thành công")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("message", e.getMessage())
            );
        }
    }

}
