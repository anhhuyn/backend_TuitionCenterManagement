package com.management.student_center.service;

import com.management.student_center.dto.AssignmentDTO;
import com.management.student_center.entity.Assignment;
import com.management.student_center.repository.AssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

	private final AssignmentRepository assignmentRepository;
	private final String uploadDir = System.getProperty("user.dir") + "/uploads/assignments";

	public AssignmentService(AssignmentRepository assignmentRepository) {
		this.assignmentRepository = assignmentRepository;
		File dir = new File(uploadDir);
		if (!dir.exists())
			dir.mkdirs();
	}

	public AssignmentDTO createAssignment(Long sessionId, String title, String description,
			java.time.LocalDateTime dueDate, MultipartFile file) throws IOException {
		Assignment assignment = new Assignment();
		assignment.setTitle(title);
		assignment.setDescription(description);
		assignment.setDueDate(dueDate);
		assignment.setSession(new com.management.student_center.entity.Session());
		assignment.getSession().setId(sessionId);

		if (file != null && !file.isEmpty()) {
			String fileName = saveFile(file);
			assignment.setFile("/uploads/assignments/" + fileName);
		}

		assignmentRepository.save(assignment);
		return mapToDTO(assignment);
	}

	public AssignmentDTO updateAssignment(Long assignmentId, String title, String description,
			java.time.LocalDateTime dueDate, MultipartFile file) throws IOException {
		Assignment assignment = assignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy assignment"));

		if (title != null)
			assignment.setTitle(title);
		if (description != null)
			assignment.setDescription(description);
		if (dueDate != null)
			assignment.setDueDate(dueDate);

		if (file != null && !file.isEmpty()) {
			if (assignment.getFile() != null) {
				Path oldFile = Paths.get(System.getProperty("user.dir") + "/public" + assignment.getFile());
				Files.deleteIfExists(oldFile);
			}
			String fileName = saveFile(file);
			assignment.setFile("/uploads/assignments/" + fileName);
		}

		assignmentRepository.save(assignment);
		return mapToDTO(assignment);
	}

	public void deleteAssignment(Long assignmentId) throws IOException {
		Assignment assignment = assignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy assignment"));

		if (assignment.getFile() != null) {
			Path oldFile = Paths.get(System.getProperty("user.dir") + "/public" + assignment.getFile());
			Files.deleteIfExists(oldFile);
		}

		assignmentRepository.delete(assignment);
	}

	public List<AssignmentDTO> getAssignmentsBySubject(Long subjectId) {
		List<Assignment> assignments = assignmentRepository.findBySession_Subject_IdOrderByDueDateAsc(subjectId);
		return assignments.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	private String saveFile(MultipartFile file) throws IOException {
		String fileName = System.currentTimeMillis() + "-" + Math.round(Math.random() * 1e9)
				+ getFileExtension(file.getOriginalFilename());
		Path filePath = Paths.get(uploadDir, fileName);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
		return fileName;
	}

	private String getFileExtension(String filename) {
		return filename.substring(filename.lastIndexOf("."));
	}

	private AssignmentDTO mapToDTO(Assignment ass) {
		String fileSize = "Không xác định";
		if (ass.getFile() != null) {
			File f = new File(System.getProperty("user.dir") + "/public" + ass.getFile());
			if (f.exists()) {
				double sizeMB = f.length() / (1024.0 * 1024.0);
				fileSize = String.format("%.2f MB", sizeMB);
			}
		}

		return new AssignmentDTO(ass.getId(), ass.getTitle(), ass.getDescription(), ass.getDueDate(), ass.getFile(),
				fileSize, new AssignmentDTO.SessionInfoDTO(ass.getSession().getId(), ass.getSession().getSessionDate(),
						ass.getSession().getStartTime(), ass.getSession().getEndTime()),
				ass.getCreatedAt());
	}
}