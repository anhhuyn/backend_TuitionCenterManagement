package com.management.student_center.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @JsonIgnore
    private String password;

    @Column(name = "fullName")
    private String fullName;

    @Column(name = "phoneNumber")
    private String phoneNumber;

    private Boolean gender;

    private String image;

    @Column(name = "roleId")
    private String roleId;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "userInfo", cascade = CascadeType.ALL)
    private Teacher teacherInfo;

    @JsonIgnore
    @OneToOne(mappedBy = "userInfo", cascade = CascadeType.ALL)
    private Student studentInfo;

    // Constructor mặc định
    public User() {}

    // Constructor đầy đủ
    public User(Long id, String email, String password, String fullName, String phoneNumber,
                Boolean gender, String image, String roleId, Teacher teacherInfo, Student studentInfo) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.image = image;
        this.roleId = roleId;
        this.teacherInfo = teacherInfo;
        this.studentInfo = studentInfo;
    }

    // Getter và Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public Teacher getTeacherInfo() { return teacherInfo; }
    public void setTeacherInfo(Teacher teacherInfo) { this.teacherInfo = teacherInfo; }

    public Student getStudentInfo() { return studentInfo; }
    public void setStudentInfo(Student studentInfo) { this.studentInfo = studentInfo; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }

    public void setPasswordUpdatedAt(LocalDateTime passwordUpdatedAt) {
        this.passwordUpdatedAt = passwordUpdatedAt;
    }

}
