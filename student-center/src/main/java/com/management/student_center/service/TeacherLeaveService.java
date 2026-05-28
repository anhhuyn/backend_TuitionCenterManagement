package com.management.student_center.service;

import com.management.student_center.dto.leave.AssignReplacementTeacherDTO;
import com.management.student_center.dto.leave.AvailableReplacementTeacherDTO;
import com.management.student_center.dto.leave.LeaveAffectedSessionDTO;
import com.management.student_center.dto.leave.PreviewAffectedSessionRequestDTO;
import com.management.student_center.dto.leave.PreviewAffectedSessionResponseDTO;
import com.management.student_center.dto.leave.PreviewReplacementPlanRequestDTO;
import com.management.student_center.dto.leave.PreviewReplacementSelectionDTO;
import com.management.student_center.dto.leave.ReplacementTeacherResponseDTO;
import com.management.student_center.dto.leave.TeacherLeaveApproveDTO;
import com.management.student_center.dto.leave.TeacherLeaveFilterRequestDTO;
import com.management.student_center.dto.leave.TeacherLeaveRequestDTO;
import com.management.student_center.dto.leave.TeacherLeaveResponseDTO;
import com.management.student_center.dto.teacher.TeacherAbsentResponse;
import com.management.student_center.entity.LeaveAffectedSession;
import com.management.student_center.entity.Session;
import com.management.student_center.entity.Teacher;
import com.management.student_center.entity.TeacherLeave;
import com.management.student_center.entity.TeacherSubject;
import com.management.student_center.entity.User;
import com.management.student_center.repository.LeaveAffectedSessionRepository;
import com.management.student_center.repository.SessionRepository;
import com.management.student_center.repository.TeacherLeaveRepository;
import com.management.student_center.repository.TeacherRepository;
import com.management.student_center.repository.TeacherSubjectRepository;
import com.management.student_center.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherLeaveService {

	private final TeacherLeaveRepository leaveRepository;
	private final TeacherRepository teacherRepository;
	private final UserRepository userRepository;
	private final TeacherSubjectRepository teacherSubjectRepository;
	private final SessionRepository sessionRepository;
	private final LeaveAffectedSessionRepository affectedSessionRepository;

	public TeacherLeaveService(TeacherLeaveRepository leaveRepository, TeacherRepository teacherRepository,
			UserRepository userRepository, TeacherSubjectRepository teacherSubjectRepository,
			SessionRepository sessionRepository, LeaveAffectedSessionRepository affectedSessionRepository,
			TeacherLeaveRepository teacherLeaveRepository) {
		this.leaveRepository = leaveRepository;
		this.teacherRepository = teacherRepository;
		this.userRepository = userRepository;
		this.teacherSubjectRepository = teacherSubjectRepository;
		this.sessionRepository = sessionRepository;
		this.affectedSessionRepository = affectedSessionRepository;
	}

	// =========================================================
	// CREATE LEAVE REQUEST
	// =========================================================

	@Transactional
	public TeacherLeaveResponseDTO createLeaveRequest(Long teacherUserId, TeacherLeaveRequestDTO dto) {
		Teacher teacher = teacherRepository.findByUserInfoId(teacherUserId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
		validateLeaveRequest(dto);
		int overlap = leaveRepository.countOverlappingLeave(teacher.getId(), dto.getStartDate(), dto.getEndDate(),
				dto.getStartTime(), dto.getEndTime());
		if (overlap > 0) {
			throw new RuntimeException("Đã tồn tại đơn nghỉ trùng thời gian");
		}
		TeacherLeave leave = new TeacherLeave();
		leave.setTeacher(teacher);
		leave.setStartDate(dto.getStartDate());
		leave.setEndDate(dto.getEndDate());
		leave.setStartTime(dto.getStartTime());
		leave.setEndTime(dto.getEndTime());
		leave.setReason(dto.getReason());
		leave.setLeaveType(dto.getLeaveType());
		leave.setStatus(TeacherLeave.LeaveStatus.PENDING);
		TeacherLeave saved = leaveRepository.save(leave);
		return mapToResponseDTO(saved);
	}

	// =========================================================
	// GET LIST
	// =========================================================

	public Page<TeacherLeaveResponseDTO> getLeaveRequests(Long userId, String role,
			TeacherLeaveFilterRequestDTO filter) {
		Pageable pageable = PageRequest.of(filter.getPage() - 1, filter.getSize(),
				Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<TeacherLeave> leavePage;

		if ("ADMIN".equalsIgnoreCase(role)) {
			// ✅ Lọc theo cả status và date range
			leavePage = leaveRepository.findByFilters(filter.getStatus(), filter.getStartDate(), filter.getEndDate(),
					pageable);
		} else {
			Teacher teacher = teacherRepository.findByUserInfoId(userId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));

			leavePage = leaveRepository.findByTeacherIdAndFilters(teacher.getId(), filter.getStatus(),
					filter.getStartDate(), filter.getEndDate(), pageable);
		}

		return leavePage.map(this::mapToResponseDTO);
	}

	// =========================================================
	// DETAIL
	// =========================================================

	public TeacherLeaveResponseDTO getLeaveRequestById(Long leaveId, Long userId, String role) {
		TeacherLeave leave = leaveRepository.findById(leaveId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ"));
		if (!"ADMIN".equalsIgnoreCase(role)) {
			Teacher teacher = teacherRepository.findByUserInfoId(userId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
			if (!leave.getTeacher().getId().equals(teacher.getId())) {
				throw new RuntimeException("Bạn không có quyền xem đơn này");
			}
		}
		TeacherLeaveResponseDTO response = mapToResponseDTO(leave);
		List<PreviewAffectedSessionResponseDTO> affectedSessions;
		if (leave.getStatus() == TeacherLeave.LeaveStatus.APPROVED) {
			affectedSessions = getSavedAffectedSessions(leave);
		} else {
			affectedSessions = previewAffectedSessionsFromLeave(leave);
		}
		response.setAffectedSessions(affectedSessions);
		return response;
	}

	// =========================================================
	// APPROVE / REJECT
	// =========================================================

	@Transactional
	public TeacherLeaveResponseDTO approveLeaveRequest(Long leaveId, Long approverUserId, TeacherLeaveApproveDTO dto) {
		User approver = userRepository.findById(approverUserId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));
		TeacherLeave leave = leaveRepository.findById(leaveId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ"));
		if (leave.getStatus() != TeacherLeave.LeaveStatus.PENDING) {
			throw new RuntimeException("Đơn đã được xử lý");
		}

		// ================= REJECT =================

		if (dto.getAction() == TeacherLeave.LeaveStatus.REJECTED) {
			leave.setStatus(TeacherLeave.LeaveStatus.REJECTED);
			leave.setApprover(approver);
			leave.setApprovedAt(LocalDateTime.now());
			TeacherLeave updated = leaveRepository.save(leave);
			cleanupAffectedSessions(leave.getId());
			return mapToResponseDTO(updated);
		}

		// ================= APPROVED =================

		if (dto.getAction() != TeacherLeave.LeaveStatus.APPROVED) {
			throw new RuntimeException("Action không hợp lệ");
		}
		if (dto.getAffectType() == TeacherLeave.AffectType.REPLACE) {
			if (dto.getReplacements() == null || dto.getReplacements().isEmpty()) {
				throw new RuntimeException("Phải chọn giáo viên thay thế");
			}
		}
		leave.setStatus(TeacherLeave.LeaveStatus.APPROVED);
		leave.setApprover(approver);
		leave.setApprovedAt(LocalDateTime.now());
		leave.setAffectType(dto.getAffectType());
		leave.setProcessed(false);
		if (dto.getComment() != null && !dto.getComment().isBlank()) {
			leave.setReason((leave.getReason() == null ? "" : leave.getReason()) + "\n[ADMIN]: " + dto.getComment());
		}
		TeacherLeave savedLeave = leaveRepository.save(leave);
		processLeaveApproval(savedLeave, dto.getReplacements());
		return mapToResponseDTO(savedLeave);
	}

	// =========================================================
	// PROCESS LEAVE
	// =========================================================

	@Transactional
	public void processLeaveApproval(TeacherLeave leave, List<PreviewReplacementSelectionDTO> replacements) {
		List<TeacherSubject> teacherSubjects = teacherSubjectRepository.findByTeacherId(leave.getTeacher().getId());
		List<Integer> subjectIds = teacherSubjects.stream().map(ts -> ts.getSubject().getId().intValue())
				.collect(Collectors.toList());
		if (subjectIds.isEmpty()) {
			return;
		}
		List<Session> sessions = sessionRepository.findSessionsForLeave(subjectIds, leave.getStartDate(),
				leave.getEndDate());
		for (Session session : sessions) {
			if (session.getSessionDate().isBefore(LocalDate.now())) {
				continue;
			}
			if (!isSessionAffected(leave, session)) {
				continue;
			}
			LeaveAffectedSession las = new LeaveAffectedSession();
			las.setLeave(leave);
			las.setSession(session);
			las.setOriginalTeacherId(leave.getTeacher().getId().intValue());
			// ==========================================
			// HƯỚNG 1: HỦY LỚP
			// ==========================================
			if (leave.getAffectType() == TeacherLeave.AffectType.CANCEL) {
				las.setStatus(LeaveAffectedSession.Status.SKIPPED);
				las.setProcessedAt(LocalDateTime.now());
				session.setStatus("cancelled");
				sessionRepository.save(session);
			}

			// ==========================================
			// HƯỚNG 2: TÌM GIÁO VIÊN THAY
			// ==========================================

			else {
				las.setStatus(LeaveAffectedSession.Status.PENDING);
				las.setReplacementResponse(LeaveAffectedSession.ReplacementResponse.PENDING);
				Long replacementTeacherId = findReplacementTeacherId(session.getId(), replacements);
				if (replacementTeacherId != null) {
					las.setReplacementTeacherId(replacementTeacherId.intValue());
				}
			}
			affectedSessionRepository.save(las);
		}
		if (leave.getAffectType() == TeacherLeave.AffectType.CANCEL) {
			leave.setProcessed(true);
			leaveRepository.save(leave);
		}
	}

	private Long findReplacementTeacherId(Long sessionId, List<PreviewReplacementSelectionDTO> replacements) {
		if (replacements == null || replacements.isEmpty()) {
			return null;
		}
		return replacements.stream().filter(r -> r.getSessionId().equals(sessionId))
				.map(PreviewReplacementSelectionDTO::getReplacementTeacherId).findFirst().orElse(null);
	}
	// =========================================================
	// ADMIN ASSIGN TEACHER
	// =========================================================

	@Transactional
	public void assignReplacementTeacher(Long affectedSessionId, AssignReplacementTeacherDTO dto) {
		LeaveAffectedSession las = affectedSessionRepository.findById(affectedSessionId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy session ảnh hưởng"));
		if (las.getLeave().getAffectType() != TeacherLeave.AffectType.REPLACE) {
			throw new RuntimeException("Đơn này không thuộc dạng REPLACE");
		}
		Session session = las.getSession();
		boolean hasTeachingConflict = sessionRepository.existsTeacherSessionOverlap(
				dto.getReplacementTeacherId().longValue(), session.getSessionDate(), session.getStartTime(),
				session.getEndTime());
		if (hasTeachingConflict) {
			throw new RuntimeException("Giáo viên bị trùng lịch dạy");
		}
		boolean hasLeaveConflict = leaveRepository.existsTeacherLeaveOverlap(dto.getReplacementTeacherId().longValue(),
				session.getSessionDate(), session.getStartTime(), session.getEndTime());
		if (hasLeaveConflict) {
			throw new RuntimeException("Giáo viên đang nghỉ phép");
		}
		boolean hasReplacementConflict = affectedSessionRepository.existsReplacementConflict(
				dto.getReplacementTeacherId().intValue(), session.getSessionDate(), session.getStartTime(),
				session.getEndTime());
		if (hasReplacementConflict) {
			throw new RuntimeException("Giáo viên đã nhận dạy thay session khác");
		}
		if (session.getSessionDate().isBefore(LocalDate.now())) {
			throw new RuntimeException("Không thể assign cho session quá khứ");
		}
		las.setReplacementTeacherId(dto.getReplacementTeacherId());
		las.setAdminNote(dto.getAdminNote());
		las.setReplacementResponse(LeaveAffectedSession.ReplacementResponse.PENDING);
		affectedSessionRepository.save(las);
		// TODO:
		// gửi notification cho teacher
	}

	// =========================================================
	// TEACHER RESPONSE
	// =========================================================

	@Transactional
	public void replacementTeacherResponse(Long affectedSessionId, Integer teacherId,
			ReplacementTeacherResponseDTO dto) {
		LeaveAffectedSession las = affectedSessionRepository.findById(affectedSessionId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy session"));
		System.out.println("🟢 TeacherId from token: " + teacherId);
		System.out.println("🟡 Replacement ID in DB: " + las.getReplacementTeacherId());
		if (las.getReplacementTeacherId() == null) {
			throw new RuntimeException("Chưa có giáo viên thay thế");
		}
		if (las.getReplacementResponse() == LeaveAffectedSession.ReplacementResponse.ACCEPTED) {
			throw new RuntimeException("Session này đã được chấp nhận");
		}
		Integer replacementId = las.getReplacementTeacherId();
		if (las.getStatus() == LeaveAffectedSession.Status.RESOLVED) {
			throw new RuntimeException("Session đã được xử lý");
		}
		if (replacementId == null || !replacementId.equals(teacherId)) {
			throw new RuntimeException("Bạn không được phản hồi session này");
		}
		// ==========================================
		// ACCEPT
		// ==========================================

		if (dto.getResponse() == LeaveAffectedSession.ReplacementResponse.ACCEPTED) {
			las.setReplacementResponse(LeaveAffectedSession.ReplacementResponse.ACCEPTED);
			las.setStatus(LeaveAffectedSession.Status.RESOLVED);
			las.setProcessedAt(LocalDateTime.now());
			las.setReplacementResponseAt(LocalDateTime.now());
			affectedSessionRepository.save(las);
			checkLeaveProcessed(las.getLeave());
			return;
		}
		// ==========================================
		// REJECT
		// ==========================================
		if (dto.getResponse() == LeaveAffectedSession.ReplacementResponse.REJECTED) {
			las.setReplacementResponse(LeaveAffectedSession.ReplacementResponse.REJECTED);
			las.setStatus(LeaveAffectedSession.Status.PENDING);
			las.setReplacementTeacherId(null);
			las.setReplacementResponseAt(LocalDateTime.now());
			affectedSessionRepository.save(las);
		}
	}

	// =========================================================
	// CANCEL REQUEST
	// =========================================================
	@Transactional
	public void cancelLeaveRequest(Long leaveId, Long teacherUserId) {
		Teacher teacher = teacherRepository.findByUserInfoId(teacherUserId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
		TeacherLeave leave = leaveRepository.findById(leaveId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ"));
		if (!leave.getTeacher().getId().equals(teacher.getId())) {
			throw new RuntimeException("Bạn không có quyền");
		}
		if (leave.getStatus() != TeacherLeave.LeaveStatus.PENDING) {
			throw new RuntimeException("Chỉ được hủy đơn PENDING");
		}
		leave.setStatus(TeacherLeave.LeaveStatus.CANCELLED);
		leaveRepository.save(leave);
	}

	// =========================================================
	// GET AFFECTED SESSIONS
	// =========================================================
	public List<LeaveAffectedSessionDTO> getAffectedSessionsByLeave(Long leaveId) {
		List<LeaveAffectedSession> list = affectedSessionRepository.findByLeaveId(leaveId);
		return list.stream().map((LeaveAffectedSession las) -> {
			LeaveAffectedSessionDTO dto = new LeaveAffectedSessionDTO();
			dto.setId(las.getId());
			dto.setSessionId(las.getSession().getId());
			dto.setSessionDate(las.getSession().getSessionDate().toString());
			dto.setStatus(las.getStatus().name());
			dto.setReplacementResponse(
					las.getReplacementResponse() != null ? las.getReplacementResponse().name() : null);
			dto.setLeaveId(las.getLeave().getId());
			dto.setOriginalTeacherId(las.getOriginalTeacherId().longValue());
			if (las.getReplacementTeacherId() != null) {
				dto.setReplacementTeacherId(las.getReplacementTeacherId().longValue());
				teacherRepository.findById(las.getReplacementTeacherId().longValue())
						.ifPresent(teacher -> dto.setReplacementTeacherName(teacher.getUserInfo().getFullName()));
			}
			return dto;
		}).collect(Collectors.toList());
	}

	// =========================================================
	// COUNT PENDING
	// =========================================================

	public long countPendingLeaves() {
		return leaveRepository.countByStatus(TeacherLeave.LeaveStatus.PENDING);
	}

	// =========================================================
	// CLEANUP
	// =========================================================

	@Transactional
	public void cleanupAffectedSessions(Long leaveId) {
		affectedSessionRepository.deleteByLeaveId(leaveId);
	}
	// =========================================================
	// CHECK SESSION AFFECTED
	// =========================================================

	private boolean isSessionAffected(TeacherLeave leave, Session session) {
		if (leave.getStartTime() == null || leave.getEndTime() == null) {
			return true;
		}
		return leave.getStartTime().isBefore(session.getEndTime())
				&& leave.getEndTime().isAfter(session.getStartTime());
	}

	// =========================================================
	// CHECK LEAVE PROCESSED
	// =========================================================

	private void checkLeaveProcessed(TeacherLeave leave) {
		boolean hasPending = affectedSessionRepository.existsByLeaveIdAndStatus(leave.getId(),
				LeaveAffectedSession.Status.PENDING);
		if (!hasPending) {
			leave.setProcessed(true);
			leaveRepository.save(leave);
		}
	}

	private List<PreviewAffectedSessionResponseDTO> previewAffectedSessionsFromLeave(TeacherLeave leave) {
		List<TeacherSubject> teacherSubjects = teacherSubjectRepository.findByTeacherId(leave.getTeacher().getId());
		List<Integer> subjectIds = teacherSubjects.stream().map(ts -> ts.getSubject().getId().intValue())
				.collect(Collectors.toList());
		if (subjectIds.isEmpty()) {
			return List.of();
		}
		List<Session> sessions = sessionRepository.findSessionsForLeave(subjectIds, leave.getStartDate(),
				leave.getEndDate());
		return sessions.stream()
				// bỏ session quá khứ
				.filter(s -> !s.getSessionDate().isBefore(LocalDate.now()))
				// overlap time
				.filter(s -> isSessionAffected(leave, s)).map(session -> {
					PreviewAffectedSessionResponseDTO dto = new PreviewAffectedSessionResponseDTO();
					dto.setSessionId(session.getId());
					dto.setSessionDate(session.getSessionDate());
					dto.setStartTime(session.getStartTime());
					dto.setEndTime(session.getEndTime());
					// SỬA ENTITY ĐÚNG PROJECT CỦA BẠN
					dto.setClassName(session.getClass().getName());
					dto.setSubjectName(session.getSubject().getName());
					if (session.getRoom() != null) {
						dto.setRoomName(session.getRoom().getName());
					}
					return dto;
				}).collect(Collectors.toList());
	}

	private List<PreviewAffectedSessionResponseDTO> getSavedAffectedSessions(TeacherLeave leave) {
		List<LeaveAffectedSession> list = affectedSessionRepository.findByLeaveId(leave.getId());
		return list.stream().map(las -> {
			Session session = las.getSession();
			PreviewAffectedSessionResponseDTO dto = new PreviewAffectedSessionResponseDTO();
			dto.setSessionId(session.getId());
			dto.setSessionDate(session.getSessionDate());
			dto.setStartTime(session.getStartTime());
			dto.setEndTime(session.getEndTime());
			// SỬA ENTITY ĐÚNG PROJECT
			dto.setClassName(session.getClass().getName());
			dto.setSubjectName(session.getSubject().getName());
			if (session.getRoom() != null) {
				dto.setRoomName(session.getRoom().getName());
			}
			return dto;
		}).collect(Collectors.toList());
	}
	// =========================================================
	// VALIDATE
	// =========================================================

	private void validateLeaveRequest(TeacherLeaveRequestDTO dto) {
		if (dto.getStartDate() == null || dto.getEndDate() == null) {
			throw new RuntimeException("Ngày nghỉ không được để trống");
		}
		if (dto.getStartDate().isAfter(dto.getEndDate())) {
			throw new RuntimeException("Ngày bắt đầu phải <= ngày kết thúc");
		}

		if (dto.getStartTime() != null && dto.getEndTime() != null) {
			if (!dto.getStartTime().isBefore(dto.getEndTime())) {
				throw new RuntimeException("startTime phải nhỏ hơn endTime");
			}
		}
		if (dto.getLeaveType() == null || dto.getLeaveType().isBlank()) {
			throw new RuntimeException("Loại nghỉ không được để trống");
		}
	}

	// =========================================================
	// MAP DTO
	// =========================================================

	private TeacherLeaveResponseDTO mapToResponseDTO(TeacherLeave leave) {
		TeacherLeaveResponseDTO dto = new TeacherLeaveResponseDTO();
		dto.setId(leave.getId());
		dto.setTeacherId(leave.getTeacher().getId());
		dto.setTeacherName(leave.getTeacher().getUserInfo().getFullName());
		dto.setTeacherEmail(leave.getTeacher().getUserInfo().getEmail());
		dto.setStartDate(leave.getStartDate());
		dto.setEndDate(leave.getEndDate());
		dto.setStartTime(leave.getStartTime());
		dto.setEndTime(leave.getEndTime());
		dto.setReason(leave.getReason());
		dto.setLeaveType(leave.getLeaveType());
		dto.setStatus(leave.getStatus().name());
		if (leave.getAffectType() != null) {

			dto.setAffectType(leave.getAffectType().name());
		}
		dto.setProcessed(leave.getProcessed());
		if (leave.getApprover() != null) {
			dto.setApproverId(leave.getApprover().getId());
			dto.setApproverName(leave.getApprover().getFullName());
		}
		dto.setApprovedAt(leave.getApprovedAt());
		dto.setCreatedAt(leave.getCreatedAt());
		dto.setUpdatedAt(leave.getUpdatedAt());
		return dto;
	}

	public List<PreviewAffectedSessionResponseDTO> previewAffectedSessions(Long teacherUserId,
			PreviewAffectedSessionRequestDTO dto) {
		Teacher teacher = teacherRepository.findByUserInfoId(teacherUserId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
		validatePreviewRequest(dto);
		List<TeacherSubject> teacherSubjects = teacherSubjectRepository.findByTeacherId(teacher.getId());
		List<Integer> subjectIds = teacherSubjects.stream().map(ts -> ts.getSubject().getId().intValue())
				.collect(Collectors.toList());
		if (subjectIds.isEmpty()) {
			return List.of();
		}
		List<Session> sessions = sessionRepository.findSessionsForLeave(subjectIds, dto.getStartDate(),
				dto.getEndDate());
		return sessions.stream().filter(s -> !s.getSessionDate().isBefore(LocalDate.now()))
				.filter(s -> isPreviewSessionAffected(dto.getStartTime(), dto.getEndTime(), s)).map(session -> {
					PreviewAffectedSessionResponseDTO response = new PreviewAffectedSessionResponseDTO();
					response.setSessionId(session.getId());
					response.setSessionDate(session.getSessionDate());
					response.setStartTime(session.getStartTime());
					response.setEndTime(session.getEndTime());
					response.setClassName(session.getClass().getName());
					response.setSubjectName(session.getSubject().getName());
					if (session.getRoom() != null) {
						response.setRoomName(session.getRoom().getName());
					}
					return response;
				}).collect(Collectors.toList());
	}

	private boolean isPreviewSessionAffected(LocalTime startTime, LocalTime endTime, Session session) {
		if (startTime == null || endTime == null) {
			return true;
		}
		return startTime.isBefore(session.getEndTime()) && endTime.isAfter(session.getStartTime());
	}

	private void validatePreviewRequest(PreviewAffectedSessionRequestDTO dto) {
		if (dto.getStartDate() == null || dto.getEndDate() == null) {
			throw new RuntimeException("Ngày nghỉ không được để trống");
		}
		if (dto.getStartDate().isAfter(dto.getEndDate())) {
			throw new RuntimeException("Ngày bắt đầu phải <= ngày kết thúc");
		}
		if (dto.getStartTime() != null && dto.getEndTime() != null) {
			if (!dto.getStartTime().isBefore(dto.getEndTime())) {
				throw new RuntimeException("startTime phải nhỏ hơn endTime");
			}
		}
	}

	public List<AvailableReplacementTeacherDTO> getAvailableReplacementTeachers(Long affectedSessionId) {
		LeaveAffectedSession las = affectedSessionRepository.findById(affectedSessionId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy session"));
		Session session = las.getSession();
		List<Teacher> teachers = teacherRepository.findAll();
		return teachers.stream().filter(t -> !t.getId().equals(las.getLeave().getTeacher().getId()))
				.filter(t -> !sessionRepository.existsTeacherSessionOverlap(t.getId().longValue(),
						session.getSessionDate(), session.getStartTime(), session.getEndTime()))
				.filter(t -> !leaveRepository.existsTeacherLeaveOverlap(t.getId(), session.getSessionDate(),
						session.getStartTime(), session.getEndTime()))
				.filter(t -> !affectedSessionRepository.existsReplacementConflict(t.getId().intValue(),
						session.getSessionDate(), session.getStartTime(), session.getEndTime()))
				.map(t -> {
					AvailableReplacementTeacherDTO dto = new AvailableReplacementTeacherDTO();
					dto.setTeacherId(t.getId());
					dto.setTeacherName(t.getUserInfo().getFullName());
					dto.setTeacherEmail(t.getUserInfo().getEmail());
					return dto;
				}).toList();
	}

	public List<AvailableReplacementTeacherDTO> getAvailableReplacementTeachers(Long sessionId, Long leaveId) {
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy session"));
		TeacherLeave leave = leaveRepository.findById(leaveId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn nghỉ"));
		List<Teacher> teachers = teacherRepository.findAll();
		return teachers.stream().filter(t -> !t.getId().equals(leave.getTeacher().getId()))
				.filter(t -> !sessionRepository.existsTeacherSessionOverlap(t.getId().longValue(),
						session.getSessionDate(), session.getStartTime(), session.getEndTime()))
				.filter(t -> !leaveRepository.existsTeacherLeaveOverlap(t.getId(), session.getSessionDate(),
						session.getStartTime(), session.getEndTime()))
				.filter(t -> !affectedSessionRepository.existsReplacementConflict(t.getId().intValue(),
						session.getSessionDate(), session.getStartTime(), session.getEndTime()))
				.map(t -> {
					AvailableReplacementTeacherDTO dto = new AvailableReplacementTeacherDTO();
					dto.setTeacherId(t.getId());
					dto.setTeacherName(t.getUserInfo().getFullName());
					dto.setTeacherEmail(t.getUserInfo().getEmail());
					return dto;
				}).toList();
	}

	public List<PreviewAffectedSessionResponseDTO> previewReplacementPlan(Long teacherUserId,
			PreviewReplacementPlanRequestDTO dto) {
		Teacher teacher = teacherRepository.findByUserInfoId(teacherUserId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
		validatePreviewReplacementRequest(dto);
		List<TeacherSubject> teacherSubjects = teacherSubjectRepository.findByTeacherId(teacher.getId());
		List<Integer> subjectIds = teacherSubjects.stream().map(ts -> ts.getSubject().getId().intValue()).toList();
		if (subjectIds.isEmpty()) {
			return List.of();
		}
		List<Session> sessions = sessionRepository.findSessionsForLeave(subjectIds, dto.getStartDate(),
				dto.getEndDate());
		return sessions.stream().filter(s -> !s.getSessionDate().isBefore(LocalDate.now()))
				.filter(s -> isPreviewSessionAffected(dto.getStartTime(), dto.getEndTime(), s)).map(session -> {
					PreviewAffectedSessionResponseDTO response = new PreviewAffectedSessionResponseDTO();
					response.setSessionId(session.getId());
					response.setSessionDate(session.getSessionDate());
					response.setStartTime(session.getStartTime());
					response.setEndTime(session.getEndTime());
					response.setClassName(session.getClass().getName());
					response.setSubjectName(session.getSubject().getName());
					if (session.getRoom() != null) {
						response.setRoomName(session.getRoom().getName());
					}
					if (dto.getReplacements() != null) {
						dto.getReplacements().stream().filter(r -> r.getSessionId().equals(session.getId())).findFirst()
								.ifPresent(selection -> {
									response.setReplacementTeacherId(selection.getReplacementTeacherId());
									teacherRepository.findById(selection.getReplacementTeacherId()).ifPresent(
											t -> response.setReplacementTeacherName(t.getUserInfo().getFullName()));
								});
					}
					return response;
				}).toList();
	}

	private void validatePreviewReplacementRequest(PreviewReplacementPlanRequestDTO dto) {
		if (dto.getStartDate() == null || dto.getEndDate() == null) {
			throw new RuntimeException("Ngày nghỉ không được để trống");
		}
		if (dto.getStartDate().isAfter(dto.getEndDate())) {
			throw new RuntimeException("Ngày bắt đầu phải <= ngày kết thúc");
		}

		if (dto.getStartTime() != null && dto.getEndTime() != null) {

			if (!dto.getStartTime().isBefore(dto.getEndTime())) {

				throw new RuntimeException("startTime phải nhỏ hơn endTime");
			}
		}
	}

	@Transactional
	public void cancelAffectedSession(Long affectedSessionId) {
		LeaveAffectedSession las = affectedSessionRepository.findById(affectedSessionId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy session bị ảnh hưởng"));
		if (las.getStatus() == LeaveAffectedSession.Status.RESOLVED) {
			throw new RuntimeException("Session đã có giáo viên nhận dạy thay");
		}
		Session session = las.getSession();
		session.setStatus("cancelled");
		las.setStatus(LeaveAffectedSession.Status.SKIPPED);
		las.setProcessedAt(LocalDateTime.now());
		affectedSessionRepository.save(las);
		sessionRepository.save(session);
		checkLeaveProcessed(las.getLeave());
	}

	public List<TeacherAbsentResponse> getAbsentTeachersThisWeek() {
		LocalDate today = LocalDate.now();
		LocalDate weekStart = today.with(DayOfWeek.MONDAY);
		LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
		List<TeacherLeave> leaves = leaveRepository.findApprovedLeavesInWeek(weekStart, weekEnd);
		return leaves.stream()
				.map(tl -> new TeacherAbsentResponse(tl.getId(), tl.getTeacher().getId(),
						tl.getTeacher().getUserInfo().getFullName(), tl.getTeacher().getUserInfo().getImage(),
						tl.getStartDate(), tl.getEndDate(), tl.getLeaveType(), tl.getReason()))
				.toList();
	}

	public long countAbsentTeachersThisWeek() {
		return getAbsentTeachersThisWeek().size();
	}

	// =========================================================
	// GET REPLACEMENT SESSIONS FOR TEACHER
	// =========================================================

	public List<LeaveAffectedSessionDTO> getReplacementSessions(Long teacherUserId) {
		Teacher teacher = teacherRepository.findByUserInfoId(teacherUserId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên"));
		List<LeaveAffectedSession> list = affectedSessionRepository
				.findByReplacementTeacherId(teacher.getId().intValue());
		return list.stream().map(las -> {
			LeaveAffectedSessionDTO dto = new LeaveAffectedSessionDTO();
			dto.setId(las.getId());
			dto.setLeaveId(las.getLeave().getId());
			dto.setSessionId(las.getSession().getId());
			dto.setSessionDate(las.getSession().getSessionDate().toString());
			dto.setStatus(las.getStatus().name());
			if (las.getReplacementResponse() != null) {
				dto.setReplacementResponse(las.getReplacementResponse().name());
			}
			dto.setOriginalTeacherId(las.getOriginalTeacherId().longValue());
			dto.setReplacementTeacherId(las.getReplacementTeacherId().longValue());
			// teacher nghỉ
			teacherRepository.findById(las.getOriginalTeacherId().longValue())
					.ifPresent(t -> dto.setOriginalTeacherName(t.getUserInfo().getFullName()));
			// teacher dạy thay
			teacherRepository.findById(las.getReplacementTeacherId().longValue())
					.ifPresent(t -> dto.setReplacementTeacherName(t.getUserInfo().getFullName()));
			// thêm info session
			dto.setClassName(las.getSession().getClass().getName());
			dto.setSubjectName(las.getSession().getSubject().getName());
			if (las.getSession().getRoom() != null) {
				dto.setRoomName(las.getSession().getRoom().getName());
			}
			dto.setStartTime(las.getSession().getStartTime());
			dto.setEndTime(las.getSession().getEndTime());
			return dto;
		}).toList();
	}

}