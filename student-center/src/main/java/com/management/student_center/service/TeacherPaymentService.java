package com.management.student_center.service;

import com.management.student_center.dto.payment.SalaryCalculationDTO;
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
public class TeacherPaymentService {

    private final TeacherRepository teacherRepository;
    private final TeacherPaymentRepository teacherPaymentRepository;
    private final TeacherPaymentDetailRepository teacherPaymentDetailRepository;
    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;

    public TeacherPaymentService(TeacherRepository teacherRepository,
                                 TeacherPaymentRepository teacherPaymentRepository,
                                 TeacherPaymentDetailRepository teacherPaymentDetailRepository,
                                 SessionRepository sessionRepository,
                                 SubjectRepository subjectRepository) {
        this.teacherRepository = teacherRepository;
        this.teacherPaymentRepository = teacherPaymentRepository;
        this.teacherPaymentDetailRepository = teacherPaymentDetailRepository;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
    }

    /**
     * 🧩 Tính lương giáo viên trong 1 tháng (Logic nội bộ)
     */
    public List<SalaryCalculationDTO> calculateTeacherSalaryByMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // Tối ưu: Nên viết Query trong Repo để chỉ lấy Teacher R1 và Active
        // List<Teacher> teachers = teacherRepository.findActiveTeachersByRole("R1");
        List<Teacher> teachers = teacherRepository.findAll(); 

        List<SalaryCalculationDTO> results = new ArrayList<>();

