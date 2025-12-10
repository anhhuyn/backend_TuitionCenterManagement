package com.management.student_center.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // <--- NHỚ IMPORT CÁI NÀY
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String details;

    private String ward;

    private String province;

    // Quan hệ với Teacher
    @OneToMany(mappedBy = "addressInfo")
    @JsonIgnore // <--- THÊM DÒNG NÀY: Ngắt vòng lặp Teacher -> Address -> Teacher...
    private List<Teacher> teachers;

    // Quan hệ với Student
    @OneToMany(mappedBy = "addressInfo")
    @JsonIgnore // <--- THÊM DÒNG NÀY LUÔN: Để tránh lỗi tương tự với học sinh
    private List<Student> students;

    // Constructor mặc định
    public Address() {}

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public List<Teacher> getTeachers() { return teachers; }
    public void setTeachers(List<Teacher> teachers) { this.teachers = teachers; }

    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }
}