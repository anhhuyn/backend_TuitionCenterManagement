package com.management.student_center.repository;

import com.management.student_center.entity.StudentSubject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSubjectRepository extends JpaRepository<StudentSubject, Long> {

    Long countBySubjectId(Long subjectId);
    
    void deleteBySubjectId(Long subjectId);
    
    List<StudentSubject> findBySubjectId(Long subjectId);
    Optional<StudentSubject> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    
    List<StudentSubject> findBySubject_Id(Long subjectId);
}
