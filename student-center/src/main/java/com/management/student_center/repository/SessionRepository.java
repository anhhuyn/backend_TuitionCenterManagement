package com.management.student_center.repository;

import com.management.student_center.entity.Room;
import com.management.student_center.entity.Session;
import com.management.student_center.entity.Subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
	
	 List<Session> findBySubject_IdOrderBySessionDateAsc(Long subjectId);

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.room r LEFT JOIN FETCH s.schedule sch " +
           "WHERE s.subject.id = :subjectId " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<Session> findBySubjectIdWithRoomAndScheduleOrder(@Param("subjectId") Long subjectId);

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.room r LEFT JOIN FETCH s.schedule sch WHERE s.id = :id")
    Session findByIdWithRoomAndSchedule(@Param("id") Long id);
    
 // --- Method check session tồn tại ---
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Session s " +
           "WHERE s.subject = :subject " +
           "AND s.sessionDate = :sessionDate " +
           "AND s.startTime = :startTime")
    boolean existsBySubjectAndSessionDateAndStartTime(
            @Param("subject") Subject subject,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime
    );
    
 // Kiểm tra trùng giờ phòng
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Session s " +
           "WHERE s.room = :room " +
           "AND s.sessionDate = :date " +
           "AND (" +
           "  (s.startTime <= :start AND s.endTime > :start) OR " +
           "  (s.startTime < :end AND s.endTime >= :end) OR " +
           "  (s.startTime >= :start AND s.endTime <= :end)" +
           ")")
    boolean existsByRoomAndDateAndTimeOverlap(
            @Param("room") Room room,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end
    );

    // Kiểm tra trùng giờ lớp
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Session s " +
           "WHERE s.subject = :subject " +
           "AND s.sessionDate = :date " +
           "AND (" +
           "  (s.startTime <= :start AND s.endTime > :start) OR " +
           "  (s.startTime < :end AND s.endTime >= :end) OR " +
           "  (s.startTime >= :start AND s.endTime <= :end)" +
           ")")
    boolean existsBySubjectAndDateAndTimeOverlap(
            @Param("subject") Subject subject,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end
    );

    // Kiểm tra trùng giờ phòng, trừ chính session
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Session s " +
           "WHERE s.room = :room " +
           "AND s.sessionDate = :date " +
           "AND s.id <> :excludeId " +
           "AND (" +
           "  (s.startTime <= :start AND s.endTime > :start) OR " +
           "  (s.startTime < :end AND s.endTime >= :end) OR " +
           "  (s.startTime >= :start AND s.endTime <= :end)" +
           ")")
    boolean existsByRoomAndDateAndTimeOverlapExcludingId(
            @Param("room") Room room,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("excludeId") Long excludeId
    );

    // Kiểm tra trùng giờ lớp, trừ chính session
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM Session s " +
           "WHERE s.subject = :subject " +
           "AND s.sessionDate = :date " +
           "AND s.id <> :excludeId " +
           "AND (" +
           "  (s.startTime <= :start AND s.endTime > :start) OR " +
           "  (s.startTime < :end AND s.endTime >= :end) OR " +
           "  (s.startTime >= :start AND s.endTime <= :end)" +
           ")")
    boolean existsBySubjectAndDateAndTimeOverlapExcludingId(
            @Param("subject") Subject subject,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("excludeId") Long excludeId
    );

}
