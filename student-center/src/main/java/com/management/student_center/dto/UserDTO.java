// UserDTO.java
package com.management.student_center.dto;

public class UserDTO {
    private Long id;
    private String fullName;
    private String email;
    private Boolean gender;
    private String phoneNumber;
    private String image; 
    
    public UserDTO() {}

    public UserDTO(String fullName, String email, String phoneNumber, Boolean gender, String image) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.image = image;
    }
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Boolean getGender() {
		return gender;
	}
	public void setGender(Boolean gender) {
		this.gender = gender;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    
}
