package com.management.student_center.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

import java.util.List;

@Entity
@Table(name = "announcements")
public class Announcement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "adminId", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_announcement_admin"))
	private User admin;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String content;

	private String imageURL;

	@Column(nullable = false)
	private String status = "active";

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> attachments;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Announcement() {
	}

	// Getter / Setter
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getAdmin() {
		return admin;
	}

	public void setAdmin(User admin) {
		this.admin = admin;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public List<String> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<String> attachments) {
		this.attachments = attachments;
	}
}
