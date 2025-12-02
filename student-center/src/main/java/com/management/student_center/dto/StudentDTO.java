package com.management.student_center.dto;

import java.time.LocalDate;

public class StudentDTO {
    private Long id;
    private String fullName;
    private Boolean gender;
    private LocalDate dateOfBirth;
    private String schoolName;
    private String grade;
    private UserDTO userInfo; // thêm field này

    public UserDTO getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(UserDTO userInfo) {
		this.userInfo = userInfo;
	}

	public StudentDTO() {}

    public StudentDTO(Long id, String fullName, Boolean gender, LocalDate dateOfBirth, String schoolName, String grade) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.schoolName = schoolName;
        this.grade = grade;
    }
    
    public StudentDTO(Long id, String grade, String schoolName, UserDTO userInfo) {
        this.id = id;
        this.grade = grade;
        this.schoolName = schoolName;
        this.userInfo = userInfo;
    }


    // getter + setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
