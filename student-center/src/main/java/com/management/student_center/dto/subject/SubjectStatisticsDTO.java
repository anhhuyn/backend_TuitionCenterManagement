package com.management.student_center.dto.subject;

public class SubjectStatisticsDTO {
	
	private long totalSubjects;
	private long totalSubjectsThisMonth;
	private double percentageIncreaseSubject;
	public SubjectStatisticsDTO(long totalSubjects, long totalSubjectsThisMonth, double percentageIncreaseSubject) {
		super();
		this.totalSubjects = totalSubjects;
		this.totalSubjectsThisMonth = totalSubjectsThisMonth;
		this.percentageIncreaseSubject = percentageIncreaseSubject;
	}
	public long getTotalSubjects() {
		return totalSubjects;
	}
	public void setTotalSubjects(long totalSubjects) {
		this.totalSubjects = totalSubjects;
	}
	public long getTotalSubjectsThisMonth() {
		return totalSubjectsThisMonth;
	}
	public void setTotalSubjectsThisMonth(long totalSubjectsThisMonth) {
		this.totalSubjectsThisMonth = totalSubjectsThisMonth;
	}
	public double getPercentageIncreaseSubject() {
		return percentageIncreaseSubject;
	}
	public void setPercentageIncreaseSubject(double percentageIncreaseSubject) {
		this.percentageIncreaseSubject = percentageIncreaseSubject;
	}
	
	

}
