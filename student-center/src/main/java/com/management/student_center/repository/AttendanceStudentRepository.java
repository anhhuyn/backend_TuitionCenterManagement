	package com.management.student_center.repository;
	
	import com.management.student_center.entity.AttendanceStudent;
	import com.management.student_center.entity.Session;
	import com.management.student_center.entity.Student;
	import org.springframework.data.jpa.repository.JpaRepository;
	
	import java.util.List;
	import java.util.Optional;
	
	public interface AttendanceStudentRepository extends JpaRepository<AttendanceStudent, Long> {
	
	    Optional<AttendanceStudent> findBySessionAndStudent(Session session, Student student);
	
	    List<AttendanceStudent> findAllByStudentAndSessionIn(Student student, List<Session> sessions);
	}
