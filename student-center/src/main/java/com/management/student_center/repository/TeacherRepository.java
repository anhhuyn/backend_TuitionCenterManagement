package com.management.student_center.repository;

import com.management.student_center.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; 
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {
    
    Optional<Teacher> findByUserInfoId(Long userId); 
    List<Teacher> findAllByTeacherSubjects_Subject_Id(Long subjectId);
    
    long count();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end); 
}