package com.management.student_center.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "assignments")
public class Assignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	private LocalDateTime dueDate;

	@CreationTimestamp
	private LocalDateTime createdAt;

	private String file;

	// Quan hệ với Session
	@ManyToOne
	@JoinColumn(name = "sessionId", nullable = false)
	private Session session;

	// Quan hệ với StudentAssignment
	@OneToMany(mappedBy = "assignment")
	private List<StudentAssignment> studentAssignments;

	public Assignment() {
	}

	// Getter & Setter
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDateTime dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public List<StudentAssignment> getStudentAssignments() {
		return studentAssignments;
	}

	public void setStudentAssignments(List<StudentAssignment> studentAssignments) {
		this.studentAssignments = studentAssignments;
	}
}
