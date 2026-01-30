package com.management.student_center.service;

import com.management.student_center.dto.*;
import com.management.student_center.dto.subject.SubjectStatisticsDTO;
import com.management.student_center.entity.StudentTuitionDetail;
import com.management.student_center.entity.Subject;
import com.management.student_center.repository.SubjectRepository;
import com.management.student_center.repository.TeacherPaymentDetailRepository;
import com.management.student_center.repository.TeacherPaymentRepository;
import com.management.student_center.repository.TeacherRepository;
import com.management.student_center.repository.TeacherSubjectRepository;
import com.management.student_center.repository.StudentSubjectRepository;
import com.management.student_center.repository.StudentTuitionDetailRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.management.student_center.entity.Teacher;
import com.management.student_center.entity.TeacherPayment;
import com.management.student_center.entity.TeacherSubject;
import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubjectService {

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private TeacherSubjectRepository teacherSubjectRepository;

	@Autowired
	private StudentSubjectRepository studentSubjectRepository;

	@Autowired
	private TeacherRepository teacherRepository;
	
	@Autowired
	private TeacherPaymentDetailRepository teacherPaymentDetailRepository;

	@Autowired
	private StudentTuitionDetailRepository studentTuitionDetailRepository;


	public Map<String, Object> getSubjectsByUserId(int page, int limit, String status, Long userId) {

		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").ascending());
		Page<Subject> subjectsPage;

		if (status != null) {
			subjectsPage = subjectRepository.findByUserIdAndStatus(userId, status, pageable);
		} else {
			subjectsPage = subjectRepository.findByUserId(userId, pageable);
		}

		List<SubjectDTO> subjectDTOs = subjectsPage.stream().map(subject -> {
			SubjectDTO dto = mapToDTO(subject);
			long current = subjectRepository.countCurrentStudents(subject.getId());
			dto.setCurrentStudents(current);
			return dto;
		}).collect(Collectors.toList());

		// Stats chung cho teacher (optional)
		long countAll = subjectRepository.findByUserId(userId, Pageable.unpaged()).getTotalElements();
		long countActive = subjectRepository.findByUserIdAndStatus(userId, "active", Pageable.unpaged())
				.getTotalElements();
		long countUpcoming = subjectRepository.findByUserIdAndStatus(userId, "upcoming", Pageable.unpaged())
				.getTotalElements();
		long countEnded = subjectRepository.findByUserIdAndStatus(userId, "ended", Pageable.unpaged())
				.getTotalElements();

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", subjectDTOs);
		response.put("total", subjectsPage.getTotalElements());
		response.put("page", page);
		response.put("limit", limit);
		response.put("totalPages", subjectsPage.getTotalPages());

		Map<String, Long> stats = new HashMap<>();
		stats.put("all", countAll);
		stats.put("active", countActive);
		stats.put("upcoming", countUpcoming);
		stats.put("ended", countEnded);

		response.put("stats", stats);
		return response;
	}
	
	// ------------------- GET ALL SUBJECTS (NO PAGINATION) -------------------
	public Map<String, Object> getAllSubjectsNoPaging(String status) {

	    List<Subject> subjects;

	    if (status != null) {
	        subjects = subjectRepository.findByStatus(status);
	    } else {
	        subjects = subjectRepository.findAll();
	    }

	    List<SubjectDTO> subjectDTOs = subjects.stream().map(subject -> {
	        SubjectDTO dto = mapToDTO(subject);
	        long current = subjectRepository.countCurrentStudents(subject.getId());
	        dto.setCurrentStudents(current);
	        return dto;
	    }).collect(Collectors.toList());

	    long countAll = subjectRepository.count();
	    long countActive = subjectRepository.countByStatus("active");
	    long countUpcoming = subjectRepository.countByStatus("upcoming");
	    long countEnded = subjectRepository.countByStatus("ended");

	    Map<String, Long> stats = new HashMap<>();
	    stats.put("all", countAll);
	    stats.put("active", countActive);
	    stats.put("upcoming", countUpcoming);
	    stats.put("ended", countEnded);

	    Map<String, Object> response = new HashMap<>();
	    response.put("success", true);
	    response.put("data", subjectDTOs);
	    response.put("total", subjectDTOs.size());
	    response.put("stats", stats);

	    return response;
	}


	// ------------------- GET ALL SUBJECTS -------------------
	public Map<String, Object> getAllSubjects(int page, int limit, String status) {

		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").ascending());
		Page<Subject> subjectsPage = (status != null) ? subjectRepository.findByStatus(status, pageable)
				: subjectRepository.findAll(pageable);

		long countAll = subjectRepository.count();
		long countActive = subjectRepository.countByStatus("active");
		long countUpcoming = subjectRepository.countByStatus("upcoming");
		long countEnded = subjectRepository.countByStatus("ended");

		List<SubjectDTO> subjectDTOs = subjectsPage.stream().map(subject -> {
			SubjectDTO dto = mapToDTO(subject);
			long current = subjectRepository.countCurrentStudents(subject.getId());
			dto.setCurrentStudents(current);
			return dto;
		}).collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", subjectDTOs);
		response.put("total", subjectsPage.getTotalElements());
		response.put("page", page);
		response.put("limit", limit);
		response.put("totalPages", subjectsPage.getTotalPages());

		Map<String, Long> stats = new HashMap<>();
		stats.put("all", countAll);
		stats.put("active", countActive);
		stats.put("upcoming", countUpcoming);
		stats.put("ended", countEnded);

		response.put("stats", stats);
		return response;
	}

	// ------------------- GET SUBJECT BY ID -------------------
	public SubjectDTO getSubjectById(Long id) {
		Subject subject = subjectRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy môn học"));

		SubjectDTO dto = mapToDTO(subject);
		long current = subjectRepository.countCurrentStudents(id);
		dto.setCurrentStudents(current);
		return dto;
	}

	// ------------------- DELETE SUBJECT -------------------
	@Transactional
	public void deleteSubject(Long id) {

	    Subject subject = subjectRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học"));

	    // 1Kiểm tra nợ lương giáo viên
	    long unpaidTeacherSalary =
	            teacherPaymentDetailRepository.countUnpaidBySubject(id);

	    if (unpaidTeacherSalary > 0) {
	        throw new IllegalStateException("TEACHER_UNPAID");
	    }

	    // 2Kiểm tra học phí học sinh
	    long unpaidStudentTuition =
	            studentTuitionDetailRepository.countUnpaidBySubject(id);

	    if (unpaidStudentTuition > 0) {
	        throw new IllegalStateException("STUDENT_UNPAID");
	    }

	    // 3Xóa các quan hệ trung gian
	    teacherSubjectRepository.deleteBySubjectId(id);
	    studentSubjectRepository.deleteBySubjectId(id);

	    // Xóa môn học
	    subjectRepository.delete(subject);
	}



	// ------------------- UPDATE SUBJECT -------------------
	@Transactional
	public void updateSubject(Long id, UpdateSubjectRequest updatedData) {

	    // 1. Lấy môn học
	    Subject subject = subjectRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học"));

	    // ====== CHECK ĐỔI GIÁO VIÊN KHI CÒN NỢ LƯƠNG ======
	    Long newTeacherId = updatedData.getTeacherId();

	    TeacherSubject existingTS =
	            teacherSubjectRepository.findBySubjectId(id).orElse(null);

	    Long currentTeacherId =
	            existingTS != null ? existingTS.getTeacher().getId() : null;

	    boolean isChangingTeacher =
	            !Objects.equals(currentTeacherId, newTeacherId);

	    if (isChangingTeacher && existingTS != null) {
	        long unpaidSalary =
	                teacherPaymentDetailRepository.countUnpaidBySubject(id);

	        if (unpaidSalary > 0) {
	            throw new IllegalStateException("TEACHER_UNPAID_CHANGE");
	        }
	    }
	    // ====== END CHECK ======

	    // 2. Cập nhật thông tin môn học
	    subject.setName(updatedData.getName());
	    subject.setGrade(updatedData.getGrade());

	    if (updatedData.getPrice() != null) {
	        subject.setPrice(BigDecimal.valueOf(updatedData.getPrice()));
	    }

	    subject.setStatus(updatedData.getStatus());
	    subject.setMaxStudents(updatedData.getMaxStudents());
	    subject.setSessionsPerWeek(updatedData.getSessionsPerWeek());
	    subject.setNote(updatedData.getNote());

	    subjectRepository.save(subject);

	    // 3. Chuẩn hóa teacherId
	    Long teacherIdNorm = updatedData.getTeacherId();

	    if (existingTS != null) {
	        if (teacherIdNorm == null) {
	            // Nếu bỏ giáo viên
	            teacherSubjectRepository.delete(existingTS);
	        } else {
	            // Cập nhật giáo viên
	            Teacher teacher = teacherRepository.findById(teacherIdNorm)
	                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
	            existingTS.setTeacher(teacher);

	            BigDecimal newSalary = updatedData.getSalaryRate() != null
	                    ? BigDecimal.valueOf(updatedData.getSalaryRate())
	                    : existingTS.getSalaryRate();
	            existingTS.setSalaryRate(newSalary);

	            teacherSubjectRepository.save(existingTS);
	        }
	    } else {
	        // Chưa có TeacherSubject nhưng có teacherId → tạo mới
	        if (teacherIdNorm != null) {
	            Teacher teacher = teacherRepository.findById(teacherIdNorm)
	                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
	            TeacherSubject ts = new TeacherSubject();
	            ts.setSubject(subject);
	            ts.setTeacher(teacher);

	            BigDecimal newSalary = updatedData.getSalaryRate() != null
	                    ? BigDecimal.valueOf(updatedData.getSalaryRate())
	                    : BigDecimal.ZERO;
	            ts.setSalaryRate(newSalary);

	            teacherSubjectRepository.save(ts);
	        }
	    }
	}


	@Transactional
	public Subject createSubject(CreateSubjectRequest request) throws Exception {

		Subject subject = new Subject();

		subject.setName(request.getName());
		subject.setGrade(request.getGrade());

		// Price: Double → BigDecimal
		if (request.getPrice() != null) {
			subject.setPrice(BigDecimal.valueOf(request.getPrice()));
		}

		subject.setStatus(request.getStatus() != null ? request.getStatus() : "active");
		int maxStudents = 30;
		try {
			maxStudents = Integer.parseInt(request.getMaxStudents());
			if (maxStudents <= 0)
				maxStudents = 30;
		} catch (Exception ignored) {
		}

		int sessionsPerWeek = 1;
		try {
			sessionsPerWeek = Integer.parseInt(request.getSessionsPerWeek());
			if (sessionsPerWeek <= 0)
				sessionsPerWeek = 1;
		} catch (Exception ignored) {
		}

		subject.setMaxStudents(maxStudents);
		subject.setSessionsPerWeek(sessionsPerWeek);

		subject.setNote(request.getNote());

		// ----------- LẤY FILE TRONG DTO -----------
		MultipartFile file = request.getImage();

		if (file != null && !file.isEmpty()) {

			String originalName = file.getOriginalFilename();
			String ext = "";

			if (originalName != null && originalName.contains(".")) {
				ext = originalName.substring(originalName.lastIndexOf("."));
			}

			String fileName = System.currentTimeMillis() + "-" + (int) (Math.random() * 1_000_000_000) + ext;

			Path uploadDir = Paths.get("uploads/subjects");

			if (!Files.exists(uploadDir)) {
				Files.createDirectories(uploadDir);
			}

			Path filePath = uploadDir.resolve(fileName);

			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

			subject.setImage("/uploads/subjects/" + fileName);
		}

		// --- LƯU SUBJECT ---
		subjectRepository.save(subject);

		// --- TẠO TEACHER-SUBJECT ---
		if (request.getTeacherId() != null) {

			Teacher teacher = teacherRepository.findById(request.getTeacherId())
					.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));

			TeacherSubject ts = new TeacherSubject();
			ts.setSubject(subject);
			ts.setTeacher(teacher);
			ts.setSalaryRate(BigDecimal.ZERO);

			teacherSubjectRepository.save(ts);
		}

		return subject;
	}
	
	public SubjectStatisticsDTO getSubjectStatistics () {
		
		long totalSubjects = subjectRepository.count();
		YearMonth currentMonth = YearMonth.now();
    	LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
    	LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
    	
    	long newSubjectThisMonth = subjectRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);
    	
    	double percentageIncreaseSubject = 0;
    	if ( totalSubjects > 0) {
    		percentageIncreaseSubject = ((double) newSubjectThisMonth / totalSubjects) * 100;	
    	}
    	percentageIncreaseSubject = Math.round(percentageIncreaseSubject * 100.0) / 100.0;
		return new SubjectStatisticsDTO(totalSubjects, newSubjectThisMonth, percentageIncreaseSubject);
	}

	// ------------------- MAPPING HELPER -------------------
	private SubjectDTO mapToDTO(Subject subject) {
		// Tạo đối tượng SubjectDTO mới
		SubjectDTO dto = new SubjectDTO();
		// Định dạng thời gian cho createdAt và updatedAt
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		// Ánh xạ các thuộc tính của Subject sang SubjectDTO
		dto.setId(subject.getId());
		dto.setName(subject.getName());
		dto.setGrade(subject.getGrade());
		dto.setPrice(subject.getPrice());
		dto.setStatus(subject.getStatus());
		dto.setMaxStudents(subject.getMaxStudents());
		dto.setSessionsPerWeek(subject.getSessionsPerWeek());
		dto.setImage(subject.getImage());
		dto.setNote(subject.getNote());

		// Ánh xạ trường createdAt và updatedAt sang String với định dạng
		dto.setCreatedAt(subject.getCreatedAt() != null ? subject.getCreatedAt().format(formatter) : null);
		dto.setUpdatedAt(subject.getUpdatedAt() != null ? subject.getUpdatedAt().format(formatter) : null);

		// Ánh xạ danh sách TeacherSubjects nếu có
		if (subject.getTeacherSubjects() != null) {
			List<TeacherSubjectDTO> tsDTOs = subject.getTeacherSubjects().stream().map(ts -> {
				TeacherSubjectDTO tsDTO = new TeacherSubjectDTO();
				tsDTO.setSalaryRate(ts.getSalaryRate());

				if (ts.getTeacher() != null) {
					TeacherDTO tDTO = new TeacherDTO();
					tDTO.setId(ts.getTeacher().getId());
					tDTO.setSpecialty(ts.getTeacher().getSpecialty());

					if (ts.getTeacher().getUserInfo() != null) {
						UserDTO uDTO = new UserDTO();
						uDTO.setId(ts.getTeacher().getUserInfo().getId());
						uDTO.setFullName(ts.getTeacher().getUserInfo().getFullName());
						uDTO.setEmail(ts.getTeacher().getUserInfo().getEmail());
						uDTO.setGender(ts.getTeacher().getUserInfo().getGender());
						uDTO.setPhoneNumber(ts.getTeacher().getUserInfo().getPhoneNumber());
						tDTO.setUser(uDTO);
					}

					tsDTO.setTeacher(tDTO);
				}

				return tsDTO;
			}).collect(Collectors.toList());

			dto.setTeacherSubjects(tsDTOs);
		}

		return dto;
	}

}
