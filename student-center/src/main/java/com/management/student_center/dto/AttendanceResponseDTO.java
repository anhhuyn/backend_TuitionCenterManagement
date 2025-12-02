package com.management.student_center.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AttendanceResponseDTO {

    private Long subjectId;
    private List<SessionDTO> sessions;
    private List<StudentAttendanceDTO> students;

    public record SessionDTO(Long sessionId, LocalDate date, LocalTime startTime, LocalTime endTime) {}
    public record AttendanceItem(Long sessionId, String status, String note) {}
    public record StudentAttendanceDTO(Long studentId, String fullName, List<AttendanceItem> attendances) {}
	public Long getSubjectId() {
		return subjectId;
	}
	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}
	public List<SessionDTO> getSessions() {
		return sessions;
	}
	public void setSessions(List<SessionDTO> sessions) {
		this.sessions = sessions;
	}
	public List<StudentAttendanceDTO> getStudents() {
		return students;
	}
	public void setStudents(List<StudentAttendanceDTO> students) {
		this.students = students;
	}

}
