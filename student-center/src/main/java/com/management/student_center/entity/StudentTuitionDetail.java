package com.management.student_center.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "student_tuition_details")
public class StudentTuitionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_tuition_id")
    @JsonBackReference
    private StudentTuition studentTuition;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private int attendedSessions;   // Số buổi đi học
    private float totalHours;       // Tổng số giờ học
    private BigDecimal hourlyRate;  // Giá tiền 1 giờ (Snapshot)
    private BigDecimal totalMoney;  // totalHours * hourlyRate

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentTuition getStudentTuition() { return studentTuition; }
    public void setStudentTuition(StudentTuition studentTuition) { this.studentTuition = studentTuition; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public int getAttendedSessions() { return attendedSessions; }
    public void setAttendedSessions(int attendedSessions) { this.attendedSessions = attendedSessions; }
    public float getTotalHours() { return totalHours; }
    public void setTotalHours(float totalHours) { this.totalHours = totalHours; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public BigDecimal getTotalMoney() { return totalMoney; }
    public void setTotalMoney(BigDecimal totalMoney) { this.totalMoney = totalMoney; }
}