package com.management.student_center.service;

import com.management.student_center.dto.StudentAssignmentResponseDTO;
import com.management.student_center.dto.StudentDTO;
import com.management.student_center.dto.UpdateStudentAssignmentDTO;
import com.management.student_center.dto.UserDTO;
import com.management.student_center.entity.Assignment;
import com.management.student_center.entity.Student;
import com.management.student_center.entity.StudentAssignment;
import com.management.student_center.entity.StudentSubject;
import com.management.student_center.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final StudentAssignmentRepository studentAssignmentRepository;
    private final StudentSubjectRepository studentSubjectRepository;

    public StudentAssignmentService(
            AssignmentRepository assignmentRepository,
            StudentAssignmentRepository studentAssignmentRepository,
            StudentSubjectRepository studentSubjectRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.studentAssignmentRepository = studentAssignmentRepository;
        this.studentSubjectRepository = studentSubjectRepository;
    }

    // ---------------------------------------------------------
    // POST /assign/:assignmentId  => assignToStudents
    // ---------------------------------------------------------
    @Transactional
    public int assignToStudents(Long assignmentId) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment không tồn tại"));

        Long subjectId = assignment.getSession().getSubject().getId();

        List<StudentSubject> studentSubjects = studentSubjectRepository.findBySubjectId(subjectId);

        if (studentSubjects.isEmpty()) {
            throw new RuntimeException("Không tìm thấy học sinh nào đang học môn này");
        }

        // Tạo danh sách StudentAssignment
        List<StudentAssignment> newAssignments = studentSubjects.stream().map(ss -> {
            StudentAssignment sa = new StudentAssignment();
            sa.setAssignment(assignment);
            sa.setStudent(ss.getStudent());
            sa.setSubmittedStatus("pending");
            sa.setFeedback(null);
            return sa;
        }).collect(Collectors.toList());

        studentAssignmentRepository.saveAll(newAssignments);

        return newAssignments.size();
    }

    // ---------------------------------------------------------
    // GET /by-assignment/:assignmentId
    // ---------------------------------------------------------
    public List<StudentAssignmentResponseDTO> getByAssignmentId(Long assignmentId) {

        List<StudentAssignment> list = studentAssignmentRepository.findByAssignmentId(assignmentId);

        if (list.isEmpty()) {
            throw new RuntimeException("Không tìm thấy học sinh nào được gán assignment này");
        }

        return list.stream().map(sa ->
            new StudentAssignmentResponseDTO(
                sa.getId(),
                sa.getAssignment().getId(),
                sa.getStudent().getId(),
                sa.getSubmittedStatus(),
                sa.getFeedback(),
                new StudentDTO(
                    sa.getStudent().getId(),
                    sa.getStudent().getGrade(),
                    sa.getStudent().getSchoolName(),
                    new UserDTO(
                        sa.getStudent().getUserInfo().getFullName(),
                        sa.getStudent().getUserInfo().getEmail(),
                        sa.getStudent().getUserInfo().getPhoneNumber(),
                        sa.getStudent().getUserInfo().getGender(),
                        sa.getStudent().getUserInfo().getImage()
                    )
                )
            )
        ).collect(Collectors.toList());
    }


    // ---------------------------------------------------------
    // PUT /assign/update/:id
    // ---------------------------------------------------------
    public StudentAssignment update(Long id, UpdateStudentAssignmentDTO dto) {
        StudentAssignment sa = studentAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy StudentAssignment"));

        if (dto.getSubmittedStatus() != null) {
            sa.setSubmittedStatus(dto.getSubmittedStatus());
        }
        if (dto.getFeedback() != null) {
            sa.setFeedback(dto.getFeedback());
        }

        return studentAssignmentRepository.save(sa);
    }
}
