package com.management.student_center.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacherpaymentdetails")
public class TeacherPaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Float totalHours;
    private Integer totalSessions;
    private BigDecimal salaryRate;
    private BigDecimal totalMoney;

    @ManyToOne
    @JoinColumn(name = "paymentId")
    @JsonBackReference
    private TeacherPayment payment;

    @ManyToOne
    @JoinColumn(name = "subjectId")
    private Subject subject;

    // --- SỬA ĐOẠN NÀY ---
    // Gán giá trị mặc định ngay lập tức để không bao giờ bị null
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Vẫn giữ PreUpdate để khi sửa đổi nó tự cập nhật lại
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    // --------------------
    
    public TeacherPaymentDetail() {}

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Float getTotalHours() { return totalHours; }
    public void setTotalHours(Float totalHours) { this.totalHours = totalHours; }
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    public BigDecimal getSalaryRate() { return salaryRate; }
    public void setSalaryRate(BigDecimal salaryRate) { this.salaryRate = salaryRate; }
    public BigDecimal getTotalMoney() { return totalMoney; }
    public void setTotalMoney(BigDecimal totalMoney) { this.totalMoney = totalMoney; }
    public TeacherPayment getPayment() { return payment; }
    public void setPayment(TeacherPayment payment) { this.payment = payment; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}