        for (Teacher teacher : teachers) {
            // Validate user info & role
            if (teacher.getUserInfo() == null || !"R1".equals(teacher.getUserInfo().getRoleId())) {
                continue;
            }

            SalaryCalculationDTO dto = new SalaryCalculationDTO();
            dto.setTeacherId(teacher.getId());
            dto.setFullName(teacher.getUserInfo().getFullName());
            dto.setEmail(teacher.getUserInfo().getEmail());
            dto.setPhoneNumber(teacher.getUserInfo().getPhoneNumber());
            
            // Dùng BigDecimal để cộng dồn tổng tiền
            BigDecimal totalAmountBD = BigDecimal.ZERO; 

            for (TeacherSubject ts : teacher.getTeacherSubjects()) {
                // Lấy các session trong tháng
                List<Session> sessions = sessionRepository.findValidSessionsForSalary(
                        teacher.getId(),
                        ts.getSubject().getId(),
                        startOfMonth,
                        endOfMonth
                );

                if (sessions.isEmpty()) continue;

                // Tính tổng giờ (dùng double cho thời gian là ok)
                double totalHours = 0.0;
                for (Session s : sessions) {
                    Duration duration = Duration.between(s.getStartTime(), s.getEndTime());
                    totalHours += (double) duration.toMinutes() / 60.0;
                }

                // --- LOGIC TÍNH TIỀN QUAN TRỌNG ---
                // 1. Convert giờ sang BigDecimal
                BigDecimal hoursBD = BigDecimal.valueOf(totalHours);
                
                // 2. Lấy mức lương (Giả sử Entity TeacherSubject đã để là BigDecimal)
                BigDecimal salaryRateBD = ts.getSalaryRate(); 
                
                // 3. Nhân tiền: Giờ * Lương
                // setScale(0, RoundingMode.HALF_UP): Làm tròn về số nguyên (VD: 500.5 -> 501)
                BigDecimal totalMoneyBD = hoursBD.multiply(salaryRateBD).setScale(0, RoundingMode.HALF_UP);

                SalaryCalculationDTO.SubjectSalaryDTO subjDTO = new SalaryCalculationDTO.SubjectSalaryDTO();
                subjDTO.setSubjectId(ts.getSubject().getId());
                subjDTO.setSubjectName(ts.getSubject().getName());
                
                // Set các giá trị đã tính toán
                subjDTO.setSalaryRate(salaryRateBD);
                subjDTO.setTotalSessions(sessions.size());
                subjDTO.setTotalHours((float) totalHours);
                subjDTO.setTotalMoney(totalMoneyBD);

                dto.getSubjects().add(subjDTO);
                
                // Cộng vào tổng lương của giáo viên
                totalAmountBD = totalAmountBD.add(totalMoneyBD);
            }

            // Chỉ thêm vào danh sách nếu có môn dạy
            if (!dto.getSubjects().isEmpty()) {
                dto.setTotalAmount(totalAmountBD);
                results.add(dto);
            }
        }
        return results;
    }

    /**
     * 🧩 Tạo bảng lương (Lưu vào DB)
     */
    @Transactional
    public List<TeacherPayment> createTeacherPayments(int month, int year, String notes) {
        String noteIdentifier = String.format("Lương tháng %d/%d", month, year);

        // Check trùng
        List<TeacherPayment> existing = teacherPaymentRepository.findByNotesContaining(noteIdentifier);
        if (!existing.isEmpty()) {
            throw new RuntimeException("Bảng lương cho tháng " + month + "/" + year + " đã được tạo trước đó.");
        }

        List<SalaryCalculationDTO> salaries = calculateTeacherSalaryByMonth(month, year);
        List<TeacherPayment> savedPayments = new ArrayList<>();

        for (SalaryCalculationDTO sal : salaries) {
            TeacherPayment payment = new TeacherPayment();
            // Load Teacher reference (dùng getReferenceById để đỡ tốn query nếu ko cần check null)
            Teacher teacherRef = teacherRepository.findById(sal.getTeacherId()).orElse(null);
            
            payment.setTeacher(teacherRef);
            payment.setAmount(sal.getTotalAmount());
            payment.setPaymentDate(LocalDate.now());
            payment.setStatus("unpaid");
            payment.setNotes(noteIdentifier + ". " + (notes != null ? notes : ""));
            
            // Tạo list details
            List<TeacherPaymentDetail> details = new ArrayList<>();
            for (SalaryCalculationDTO.SubjectSalaryDTO sub : sal.getSubjects()) {
                TeacherPaymentDetail detail = new TeacherPaymentDetail();
                
                // Set quan hệ 2 chiều để Cascade hoạt động
                detail.setPayment(payment); 
                detail.setSubject(subjectRepository.findById(sub.getSubjectId()).orElse(null));
                
                detail.setTotalHours(sub.getTotalHours());
                detail.setTotalSessions(sub.getTotalSessions());
                detail.setSalaryRate(sub.getSalaryRate()); // BigDecimal
                detail.setTotalMoney(sub.getTotalMoney()); // BigDecimal
                
                details.add(detail);
            }
            
            // Gán details vào payment
            payment.setPaymentDetails(details);

            // Chỉ cần save Payment, Hibernate sẽ tự save Details nhờ CascadeType.ALL
            savedPayments.add(teacherPaymentRepository.save(payment));
        }
        return savedPayments;
    }

    /**
     * 🧩 Thanh toán lương
     */
    @Transactional
    public TeacherPayment payTeacherSalary(Long teacherId, int month, int year) {
        String noteIdentifier = String.format("Lương tháng %d/%d", month, year);
        
        TeacherPayment payment = teacherPaymentRepository.findByTeacherIdAndNotesContaining(teacherId, noteIdentifier)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng lương!"));

        if ("paid".equals(payment.getStatus())) {
            throw new RuntimeException("Bảng lương đã được thanh toán!");
        }

        payment.setStatus("paid");
        payment.setPaymentDate(LocalDate.now());
        return teacherPaymentRepository.save(payment);
    }

    // Các hàm GET giữ nguyên
    public TeacherPayment getTeacherSalaryDetail(Long teacherId, int month, int year) {
        String noteIdentifier = String.format("Lương tháng %d/%d", month, year);
        return teacherPaymentRepository.findByTeacherIdAndNotesContaining(teacherId, noteIdentifier)
                .orElse(null);
    }
    
    public List<TeacherPayment> getPaymentsByMonth(int month, int year) {
        String noteIdentifier = String.format("Lương tháng %d/%d", month, year);
        return teacherPaymentRepository.findByNotesContaining(noteIdentifier);
    }
}