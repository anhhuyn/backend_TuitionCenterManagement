package com.management.student_center.service;

import com.management.student_center.dto.tuition.TuitionCalculationDTO;
import com.management.student_center.entity.*;
import com.management.student_center.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentTuitionService {

    private final StudentRepository studentRepository;
    private final AttendanceStudentRepository attendanceRepository;
    private final StudentTuitionRepository tuitionRepository;
    private final StudentTuitionDetailRepository detailRepository; // Không bắt buộc nếu dùng Cascade

    public StudentTuitionService(StudentRepository studentRepository,
                                 AttendanceStudentRepository attendanceRepository,
                                 StudentTuitionRepository tuitionRepository,
                                 StudentTuitionDetailRepository detailRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.tuitionRepository = tuitionRepository;
        this.detailRepository = detailRepository;
    }

    /**
     * PHẦN 1: Tính toán logic (Trả về DTO để xem trước hoặc dùng để lưu)
     */
    public List<TuitionCalculationDTO> calculateTuitionByMonth(int month, int year) {
        // 1. Xác định ngày đầu tháng và cuối tháng
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // 2. Lấy tất cả học sinh (Nên lọc học sinh Active nếu có trường status)
        List<Student> students = studentRepository.findAll();
        List<TuitionCalculationDTO> results = new ArrayList<>();

        for (Student student : students) {
            // Có thể check Role nếu student entity chưa lọc
            if (student.getUserInfo() == null) continue;

            TuitionCalculationDTO dto = new TuitionCalculationDTO();
            dto.setStudentId(student.getId());
            dto.setFullName(student.getUserInfo().getFullName());
            dto.setPhoneNumber(student.getUserInfo().getPhoneNumber());

            BigDecimal totalAmountBD = BigDecimal.ZERO; // Tổng tiền hóa đơn

            // 3. Duyệt từng môn học sinh này đã đăng ký
            // Lưu ý: student.getStudentSubjects() lấy ra danh sách StudentSubject
            if (student.getStudentSubjects() != null) {
                for (StudentSubject ss : student.getStudentSubjects()) {
                    Subject subject = ss.getSubject();

                    // 4. Lấy danh sách các buổi học hợp lệ (đã completed + có mặt)
                    List<AttendanceStudent> attendances = attendanceRepository.findValidAttendanceForTuition(
                            student.getId(),
                            subject.getId(),
                            startOfMonth,
                            endOfMonth
                    );

                    // Nếu không đi học buổi nào môn này thì bỏ qua
                    if (attendances.isEmpty()) continue;

                    // 5. Tính tổng giờ học
                    double totalHours = 0.0;
                    for (AttendanceStudent ats : attendances) {
                        Session s = ats.getSession();
                        if (s.getStartTime() != null && s.getEndTime() != null) {
                            Duration duration = Duration.between(s.getStartTime(), s.getEndTime());
                            totalHours += (double) duration.toMinutes() / 60.0;
                        }
                    }

                    // 6. Tính tiền: Giờ * Giá (Subject Price)
                    BigDecimal hoursBD = BigDecimal.valueOf(totalHours);
                    BigDecimal priceBD = subject.getPrice(); // Giá 1 giờ

                    // Nếu giá chưa set, mặc định là 0
                    if (priceBD == null) priceBD = BigDecimal.ZERO;

                    // Nhân tiền và làm tròn
                    BigDecimal moneyBD = hoursBD.multiply(priceBD).setScale(0, RoundingMode.HALF_UP);

                    // 7. Map vào DTO con
                    TuitionCalculationDTO.SubjectTuitionDTO subjDTO = new TuitionCalculationDTO.SubjectTuitionDTO();
                    subjDTO.setSubjectId(subject.getId());
                    subjDTO.setSubjectName(subject.getName());
                    subjDTO.setHourlyRate(priceBD);
                    subjDTO.setTotalSessions(attendances.size());
                    subjDTO.setTotalHours((float) totalHours);
                    subjDTO.setTotalMoney(moneyBD);

                    dto.getSubjects().add(subjDTO);
                    totalAmountBD = totalAmountBD.add(moneyBD);
                }
            }

            // Chỉ thêm vào kết quả nếu học sinh có phát sinh học phí > 0
            if (!dto.getSubjects().isEmpty()) {
                dto.setTotalAmount(totalAmountBD);
                results.add(dto);
            }
        }
        return results;
    }

    /**
     * PHẦN 2: Lưu vào Database (Create)
     */
    @Transactional
    public List<StudentTuition> createTuitions(int month, int year, String notes) {
        String noteIdentifier = String.format("Học phí tháng %d/%d", month, year);

        // 1. Tính toán lại dữ liệu
        List<TuitionCalculationDTO> calculations = calculateTuitionByMonth(month, year);
        List<StudentTuition> savedList = new ArrayList<>();

        for (TuitionCalculationDTO calc : calculations) {
            // 2. Kiểm tra xem học sinh này đã có hóa đơn tháng này chưa
            boolean exists = tuitionRepository.existsByStudentIdAndMonthAndYear(calc.getStudentId(), month, year);
            if (exists) {
                // Nếu muốn bỏ qua không tạo lại: continue;
                // Nếu muốn báo lỗi: throw new RuntimeException(...)
                continue; // Ở đây mình chọn bỏ qua
            }

            // 3. Tạo Header Hóa đơn
            StudentTuition tuition = new StudentTuition();
            Student studentRef = studentRepository.findById(calc.getStudentId()).orElse(null);

            tuition.setStudent(studentRef);
            tuition.setMonth(month);
            tuition.setYear(year);
            tuition.setTotalAmount(calc.getTotalAmount());
            tuition.setStatus("unpaid");
            tuition.setNotes(noteIdentifier + (notes != null ? ". " + notes : ""));
            // createdAt được set tự động bởi @PrePersist trong Entity

            // 4. Tạo Details (Chi tiết từng môn)
            List<StudentTuitionDetail> details = new ArrayList<>();
            for (TuitionCalculationDTO.SubjectTuitionDTO subDTO : calc.getSubjects()) {
                StudentTuitionDetail detail = new StudentTuitionDetail();
                
                // Set quan hệ 2 chiều
                detail.setStudentTuition(tuition);
                
                // Set Subject (Giả lập object để không phải query lại DB)
                Subject subject = new Subject();
                subject.setId(subDTO.getSubjectId());
                detail.setSubject(subject);

                detail.setAttendedSessions(subDTO.getTotalSessions());
                detail.setTotalHours(subDTO.getTotalHours());
                detail.setHourlyRate(subDTO.getHourlyRate());
                detail.setTotalMoney(subDTO.getTotalMoney());

                details.add(detail);
            }

            // Gán list details vào tuition để Cascade.ALL hoạt động
            tuition.setDetails(details);

            // 5. Save (Lưu 1 lần là được cả Header và Details)
            savedList.add(tuitionRepository.save(tuition));
        }
        
        return savedList;
    }
    
    public List<StudentTuition> getTuitionsByMonth(int month, int year) {
        // Cần thêm hàm này trong StudentTuitionRepository:
        // List<StudentTuition> findByMonthAndYear(int month, int year);
        return tuitionRepository.findByMonthAndYear(month, year);
    }

    /**
     * 4. Lấy chi tiết hóa đơn 1 học sinh
     */
    public StudentTuition getTuitionDetail(Long studentId, int month, int year) {
        // Cần thêm hàm này trong StudentTuitionRepository:
        // Optional<StudentTuition> findByStudentIdAndMonthAndYear(Long studentId, int month, int year);
        return tuitionRepository.findByStudentIdAndMonthAndYear(studentId, month, year)
                .orElse(null);
    }

    /**
     * 5. Thanh toán học phí
     */
    @Transactional
    public StudentTuition payTuition(Long studentId, int month, int year) {
        StudentTuition tuition = tuitionRepository.findByStudentIdAndMonthAndYear(studentId, month, year)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn học phí!"));

        if ("paid".equals(tuition.getStatus())) {
            throw new RuntimeException("Hóa đơn này đã được thanh toán rồi!");
        }

        tuition.setStatus("paid");
        // Nếu muốn lưu ngày thanh toán thực tế, thêm trường paidDate vào Entity và set ở đây
        // tuition.setPaidDate(LocalDate.now());
        
        return tuitionRepository.save(tuition);
    }
}