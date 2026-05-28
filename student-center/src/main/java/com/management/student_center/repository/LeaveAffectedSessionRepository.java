package com.management.student_center.repository;

import com.management.student_center.entity.LeaveAffectedSession;
import com.management.student_center.entity.LeaveAffectedSession.Status;
import com.management.student_center.entity.LeaveAffectedSession.ReplacementResponse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface LeaveAffectedSessionRepository extends JpaRepository<LeaveAffectedSession, Long> {
	Optional<LeaveAffectedSession> findByLeaveIdAndSessionId(Long leaveId, Long sessionId);

	// ================= BASIC =================
	List<LeaveAffectedSession> findByLeaveId(Long leaveId);

	@Modifying
	@Transactional
	void deleteByLeaveId(Long leaveId);

	// ================= ADMIN =================

	/**
	 * Các session chưa xử lý xong
	 */
	List<LeaveAffectedSession> findByStatus(Status status);

	/**
	 * Các session đang chờ giáo viên phản hồi
	 */
	List<LeaveAffectedSession> findByReplacementResponse(ReplacementResponse replacementResponse);

	/**
	 * Session bị teacher từ chối
	 */
	List<LeaveAffectedSession> findByReplacementResponseAndStatus(ReplacementResponse replacementResponse,
			Status status);

	// ================= TEACHER =================

	/**
	 * Session được assign cho teacher
	 */
	List<LeaveAffectedSession> findByReplacementTeacherId(Integer replacementTeacherId);

	/*
	 * Session teacher chưa phản hồi
	 */
	List<LeaveAffectedSession> findByReplacementTeacherIdAndReplacementResponse(Integer replacementTeacherId,
			ReplacementResponse replacementResponse);

	// ================= PROCESS CHECK =================
	long countByLeaveIdAndStatus(Long leaveId, Status status);

	boolean existsByLeaveIdAndStatus(Long leaveId, Status status);

	@Query("""
			    SELECT CASE WHEN COUNT(las) > 0 THEN true ELSE false END
			    FROM LeaveAffectedSession las
			    WHERE las.replacementTeacherId = :teacherId
			    AND las.status = 'PENDING'
			    AND las.session.sessionDate = :date
			    AND (
			        las.session.startTime < :endTime
			        AND las.session.endTime > :startTime
			    )
			""")
	boolean existsReplacementConflict(@Param("teacherId") Integer teacherId, @Param("date") LocalDate date,
			@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
	
	
}

