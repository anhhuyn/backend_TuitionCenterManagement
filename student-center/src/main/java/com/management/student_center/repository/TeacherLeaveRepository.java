package com.management.student_center.repository;

import com.management.student_center.entity.TeacherLeave;
import com.management.student_center.entity.TeacherLeave.LeaveStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TeacherLeaveRepository
		extends JpaRepository<TeacherLeave, Long>, JpaSpecificationExecutor<TeacherLeave> {

	// ================= LIST =================

	Page<TeacherLeave> findByTeacherId(Long teacherId, Pageable pageable);

	Page<TeacherLeave> findByStatus(LeaveStatus status, Pageable pageable);

	Page<TeacherLeave> findByTeacherIdAndStatus(Long teacherId, LeaveStatus status, Pageable pageable);

	// ================= COUNT =================

	long countByStatus(LeaveStatus status);

	// ================= CHECK OVERLAP =================

	/**
	 * Check nghỉ trùng ngày + giờ
	 */
	@Query("""
			    SELECT COUNT(l)
			    FROM TeacherLeave l
			    WHERE l.teacher.id = :teacherId
			    AND l.status IN (
			        com.management.student_center.entity.TeacherLeave$LeaveStatus.PENDING,
			        com.management.student_center.entity.TeacherLeave$LeaveStatus.APPROVED
			    )
			    AND l.startDate <= :endDate
			    AND l.endDate >= :startDate

			    AND (
			        (
			            l.startTime IS NULL
			            OR l.endTime IS NULL
			        )
			        OR
			        (
			            :startTime < l.endTime
			            AND :endTime > l.startTime
			        )
			    )
			""")
	int countOverlappingLeave(@Param("teacherId") Long teacherId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate, @Param("startTime") LocalTime startTime,
			@Param("endTime") LocalTime endTime);

	@Query("""
			    SELECT CASE WHEN COUNT(tl) > 0 THEN true ELSE false END
			    FROM TeacherLeave tl
			    WHERE tl.teacher.id = :teacherId
			    AND tl.status IN ('PENDING', 'APPROVED')
			    AND :date BETWEEN tl.startDate AND tl.endDate
			    AND (
			        tl.startTime IS NULL
			        OR tl.endTime IS NULL
			        OR (
			            tl.startTime < :endTime
			            AND tl.endTime > :startTime
			        )
			    )
			""")
	boolean existsTeacherLeaveOverlap(@Param("teacherId") Long teacherId, @Param("date") LocalDate date,
			@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

	@Query("""
			    SELECT tl FROM TeacherLeave tl
			    WHERE tl.teacher.id = :teacherId
			    AND :date BETWEEN tl.startDate AND tl.endDate
			""")
	List<TeacherLeave> findByTeacherIdAndDate(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);

	@Query("""
			    SELECT tl
			    FROM TeacherLeave tl
			    WHERE tl.status = 'APPROVED'
			    AND tl.startDate <= :weekEnd
			    AND tl.endDate >= :weekStart
			""")
	List<TeacherLeave> findApprovedLeavesInWeek(LocalDate weekStart, LocalDate weekEnd);

	// ✅ Method 1: Filter cho ADMIN
	@Query("SELECT DISTINCT tl FROM TeacherLeave tl " + "LEFT JOIN FETCH tl.teacher t " + "LEFT JOIN FETCH t.userInfo "
			+ "WHERE " + "(:status IS NULL OR tl.status = :status) "
			+ "AND (:startDate IS NULL OR tl.endDate >= :startDate) " + // Đơn kết thúc SAU ngày bắt đầu
			"AND (:endDate IS NULL OR tl.startDate <= :endDate)") // Đơn bắt đầu TRƯỚC ngày kết thúc
	Page<TeacherLeave> findByFilters(@Param("status") TeacherLeave.LeaveStatus status,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

	// ✅ Method 2: Filter cho TEACHER (có teacherId)
	@Query("SELECT DISTINCT tl FROM TeacherLeave tl " + "LEFT JOIN FETCH tl.teacher t " + "LEFT JOIN FETCH t.userInfo "
			+ "WHERE " + "tl.teacher.id = :teacherId " + "AND (:status IS NULL OR tl.status = :status) "
			+ "AND (:startDate IS NULL OR tl.endDate >= :startDate) "
			+ "AND (:endDate IS NULL OR tl.startDate <= :endDate)")
	Page<TeacherLeave> findByTeacherIdAndFilters(@Param("teacherId") Long teacherId,
			@Param("status") TeacherLeave.LeaveStatus status, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate, Pageable pageable);

	// ✅ Method 3: Count query cho pagination (nếu cần performance)
	@Query("SELECT COUNT(tl) FROM TeacherLeave tl WHERE " + "(:status IS NULL OR tl.status = :status) "
			+ "AND (:startDate IS NULL OR tl.endDate >= :startDate) "
			+ "AND (:endDate IS NULL OR tl.startDate <= :endDate)")
	long countByFilters(@Param("status") TeacherLeave.LeaveStatus status, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

}
