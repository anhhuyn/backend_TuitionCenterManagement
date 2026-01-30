package com.management.student_center.repository;

import com.management.student_center.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    List<Student> findByGrade(String grade);
    
    Optional<Student> findByUserInfoId(Long userId);
    
    //Tổng số học sinh
    long count();
    
    // Đếm số học sinh được tạo trong khoảng thời gian
    long countByCreatedAtBetween( LocalDateTime start, LocalDateTime end);
}
