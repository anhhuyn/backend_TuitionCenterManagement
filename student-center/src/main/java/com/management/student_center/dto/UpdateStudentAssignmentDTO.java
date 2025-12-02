package com.management.student_center.dto;

public class UpdateStudentAssignmentDTO {
    private String submittedStatus;
    private String feedback;
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
   
}
