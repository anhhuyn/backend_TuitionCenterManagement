package com.management.student_center.repository;

import com.management.student_center.entity.StudentTuition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentTuitionRepository extends JpaRepository<StudentTuition, Long> {
	// Tìm hóa đơn theo ghi chú để check trùng (hoặc bạn có thể query theo studentId
	// + month + year)
	List<StudentTuition> findByNotesContaining(String note);

	// Tìm hóa đơn của 1 học sinh theo tháng năm (để tránh tạo trùng)
	boolean existsByStudentIdAndMonthAndYear(Long studentId, int month, int year);

	List<StudentTuition> findByMonthAndYear(int month, int year);

	Optional<StudentTuition> findByStudentIdAndMonthAndYear(Long studentId, int month, int year);
}