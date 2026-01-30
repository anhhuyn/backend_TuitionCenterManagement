package com.management.student_center.repository;

import com.management.student_center.entity.Subject;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findByStatus(String status, Pageable pageable);
    List<Subject> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT COUNT(ss) FROM StudentSubject ss WHERE ss.subject.id = :subjectId")
    long countCurrentStudents(Long subjectId);
    
    // ------------------- Lấy môn học theo teacher -------------------
    @Query("SELECT s FROM Subject s JOIN s.teacherSubjects ts WHERE ts.teacher.userInfo.id = :userId")
    Page<Subject> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM Subject s JOIN s.teacherSubjects ts WHERE ts.teacher.userInfo.id = :userId AND s.status = :status")
    Page<Subject> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, Pageable pageable);
    
    long count();
    long countByCreatedAtBetween( LocalDateTime start, LocalDateTime end);

}
