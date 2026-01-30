package com.management.student_center.service;

import com.management.student_center.dto.*;
import com.management.student_center.dto.student.*;
import com.management.student_center.dto.student.StudentDTO;
import com.management.student_center.entity.*;
import com.management.student_center.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ParentContactRepository parentContactRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;
    private final StudentTuitionRepository studentTuitionRepository;
    private final StudentTuitionDetailRepository studentTuitionDetailRepository;
    // Mapping role
    private static final Map<String, String> roleMapping = Map.of(
        "R0", "Admin",
        "R1", "Giáo viên",
        "R2", "Học sinh"
    );

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          AddressRepository addressRepository,
                          ParentContactRepository parentContactRepository,
                          StudentSubjectRepository studentSubjectRepository,
                          PasswordEncoder passwordEncoder,
                          ImageService imageService,
                          StudentTuitionDetailRepository studentTuitionDetailRepository,
                          StudentTuitionRepository studentTuitionRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.parentContactRepository = parentContactRepository;
        this.studentSubjectRepository = studentSubjectRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageService = imageService;
        this.studentTuitionDetailRepository = studentTuitionDetailRepository;
        this.studentTuitionRepository = studentTuitionRepository;
    }
    
    public StudentGroupResponseDTO getAllStudentsGroupBySchool(Map<String, String> filters) {

        Specification<Student> spec = Specification
                .where(StudentSpecification.hasRole("R2"))
                .and(StudentSpecification.nameContains(filters.get("name")))
                .and(StudentSpecification.genderIs(parseGender(filters.get("gender"))))
                .and(StudentSpecification.gradeContains(filters.get("grade")))
                .and(StudentSpecification.schoolNameContains(filters.get("schoolName")));

        List<Student> students = studentRepository.findAll(spec);

        long totalStudents = students.size();

        List<StudentDTO> studentDTOs = students.stream()
                .map(this::mapToStudentDTO)
                .toList();

        // ===============================
        // GROUP THEO CẤP → TRƯỜNG
        // ===============================

        Map<String, Map<String, Long>> totalByLevelAndSchool = new HashMap<>();
        Map<String, Map<String, List<StudentDTO>>> groupResult = new HashMap<>();

        for (StudentDTO s : studentDTOs) {

            // Xác định cấp
            String grade = s.getGrade();
            String level;

            if (grade == null) {
                level = "Chưa xác định";
            } else if (List.of("6", "7", "8", "9").contains(grade)) {
                level = "Cấp 2";
            } else if (List.of("10", "11", "12").contains(grade)) {
                level = "Cấp 3";
            } else {
                level = "Khác";
            }

            String school =
                    (s.getSchoolName() != null && !s.getSchoolName().isBlank())
                            ? s.getSchoolName()
                            : "Chưa có trường";

            // ===== group danh sách =====
            groupResult
                    .computeIfAbsent(level, k -> new HashMap<>())
                    .computeIfAbsent(school, k -> new ArrayList<>())
                    .add(s);

            // ===== đếm số lượng =====
            totalByLevelAndSchool
                    .computeIfAbsent(level, k -> new HashMap<>())
                    .merge(school, 1L, Long::sum);
        }

        StudentGroupResponseDTO response = new StudentGroupResponseDTO();
        response.setTotalStudents(totalStudents);
        response.setStudentsBySchool(groupResult);
        response.setTotalStudentsBySchool(totalByLevelAndSchool);

        return response;
    }


    /**
     * getAllStudents (Có filter User, Student, Subject)
     */
    public PaginatedResponseDTO<StudentDTO> getAllStudents(int page, int limit, Map<String, String> filters) {
        Pageable pageable = PageRequest.of(page - 1, limit);

        Specification<Student> spec = Specification.where(StudentSpecification.hasRole("R2"))
                .and(StudentSpecification.nameContains(filters.get("name")))
                .and(StudentSpecification.genderIs(parseGender(filters.get("gender"))))
                .and(StudentSpecification.gradeContains(filters.get("grade")))
                .and(StudentSpecification.schoolNameContains(filters.get("schoolName")))
                .and(StudentSpecification.hasSubjectName(filters.get("subject")));

        Page<Student> studentPage = studentRepository.findAll(spec, pageable);

        List<StudentDTO> studentDTOs = studentPage.getContent().stream()
                .map(this::mapToStudentDTO)
                .collect(Collectors.toList());

        PaginationDTO pagination = new PaginationDTO(
                studentPage.getTotalElements(),
                page,
                limit,
                studentPage.getTotalPages()
        );

        return new PaginatedResponseDTO<>(studentDTOs, pagination);
    }
    
    /**
     * getStudentById
     */
    public StudentDTO getStudentById(Long userId) {
        Student student = studentRepository.findByUserInfoId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên!"));
        return mapToStudentDTO(student);
    }

    /**
     * createNewStudent
     */
    @Transactional
    public User createNewStudent(CreateStudentDTO dto, MultipartFile file) {
        // Validation cơ bản
        if (dto.getEmail() == null || dto.getFullName() == null || dto.getRoleId() == null) {
            throw new RuntimeException("Thiếu các thông tin bắt buộc.");
        }
        if (!"R2".equals(dto.getRoleId())) {
            throw new RuntimeException("RoleId phải là R2 (Học sinh).");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email này đã tồn tại.");
        }

        // 1. Tạo User
        String hashedPassword = passwordEncoder.encode(dto.getPassword() != null ? dto.getPassword() : "123456");
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

        // 2. Tạo Address
        Address savedAddress = null;
        if (dto.getAddress() != null) {
            Address newAddress = new Address();
            newAddress.setDetails(dto.getAddress().getDetails());
            newAddress.setWard(dto.getAddress().getWard());
            newAddress.setProvince(dto.getAddress().getProvince());
            savedAddress = addressRepository.save(newAddress);
        }

        // 3. Tạo Student
        Student newStudent = new Student();
        newStudent.setUserInfo(savedUser);
        newStudent.setAddressInfo(savedAddress);
        newStudent.setDateOfBirth(dto.getDateOfBirth());
        newStudent.setGrade(dto.getGrade());
        newStudent.setSchoolName(dto.getSchoolName());
        Student savedStudent = studentRepository.save(newStudent);

        // 4. Tạo ParentContacts
        if (dto.getParents() != null && !dto.getParents().isEmpty()) {
            for (ParentContactDTO pDto : dto.getParents()) {
                ParentContact pc = new ParentContact();
                pc.setStudent(savedStudent);
                pc.setFullName(pDto.getFullName());
                pc.setPhoneNumber(pDto.getPhoneNumber());
                pc.setRelationship(pDto.getRelationship() != null ? pDto.getRelationship() : "Phụ huynh");
                parentContactRepository.save(pc);
            }
        }
        
        return savedUser;
    }

    /**
     * updateStudent
     */
    @Transactional
    public void updateStudent(Long userId, CreateStudentDTO dto, MultipartFile file) {
        Student student = studentRepository.findByUserInfoId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên!"));
        User user = student.getUserInfo();
        Address address = student.getAddressInfo();

        // 1. Update User
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setGender(dto.getGender());
        // Không update email ở đây để an toàn, hoặc tùy logic
        
        if (file != null && !file.isEmpty()) {
            imageService.deleteImage(user.getImage());
            user.setImage(imageService.saveImage(file));
        }
        userRepository.save(user);

        // 2. Update Student info
        student.setGrade(dto.getGrade());
        student.setSchoolName(dto.getSchoolName());
        student.setDateOfBirth(dto.getDateOfBirth());
        
        // 3. Update Address
        if (dto.getAddress() != null) {
            if (address == null) {
                address = new Address();
                student.setAddressInfo(address); // Link lại nếu mới tạo
            }
            address.setDetails(dto.getAddress().getDetails());
            address.setWard(dto.getAddress().getWard());
            address.setProvince(dto.getAddress().getProvince());
            addressRepository.save(address);
        }
        studentRepository.save(student);

        // 4. Update ParentContacts (Xóa cũ, thêm mới cho đơn giản)
        parentContactRepository.deleteByStudentId(student.getId());
        if (dto.getParents() != null) {
            for (ParentContactDTO pDto : dto.getParents()) {
                ParentContact pc = new ParentContact();
                pc.setStudent(student);
                pc.setFullName(pDto.getFullName());
                pc.setPhoneNumber(pDto.getPhoneNumber());
                pc.setRelationship(pDto.getRelationship());
                parentContactRepository.save(pc);
            }
        }
    }

    /**
     * deleteStudent
     */
    @Transactional
    public void deleteStudent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Optional<Student> studentOpt = studentRepository.findByUserInfoId(userId);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();

            // --- BỔ SUNG: Kiểm tra học phí ---
            long unpaidCount = studentTuitionDetailRepository.countUnpaidByStudent(student.getId());
            if (unpaidCount > 0) {
                throw new IllegalStateException(
                    "Không thể xóa học sinh vì còn nợ học phí"
                );
            }

            // Xóa ParentContacts
            parentContactRepository.deleteByStudentId(student.getId());

            // Xóa StudentSubjects
            studentSubjectRepository.deleteByStudentId(student.getId());

            // Xóa Address
            if (student.getAddressInfo() != null) {
                addressRepository.delete(student.getAddressInfo());
            }

            // Xóa Student
            studentRepository.delete(student);
        }
        // Xóa ảnh
        if (user.getImage() != null) {
            imageService.deleteImage(user.getImage());
        }

        // Xóa User
        userRepository.delete(user);
    }


    /**
     * deleteMultipleStudents
     */
    @Transactional
    public void deleteMultipleStudents(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) throw new RuntimeException("Danh sách ID trống!");
        for (Long id : userIds) {
            
                deleteStudent(id);
           
        }
    }

    /**
     * exportStudentsToExcel
     */
    public byte[] exportStudentsToExcel(Map<String, String> filters) throws IOException {
        // Lấy list (không phân trang)
    	Boolean gender = null;
    	if (filters.get("gender") != null && !filters.get("gender").isBlank()) {
    	    gender = parseGender(filters.get("gender"));
    	}
        Specification<Student> spec = Specification.where(StudentSpecification.hasRole("R2"))
                .and(StudentSpecification.nameContains(filters.get("name")))
                .and(StudentSpecification.genderIs(gender))
                .and(StudentSpecification.gradeContains(filters.get("grade")))
                .and(StudentSpecification.schoolNameContains(filters.get("schoolName")));
        
        List<Student> students = studentRepository.findAll(spec);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh sách học viên");

        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {
        	    "STT",
        	    "Họ và tên",
        	    "Email",
        	    "Giới tính",
        	    "Ngày sinh",
        	    "SĐT",
        	    "Khối",
        	    "Trường",
        	    "Địa chỉ",
        	    "Phụ huynh",
        	    "SĐT phụ huynh"
        	};
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            User u = s.getUserInfo();
            if (u == null) continue; 
            Address a = s.getAddressInfo();
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(u.getFullName());
            row.createCell(2).setCellValue(u.getEmail());
            row.createCell(3).setCellValue(
            	    Boolean.TRUE.equals(u.getGender()) ? "Nam" : "Nữ"
            	);
            if (s.getDateOfBirth() != null) {
                row.createCell(4).setCellValue(s.getDateOfBirth().toString());
            } else {
                row.createCell(4).setCellValue("");
            }
            row.createCell(5).setCellValue(u.getPhoneNumber());
            row.createCell(6).setCellValue(s.getGrade());
            row.createCell(7).setCellValue(s.getSchoolName());

            String addressStr = "";
            if (a != null) {
                 addressStr = String.join(", ", 
                    Optional.ofNullable(a.getDetails()).orElse(""),
                    Optional.ofNullable(a.getWard()).orElse(""),
                    Optional.ofNullable(a.getProvince()).orElse("")
                ).replaceAll("^, |^, |, $", ""); // Clean string
            }
            row.createCell(8).setCellValue(addressStr);

            String parentNameStr = "";
            String parentPhoneStr = "";

            if (s.getParentContacts() != null && !s.getParentContacts().isEmpty()) {
                parentNameStr = s.getParentContacts().stream()
                    .map(ParentContact::getFullName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

                parentPhoneStr = s.getParentContacts().stream()
                    .map(ParentContact::getPhoneNumber)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            }

            row.createCell(9).setCellValue(parentNameStr);
            row.createCell(10).setCellValue(parentPhoneStr);

        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // Helper Methods
    private StudentDTO mapToStudentDTO(Student student) {
        User user = student.getUserInfo();
        Address address = student.getAddressInfo();

        AddressDTO addressDTO = new AddressDTO();
        if (address != null) {
            addressDTO.setId(address.getId());
            addressDTO.setDetails(address.getDetails());
            addressDTO.setWard(address.getWard());
            addressDTO.setProvince(address.getProvince());
        }
        List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(student.getId());
        
        List<StudentDTO.SubjectInfoDTO> subjectDTOs = studentSubjects.stream()
                .map(ss -> new StudentDTO.SubjectInfoDTO(
                        ss.getSubject().getId(), 
                        ss.getSubject().getName()
                ))
                .collect(Collectors.toList());
        
        List<ParentContactDTO> parentDTOs = student.getParentContacts().stream()
                .map(p -> {
                    ParentContactDTO dto = new ParentContactDTO();
                    dto.setId(p.getId());
                    dto.setFullName(p.getFullName());
                    dto.setPhoneNumber(p.getPhoneNumber());
                    dto.setRelationship(p.getRelationship());
                    return dto;
                }).collect(Collectors.toList());

        StudentDTO dto = new StudentDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setGender(user.getGender());
        dto.setImage(user.getImage());
        dto.setRoleId(user.getRoleId());
        dto.setRoleName(roleMapping.getOrDefault(user.getRoleId(), ""));
        dto.setDateOfBirth(student.getDateOfBirth());
        dto.setGrade(student.getGrade());
        dto.setSchoolName(student.getSchoolName());
        dto.setAddress(addressDTO);
        dto.setParents(parentDTOs);
        dto.setSubjects(subjectDTOs);
        // Set createdAt và updatedAt
        dto.setCreatedAt(student.getCreatedAt());  // Set createdAt
        dto.setUpdatedAt(student.getUpdatedAt());  // Set updatedAt

        return dto;
    }


    private Boolean parseGender(String genderStr) {
        if (genderStr == null || genderStr.isEmpty()) return null;
        return "true".equalsIgnoreCase(genderStr) || "1".equals(genderStr);
    }
    
    public StudentStatisticDTO getStudentStatistics() {
    	
    	long totalStudents = studentRepository.count();
    	
    	YearMonth currentMonth = YearMonth.now();
    	LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
    	LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
    	
    	long newStudentsThisMonth = studentRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);
    	
    	double percentageIncrease = 0;
    	if (totalStudents >0) {
    		percentageIncrease = ((double) newStudentsThisMonth / totalStudents) * 100;
    	}
    	
    	percentageIncrease = Math.round(percentageIncrease * 100.0) / 100.0;
    	
    	return new StudentStatisticDTO(totalStudents, newStudentsThisMonth, percentageIncrease);
    }
}