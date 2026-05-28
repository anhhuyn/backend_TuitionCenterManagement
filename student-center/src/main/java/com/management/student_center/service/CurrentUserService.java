package com.management.student_center.service;

import com.management.student_center.entity.User;
import com.management.student_center.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }
        
        Object principal = authentication.getPrincipal();
        User user = null;
        
        // 👇 Xử lý cả 2 trường hợp
        if (principal instanceof User) {
            // Nếu principal là object User (cách hiện tại)
            user = (User) principal;
        } else if (principal instanceof String) {
            // Nếu principal là email string
            String email = (String) principal;
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        } else {
            throw new RuntimeException("Không thể xác định thông tin người dùng");
        }
        
        if (user == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }
        
        return user;
    }
    
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}