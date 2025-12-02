package com.management.student_center.repository;

import com.management.student_center.entity.StudentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Long> {
    List<StudentAssignment> findByAssignmentId(Long assignmentId);
}
