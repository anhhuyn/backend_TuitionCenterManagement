package com.management.student_center.dto.leave;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import com.management.student_center.entity.TeacherLeave;

import java.time.LocalDate;

@Data
public class TeacherLeaveFilterRequestDTO {
	private Integer page = 1;
	private Integer size = 10;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate startDate; // FROM date

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate endDate; // TO date

	private TeacherLeave.LeaveStatus status;

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public TeacherLeave.LeaveStatus getStatus() {
		return status;
	}

	public void setStatus(TeacherLeave.LeaveStatus status) {
		this.status = status;
	}
}