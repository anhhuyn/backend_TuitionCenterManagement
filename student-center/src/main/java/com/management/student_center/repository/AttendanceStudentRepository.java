	package com.management.student_center.repository;
	
	import com.management.student_center.entity.AttendanceStudent;
	import com.management.student_center.entity.Session;
	import com.management.student_center.entity.Student;
	import org.springframework.data.jpa.repository.JpaRepository;
	
	import java.util.List;
	import java.util.Optional;
	
	import org.springframework.data.jpa.repository.Query;
	import org.springframework.data.repository.query.Param;
	import java.time.LocalDate;
	
	public interface AttendanceStudentRepository extends JpaRepository<AttendanceStudent, Long> {
	
	    Optional<AttendanceStudent> findBySessionAndStudent(Session session, Student student);
	
	    List<AttendanceStudent> findAllByStudentAndSessionIn(Student student, List<Session> sessions);
	    
	    @Query("SELECT ats FROM AttendanceStudent ats " +
	            "JOIN ats.session s " +
	            "WHERE ats.student.id = :studentId " +
	            "AND s.subject.id = :subjectId " +
	            "AND s.sessionDate BETWEEN :startDate AND :endDate " +
	            "AND s.status = 'completed' " +
	            "AND (ats.status = 'present' OR ats.status = 'late')")
	     List<AttendanceStudent> findValidAttendanceForTuition(
	             @Param("studentId") Long studentId,
	             @Param("subjectId") Long subjectId,
	             @Param("startDate") LocalDate startDate,
	             @Param("endDate") LocalDate endDate
	     );
	}
