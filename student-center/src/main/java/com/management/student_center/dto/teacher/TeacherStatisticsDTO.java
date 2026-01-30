package com.management.student_center.dto.teacher;

public class TeacherStatisticsDTO {

    private long totalTeachers;              
    private long totalTeachersThisMonth;     
    private double percentageIncreaseTeacher;  

    public TeacherStatisticsDTO(long totalTeachers, long totalTeachersThisMonth, double percentageIncreaseTeacher) {
        this.totalTeachers = totalTeachers;
        this.totalTeachersThisMonth = totalTeachersThisMonth;
        this.percentageIncreaseTeacher = percentageIncreaseTeacher;
    }

    // Getter và Setter
    public long getTotalTeachers() {
        return totalTeachers;
    }

    public void setTotalTeachers(long totalTeachers) {
        this.totalTeachers = totalTeachers;
    }

    public long getTotalTeachersThisMonth() {
        return totalTeachersThisMonth;
    }

    public void setTotalTeachersThisMonth(long totalTeachersThisMonth) {
        this.totalTeachersThisMonth = totalTeachersThisMonth;
    }

    public double getPercentageIncreaseTeacher() {
        return percentageIncreaseTeacher;
    }

    public void setPercentageIncreaseTeacher(double percentageIncreaseTeacher) {
        this.percentageIncreaseTeacher = percentageIncreaseTeacher;
    }
}
