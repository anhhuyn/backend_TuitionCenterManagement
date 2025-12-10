package com.management.student_center.dto.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SalaryCalculationDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private BigDecimal totalAmount; // Đã là BigDecimal (Chuẩn)
    private List<SubjectSalaryDTO> subjects = new ArrayList<>();

    // Getters & Setters giữ nguyên, chỉ đảm bảo totalAmount là BigDecimal
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public List<SubjectSalaryDTO> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectSalaryDTO> subjects) { this.subjects = subjects; }

    public static class SubjectSalaryDTO {
        private Long subjectId;
        private String subjectName;
        
        // SỬA: Float -> BigDecimal
        private BigDecimal salaryRate; 
        
        private int totalSessions;
        private Float totalHours; // Giờ giữ Float/Double ok
        
        // SỬA: Float -> BigDecimal
        private BigDecimal totalMoney; 

        // Getters & Setters
        public Long getSubjectId() { return subjectId; }
        public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        
        public BigDecimal getSalaryRate() { return salaryRate; }
        public void setSalaryRate(BigDecimal salaryRate) { this.salaryRate = salaryRate; }
        
        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        public Float getTotalHours() { return totalHours; }
        public void setTotalHours(Float totalHours) { this.totalHours = totalHours; }
        
        public BigDecimal getTotalMoney() { return totalMoney; }
        public void setTotalMoney(BigDecimal totalMoney) { this.totalMoney = totalMoney; }
    }
}