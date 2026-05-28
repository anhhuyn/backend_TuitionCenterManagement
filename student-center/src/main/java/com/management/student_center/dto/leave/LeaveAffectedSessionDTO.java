package com.management.student_center.dto.leave;

import java.time.LocalTime;

public class LeaveAffectedSessionDTO {
    private Long id;
    private Long sessionId;
    private String sessionDate;
    private String status;
    private String replacementResponse;

    private Long leaveId;
    private Long originalTeacherId;
    
    private Long replacementTeacherId;
    private String replacementTeacherName;
    private String className;
    private String subjectName;
    private String roomName;

    private LocalTime startTime;
    private LocalTime endTime;

    private String originalTeacherName;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getSubjectName() {
		return subjectName;
	}
	public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}
	public String getRoomName() {
		return roomName;
	}
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	public LocalTime getStartTime() {
		return startTime;
	}
	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}
	public LocalTime getEndTime() {
		return endTime;
	}
	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}
	public String getOriginalTeacherName() {
		return originalTeacherName;
	}
	public void setOriginalTeacherName(String originalTeacherName) {
		this.originalTeacherName = originalTeacherName;
	}
	public Long getSessionId() {
		return sessionId;
	}
	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}
	public String getSessionDate() {
		return sessionDate;
	}
	public void setSessionDate(String sessionDate) {
		this.sessionDate = sessionDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReplacementResponse() {
		return replacementResponse;
	}
	public void setReplacementResponse(String replacementResponse) {
		this.replacementResponse = replacementResponse;
	}
	public Long getLeaveId() {
		return leaveId;
	}
	public void setLeaveId(Long leaveId) {
		this.leaveId = leaveId;
	}
	public Long getOriginalTeacherId() {
		return originalTeacherId;
	}
	public void setOriginalTeacherId(Long originalTeacherId) {
		this.originalTeacherId = originalTeacherId;
	}
	
	public Long getReplacementTeacherId() {
	    return replacementTeacherId;
	}

	public void setReplacementTeacherId(Long replacementTeacherId) {
	    this.replacementTeacherId = replacementTeacherId;
	}

	public String getReplacementTeacherName() {
	    return replacementTeacherName;
	}

	public void setReplacementTeacherName(String replacementTeacherName) {
	    this.replacementTeacherName = replacementTeacherName;
	}
}