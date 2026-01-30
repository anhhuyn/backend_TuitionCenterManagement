package com.management.student_center.controller;

import com.management.student_center.entity.User;
import com.management.student_center.service.LoginService;
import com.management.student_center.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api")
public class AuthController {

    private final LoginService loginService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    // ========================
    // LOGIN
    // ========================
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOpt = loginService.handleUserLogin(email, password);
        Map<String, Object> res = new HashMap<>();

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String token = JwtUtil.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRoleId(),
                    jwtSecret,
                    3600 * 1000 // 1 giờ
            );

            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(3600); // 1 giờ
            cookie.setPath("/");
            response.addCookie(cookie);

            res.put("message", "Đăng nhập thành công!");
            res.put("token", token);
            res.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName(),
                    "phoneNumber", user.getPhoneNumber(),
                    "gender", user.getGender(),
                    "image", user.getImage(),
                    "roleId", user.getRoleId()
            ));

            return ResponseEntity.ok(res);
        } else {
            res.put("message", "Email hoặc mật khẩu không đúng!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
    }

    // ========================
    // LOGOUT
    // ========================
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Đăng xuất thành công!");
        return res;
    }

    // ========================
    // GET CURRENT USER  (/auth/me)
    // ========================
    @GetMapping("/auth/me")
    public Map<String, Object> getCurrentUser(HttpServletRequest request) {

        // Lấy user đã được JwtFilter inject
        User user = (User) request.getAttribute("user");

        if (user == null) {
            return Map.of("message", "Token không hợp lệ hoặc chưa đăng nhập");
        }

        Map<String, Object> res = new HashMap<>();
        res.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "phoneNumber", user.getPhoneNumber(),
                "gender", user.getGender(),
                "image", user.getImage(),
                "roleId", user.getRoleId(),
                "createdAt", user.getCreatedAt()
        ));

        return res;
    }
}
