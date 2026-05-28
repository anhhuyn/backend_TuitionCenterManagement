package com.management.student_center.repository;

import com.management.student_center.entity.StudentSubject;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSubjectRepository extends JpaRepository<StudentSubject, Long> {

	Long countBySubjectId(Long subjectId);

	void deleteBySubjectId(Long subjectId);

	List<StudentSubject> findBySubjectId(Long subjectId);

	@Query("SELECT ss FROM StudentSubject ss WHERE ss.subject.id = :subjectId AND ss.deletedAt IS NULL")
    List<StudentSubject> findActiveBySubjectId(@Param("subjectId") Long subjectId);

	Optional<StudentSubject> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
	
	List<StudentSubject> findBySubject_Id(Long subjectId);
	
	 @Query("SELECT ss FROM StudentSubject ss WHERE ss.student.id = :studentId AND ss.deletedAt IS NULL")
	    List<StudentSubject> findActiveByStudentId(@Param("studentId") Long studentId);

	void deleteByStudentId(Long studentId);

	List<StudentSubject> findByStudentId(Long studentId);
	
	 // Lấy student subjects active tại 1 thời điểm (dùng để kiểm tra)
    @Query("SELECT ss FROM StudentSubject ss WHERE ss.subject.id = :subjectId " +
           "AND ss.enrollmentDate <= :date " +
           "AND (ss.deletedAt IS NULL OR ss.deletedAt > :date)")
    List<StudentSubject> findActiveStudentsBySubjectAndDate(
        @Param("subjectId") Long subjectId, 
        @Param("date") LocalDate date);
    
    // Optional: Lấy tất cả học sinh chưa bị xóa (không cần check thời gian)
    List<StudentSubject> findBySubject_IdAndDeletedAtIsNull(Long subjectId);
    
    @Query("SELECT COUNT(ss) FROM StudentSubject ss WHERE ss.subject.id = :subjectId AND ss.deletedAt IS NULL")
    long countActiveBySubjectId(@Param("subjectId") Long subjectId);
}
