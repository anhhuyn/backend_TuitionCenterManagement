package com.management.student_center.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StudentAssignmentResponseDTO {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String submittedStatus;
    private String feedback;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @JsonProperty("Student")
    private StudentDTO student;

    public StudentAssignmentResponseDTO(Long id, Long assignmentId, Long studentId,
                                        String submittedStatus, String feedback,
                                       
                                        StudentDTO student) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.submittedStatus = submittedStatus;
        this.feedback = feedback;
        
        this.student = student;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAssignmentId() {
		return assignmentId;
	}

	public void setAssignmentId(Long assignmentId) {
		this.assignmentId = assignmentId;
	}

	public Long getStudentId() {
		return studentId;
	}

	public void setStudentId(Long studentId) {
		this.studentId = studentId;
	}

	public String getSubmittedStatus() {
		return submittedStatus;
	}

	public void setSubmittedStatus(String submittedStatus) {
		this.submittedStatus = submittedStatus;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public StudentDTO getStudent() {
		return student;
	}

	public void setStudent(StudentDTO student) {
		this.student = student;
	}

    
}
