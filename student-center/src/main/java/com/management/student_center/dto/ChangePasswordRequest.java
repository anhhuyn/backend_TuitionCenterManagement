package com.management.student_center.dto;

public record ChangePasswordRequest(
		  String currentPassword,
		  String newPassword
		) {}
