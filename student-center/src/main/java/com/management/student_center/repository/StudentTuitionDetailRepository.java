package com.management.student_center.repository;

import com.management.student_center.entity.StudentTuitionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentTuitionDetailRepository extends JpaRepository<StudentTuitionDetail, Long> {
}