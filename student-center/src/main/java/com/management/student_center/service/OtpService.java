package com.management.student_center.service;

import com.management.student_center.entity.User;
import com.management.student_center.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class OtpService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    // OTP store tạm thời
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    @Value("${spring.mail.username}")
    private String fromEmail;

    public OtpService(UserRepository userRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ----------------- SEND OTP -----------------
    public void sendPasswordResetOtp(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Email không tồn tại trong hệ thống."));

        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiry = Instant.now().toEpochMilli() + 5 * 60 * 1000; // 5 phút

        otpStore.put(email, new OtpRecord(otp, expiry));

        sendEmail(email, "Password Reset OTP", 
            "Mã OTP để reset mật khẩu của bạn là: " + otp + ". OTP có hiệu lực trong 5 phút.");
    }

    public void sendEmailChangeOtp(String email) throws Exception {
        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiry = Instant.now().toEpochMilli() + 5 * 60 * 1000; // 5 phút
        otpStore.put(email, new OtpRecord(otp, expiry));

        sendEmail(email, "Email Change OTP",
            "Mã OTP để đổi email là: " + otp + ". OTP có hiệu lực trong 5 phút.");
    }

    private void sendEmail(String to, String subject, String text) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false);
        mailSender.send(message);
    }

    // ----------------- VERIFY OTP -----------------
    public void verifyOtp(String email, String otp) throws Exception {
        OtpRecord record = otpStore.get(email);
        if (record == null) throw new Exception("Chưa có OTP cho email này.");
        if (!record.getOtp().equals(otp)) throw new Exception("OTP không đúng.");
        if (Instant.now().toEpochMilli() > record.getExpiry()) throw new Exception("OTP đã hết hạn.");
    }

    // ----------------- RESET PASSWORD -----------------
    public void resetPassword(String email, String otp, String newPassword) throws Exception {
        verifyOtp(email, otp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Email không tồn tại trong hệ thống."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpStore.remove(email); // xóa OTP sau khi dùng
    }

    // Internal class lưu OTP
    private static class OtpRecord {
        private final String otp;
        private final long expiry;

        public OtpRecord(String otp, long expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }

        public String getOtp() { return otp; }
        public long getExpiry() { return expiry; }
    }
    
    public void changePassword(
    	    Long userId,
    	    String currentPassword,
    	    String newPassword
    	) throws Exception {

    	    User user = userRepository.findById(userId)
    	        .orElseThrow(() -> new Exception("User không tồn tại"));

    	    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
    	        throw new Exception("Mật khẩu hiện tại không đúng");
    	    }

    	    user.setPassword(passwordEncoder.encode(newPassword));
    	    user.setPasswordUpdatedAt(LocalDateTime.now());

    	    userRepository.save(user);
    	}
}
