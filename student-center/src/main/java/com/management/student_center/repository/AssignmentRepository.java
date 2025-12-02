package com.management.student_center.repository;

import com.management.student_center.entity.Assignment;
import com.management.student_center.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findBySession_Subject_IdOrderByDueDateAsc(Long subjectId);
}
