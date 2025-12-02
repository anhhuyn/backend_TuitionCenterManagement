package com.management.student_center.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class AssignmentDTO {
	private Long id;
	private String title;
	private String description;
	private LocalDateTime dueDate;
	private String file;
	private String fileSize;
	private LocalDateTime createdAt;
	private Long sessionId;

	@JsonProperty("Session")
	private SessionInfoDTO session;

	public AssignmentDTO() {
	}

	public AssignmentDTO(Long id, String title, String description, LocalDateTime dueDate, String file, String fileSize,
			SessionInfoDTO session, LocalDateTime createdAt) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.file = file;
		this.fileSize = fileSize;
		this.session = session;
		this.sessionId = session != null ? session.getId() : null;
		this.createdAt = createdAt;
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
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

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}

	public SessionInfoDTO getSession() {
		return session;
	}

	public void setSession(SessionInfoDTO session) {
		this.session = session;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public static class SessionInfoDTO {
		private Long id;
		private LocalDate sessionDate;
		private LocalTime startTime;
		private LocalTime endTime;

		public SessionInfoDTO() {
		}

		public SessionInfoDTO(Long id, LocalDate sessionDate, LocalTime startTime, LocalTime endTime) {
			this.id = id;
			this.sessionDate = sessionDate;
			this.startTime = startTime;
			this.endTime = endTime;
		}

		// Getter & Setter
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalDate getSessionDate() {
			return sessionDate;
		}

		public void setSessionDate(LocalDate sessionDate) {
			this.sessionDate = sessionDate;
		}

		public LocalTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalTime startTime) {
			this.startTime = startTime;
		}

		public LocalTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalTime endTime) {
			this.endTime = endTime;
		}
	}
}
