package com.management.student_center.dto.student;

public class StudentStatisticDTO {

	private long totalStudents;
	private long newStudentsThisMonth;
	private double percentageIncrease;
	public StudentStatisticDTO(long totalStudents, long newStudentsThisMonth, double percentageIncrease) {
		super();
		this.totalStudents = totalStudents;
		this.newStudentsThisMonth = newStudentsThisMonth;
		this.percentageIncrease = percentageIncrease;
	}
	public long getTotalStudents() {
		return totalStudents;
	}
	public void setTotalStudents(long totalStudents) {
		this.totalStudents = totalStudents;
	}
	public long getNewStudentsThisMonth() {
		return newStudentsThisMonth;
	}
	public void setNewStudentsThisMonth(long newStudentsThisMonth) {
		this.newStudentsThisMonth = newStudentsThisMonth;
	}
	public double getPercentageIncrease() {
		return percentageIncrease;
	}
	public void setPercentageIncrease(double percentageIncrease) {
		this.percentageIncrease = percentageIncrease;
	}
	
}
