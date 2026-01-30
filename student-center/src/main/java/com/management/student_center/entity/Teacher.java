package com.management.student_center.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ với User
    @OneToOne
    @JoinColumn(name = "userId")
    private User userInfo;

    // Quan hệ với Address
    @ManyToOne
    @JoinColumn(name = "addressId")
    private Address addressInfo;

    private LocalDate dateOfBirth;

    private String specialty;

    // Quan hệ với TeacherSubject
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TeacherSubject> teacherSubjects;

    // Quan hệ với TeacherPayment
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TeacherPayment> teacherPayments;
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Constructor mặc định
    public Teacher() {}

    // Constructor đầy đủ
    public Teacher(Long id, User userInfo, Address addressInfo, LocalDate dateOfBirth,
                   String specialty, List<TeacherSubject> teacherSubjects,
                   List<TeacherPayment> teacherPayments) {
        this.id = id;
        this.userInfo = userInfo;
        this.addressInfo = addressInfo;
        this.dateOfBirth = dateOfBirth;
        this.specialty = specialty;
        this.teacherSubjects = teacherSubjects;
        this.teacherPayments = teacherPayments;
    }

    // Getter và Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUserInfo() { return userInfo; }
    public void setUserInfo(User userInfo) { this.userInfo = userInfo; }

    public Address getAddressInfo() { return addressInfo; }
    public void setAddressInfo(Address addressInfo) { this.addressInfo = addressInfo; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public List<TeacherSubject> getTeacherSubjects() { return teacherSubjects; }
    public void setTeacherSubjects(List<TeacherSubject> teacherSubjects) { this.teacherSubjects = teacherSubjects; }

    public List<TeacherPayment> getTeacherPayments() { return teacherPayments; }
    public void setTeacherPayments(List<TeacherPayment> teacherPayments) { this.teacherPayments = teacherPayments; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
