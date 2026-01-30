package com.management.student_center.service;

import com.management.student_center.dto.TeacherBasicDTO;
import com.management.student_center.entity.Teacher;
import com.management.student_center.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.management.student_center.dto.AddressDTO;
import com.management.student_center.dto.PaginationDTO; // Đảm bảo import đúng
import com.management.student_center.dto.PaginatedResponseDTO; // Đảm bảo import đúng
import com.management.student_center.dto.teacher.*;

import com.management.student_center.entity.*;
import com.management.student_center.repository.*;

//Import Spring
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

//Import Java utils
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;

//Import Apache POI (Excel)
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class TeacherService {

	private final TeacherRepository teacherRepository;
	private final UserRepository userRepository;
	private final AddressRepository addressRepository;
	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder;
	private final ImageService imageService;
	private static final Map<String, String> roleMapping = Map.of("R0", "Admin", "R1", "Giáo viên", "R2", "Học sinh");
	private final TeacherPaymentRepository teacherPaymentRepository;

	public TeacherService(TeacherRepository teacherRepository, UserRepository userRepository,
			AddressRepository addressRepository, StudentRepository studentRepository, PasswordEncoder passwordEncoder,
			ImageService imageService, TeacherPaymentRepository teacherPaymentRepository) {
		this.teacherRepository = teacherRepository;
		this.userRepository = userRepository;
		this.addressRepository = addressRepository;
		this.studentRepository = studentRepository;
		this.passwordEncoder = passwordEncoder;
		this.imageService = imageService;
		this.teacherPaymentRepository = teacherPaymentRepository;
	}

	public PaginatedResponseDTO<TeacherDTO> getAllTeachers(int page, int limit, Map<String, String> filters) {
		Pageable pageable = PageRequest.of(page - 1, limit);

		// Xây dựng Specification (filter động)
		Specification<Teacher> spec = Specification.where(TeacherSpecification.hasRole("R1"))
				.and(TeacherSpecification.nameContains(filters.get("name")))
				.and(TeacherSpecification.genderIs(parseGender(filters.get("gender"))))
				.and(TeacherSpecification.specialtyContains(filters.get("specialty")));

		Page<Teacher> teacherPage = teacherRepository.findAll(spec, pageable);

		List<TeacherDTO> teacherDTOs = teacherPage.getContent().stream().map(this::mapToTeacherDTO)
				.collect(Collectors.toList());

		PaginationDTO pagination = new PaginationDTO(teacherPage.getTotalElements(), page, limit,
				teacherPage.getTotalPages());

		return new PaginatedResponseDTO<>(teacherDTOs, pagination);
	}

	public List<TeacherBasicDTO> getTeacherBasicList() {
		return teacherRepository.findAll().stream()
				.filter(t -> t.getUserInfo() != null && "R1".equals(t.getUserInfo().getRoleId()))
				.map(t -> new TeacherBasicDTO(t.getId(), t.getUserInfo().getId(), t.getUserInfo().getFullName(),
						t.getUserInfo().getEmail(), t.getUserInfo().getPhoneNumber(), t.getUserInfo().getGender(),
						t.getSpecialty()))
				.collect(Collectors.toList());
	}

	@Transactional
	public User createNewEmployee(CreateEmployeeDTO dto, MultipartFile file) {
		if (dto.getEmail() == null || dto.getPassword() == null || dto.getFullName() == null
				|| dto.getRoleId() == null) {
			throw new RuntimeException("Thiếu các thông tin bắt buộc.");
		}
		if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
			throw new RuntimeException("Email này đã tồn tại trong hệ thống.");
		}

		String hashedPassword = passwordEncoder.encode(dto.getPassword());
		String imagePath = (file != null && !file.isEmpty()) ? imageService.saveImage(file) : null;

		User newUser = new User();
		newUser.setEmail(dto.getEmail());
		newUser.setPassword(hashedPassword);
		newUser.setFullName(dto.getFullName());
		newUser.setPhoneNumber(dto.getPhoneNumber());
		newUser.setGender(dto.getGender());
		newUser.setImage(imagePath);
		newUser.setRoleId(dto.getRoleId());
		User savedUser = userRepository.save(newUser);

		if ("R1".equals(dto.getRoleId())) {
			Address savedAddress = null;
			if (dto.getAddress() != null) {
				Address newAddress = new Address();
				newAddress.setDetails(dto.getAddress().getDetails());
				newAddress.setWard(dto.getAddress().getWard());
				newAddress.setProvince(dto.getAddress().getProvince());
				savedAddress = addressRepository.save(newAddress);
			}

			Teacher newTeacher = new Teacher();
			newTeacher.setUserInfo(savedUser);
			newTeacher.setAddressInfo(savedAddress);
			newTeacher.setDateOfBirth(dto.getDateOfBirth());
			newTeacher.setSpecialty(dto.getSpecialty());
			teacherRepository.save(newTeacher);
		}

		return savedUser;
	}

	/**
	 * Tương đương: updateEmployeeData
	 */
	@Transactional
	public Teacher updateEmployeeData(Long userId, UpdateEmployeeDTO dto, MultipartFile file) {
		Teacher teacher = teacherRepository.findByUserInfoId(userId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên!"));

		User user = teacher.getUserInfo();
		Address address = teacher.getAddressInfo();

		user.setFullName(dto.getFullName());
		user.setPhoneNumber(dto.getPhoneNumber());
		user.setGender(dto.getGender());

		if (file != null && !file.isEmpty()) {
			imageService.deleteImage(user.getImage());
			user.setImage(imageService.saveImage(file));
		}
		userRepository.save(user);

		teacher.setDateOfBirth(dto.getDateOfBirth());
		teacher.setSpecialty(dto.getSpecialty());

		if (dto.getAddress() != null) {
			if (address == null) {
				address = new Address();
			}
			address.setDetails(dto.getAddress().getDetails());
			address.setWard(dto.getAddress().getWard());
			address.setProvince(dto.getAddress().getProvince());
			Address savedAddress = addressRepository.save(address);
			teacher.setAddressInfo(savedAddress);
		}

		return teacherRepository.save(teacher);
	}

	// TeacherService.java

	@Transactional
	public void deleteEmployee(Long userId) {
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

	    Optional<Teacher> teacherOpt = teacherRepository.findByUserInfoId(userId);
	    if (teacherOpt.isPresent()) {
	        Teacher teacher = teacherOpt.get();

	        // Kiểm tra lương chưa thanh toán bằng repository
	        long unpaidCount = teacherPaymentRepository.countUnpaidByTeacher(teacher.getId());
	        if (unpaidCount > 0) {
	            throw new RuntimeException(
	                "Không thể xóa giáo viên này vì vẫn còn lương chưa thanh toán.");
	        }

	        if (teacher.getAddressInfo() != null) {
	            addressRepository.delete(teacher.getAddressInfo());
	        }
	        teacherRepository.delete(teacher);
	    }

	    if (user.getImage() != null) {
	        imageService.deleteImage(user.getImage());
	    }
	    userRepository.delete(user);
	}


	/**
	 * Tương đương: deleteMultipleTeachers
	 */
	@Transactional
	public void deleteMultipleTeachers(List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			throw new RuntimeException("Danh sách ID không hợp lệ!");
		}
		for (Long id : userIds) {
			this.deleteEmployee(id); // Gọi lại hàm xóa 1
		}
	}

	/**
	 * Tương đương: exportTeachersToExcel
	 */
	public byte[] exportTeachersToExcel(Map<String, String> filters) throws IOException {
		Specification<Teacher> spec = Specification.where(TeacherSpecification.hasRole("R1"))
				.and(TeacherSpecification.nameContains(filters.get("name")))
				.and(TeacherSpecification.genderIs(parseGender(filters.get("gender"))))
				.and(TeacherSpecification.specialtyContains(filters.get("specialty")));

		List<Teacher> teachers = teacherRepository.findAll(spec);

		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Danh sách giáo viên");

		Row headerRow = sheet.createRow(0);
		String[] headers = { "ID", "Họ tên", "Email", "Số điện thoại", "Giới tính", "Ngày sinh", "Chuyên môn",
				"Địa chỉ" };
		for (int i = 0; i < headers.length; i++) {
			headerRow.createCell(i).setCellValue(headers[i]);
		}

		int rowNum = 1;
		for (Teacher t : teachers) {
			Row row = sheet.createRow(rowNum++);
			User u = t.getUserInfo();
			Address a = t.getAddressInfo();

			row.createCell(0).setCellValue(u.getId());
			row.createCell(1).setCellValue(u.getFullName());
			row.createCell(2).setCellValue(u.getEmail());
			row.createCell(3).setCellValue(u.getPhoneNumber() != null ? u.getPhoneNumber() : "");
			row.createCell(4).setCellValue(u.getGender() ? "Nam" : "Nữ");
			row.createCell(5).setCellValue(t.getDateOfBirth() != null ? t.getDateOfBirth().toString() : "");
			row.createCell(6).setCellValue(t.getSpecialty() != null ? t.getSpecialty() : "");

			String addressStr = "";
			if (a != null) {
				addressStr = List
						.of(Optional.ofNullable(a.getDetails()).orElse(""), Optional.ofNullable(a.getWard()).orElse(""),
								Optional.ofNullable(a.getProvince()).orElse(""))
						.stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(", "));
			}
			row.createCell(7).setCellValue(addressStr);
		}

		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();
		return out.toByteArray();
	}

	// ----- CÁC HÀM TIỆN ÍCH (HELPER) -----

	private TeacherDTO mapToTeacherDTO(Teacher teacher) {
		User user = teacher.getUserInfo();
		Address address = teacher.getAddressInfo();

		AddressDTO addressDTO = new AddressDTO();
		if (address != null) {
			addressDTO.setId(address.getId());
			addressDTO.setDetails(address.getDetails());
			addressDTO.setWard(address.getWard());
			addressDTO.setProvince(address.getProvince());
		}

		TeacherDTO dto = new TeacherDTO();
		dto.setId(user.getId());
		dto.setEmail(user.getEmail());
		dto.setFullName(user.getFullName());
		dto.setPhoneNumber(user.getPhoneNumber());
		dto.setGender(user.getGender());
		dto.setImage(user.getImage());
		dto.setRoleId(user.getRoleId());
		dto.setRoleName(roleMapping.getOrDefault(user.getRoleId(), ""));
		dto.setDateOfBirth(teacher.getDateOfBirth());
		dto.setSpecialty(teacher.getSpecialty());
		dto.setAddress(addressDTO);

		return dto;
	}

	private Boolean parseGender(String genderStr) {
		if (genderStr == null || genderStr.isEmpty()) {
			return null;
		}
		return "true".equalsIgnoreCase(genderStr) || "1".equals(genderStr);
	}
	
	 public TeacherStatisticsDTO getTeacherStatistics() {
	        long totalTeachers = teacherRepository.count();

	        YearMonth currentMonth = YearMonth.now();
	        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
	        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

	        long newTeachersThisMonth = teacherRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

	        double percentageIncreaseTeacher = 0;
	        if (totalTeachers > 0) {
	            percentageIncreaseTeacher = ((double) newTeachersThisMonth / totalTeachers) * 100;
	        }
	        percentageIncreaseTeacher = Math.round(percentageIncreaseTeacher * 100.0) / 100.0;
	        return new TeacherStatisticsDTO(totalTeachers, newTeachersThisMonth, percentageIncreaseTeacher);
	    }
}
