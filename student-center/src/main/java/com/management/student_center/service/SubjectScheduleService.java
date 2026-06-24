package com.management.student_center.service;

import com.management.student_center.dto.CreateSubjectScheduleRequest;
import com.management.student_center.dto.ManualSessionRequest;
import com.management.student_center.dto.RoomDTO;
import com.management.student_center.dto.ScheduleInfoDTO;
import com.management.student_center.dto.SessionDTO;
import com.management.student_center.dto.SubjectScheduleDTO;
import com.management.student_center.entity.Room;
import com.management.student_center.entity.Session;
import com.management.student_center.entity.Subject;
import com.management.student_center.entity.SubjectSchedule;
import com.management.student_center.entity.User;
import com.management.student_center.enums.ActivityActionType;
import com.management.student_center.enums.ActivityTargetType;
import com.management.student_center.repository.RoomRepository;
import com.management.student_center.repository.SessionRepository;
import com.management.student_center.repository.SubjectRepository;
import com.management.student_center.repository.SubjectScheduleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubjectScheduleService {

	private final SubjectScheduleRepository scheduleRepository;
	private final SessionRepository sessionRepository;
	private final SubjectRepository subjectRepository;
	private final RoomRepository roomRepository;
	private final ActivityLogService activityLogService;
	private final CurrentUserService currentUserService;
	private final SessionTeacherSyncService sessionTeacherSyncService;

	public SubjectScheduleService(SubjectScheduleRepository scheduleRepository, SessionRepository sessionRepository,
			SubjectRepository subjectRepository, RoomRepository roomRepository, ActivityLogService activityLogService,
			CurrentUserService currentUserService, SessionTeacherSyncService sessionTeacherSyncService) {
		this.scheduleRepository = scheduleRepository;
		this.sessionRepository = sessionRepository;
		this.subjectRepository = subjectRepository;
		this.roomRepository = roomRepository;
		this.activityLogService = activityLogService;
		this.currentUserService = currentUserService;
		this.sessionTeacherSyncService = sessionTeacherSyncService;
	}

	/**
	 * Lấy tất cả sessions theo subjectId (include room.name và
	 * subjectSchedule(dayOfWeek, startTime, endTime))
	 */
	@Transactional(readOnly = true)
	public List<SessionDTO> getScheduleBySubjectId(Long subjectId) {
		if (subjectId == null) {
			throw new IllegalArgumentException("subjectId is required");
		}

		List<Session> sessions = sessionRepository.findBySubjectIdWithRoomAndScheduleOrder(subjectId);
		return sessions.stream().map(this::toDto).collect(Collectors.toList());
	}

	/**
	 * Xoá session theo id
	 */
	@Transactional
	public Map<String, Object> deleteSession(Long sessionId) {
		User currentUser = currentUserService.getCurrentUser();

		if (sessionId == null) {
			throw new IllegalArgumentException("Thiếu sessionId để xoá");
		}

		Optional<Session> opt = sessionRepository.findById(sessionId);
		if (opt.isEmpty()) {
			throw new NoSuchElementException("Buổi học không tồn tại");
		}

		Session session = opt.get();

		// Lưu thông tin để ghi log
		Long subjectId = session.getSubject() != null ? session.getSubject().getId() : null;
		String subjectName = session.getSubject() != null ? session.getSubject().getName() : "Không xác định";
		LocalDate sessionDate = session.getSessionDate();
		LocalTime startTime = session.getStartTime();
		LocalTime endTime = session.getEndTime();
		String roomName = session.getRoom() != null ? session.getRoom().getName() : "Chưa có phòng";

		sessionRepository.delete(session);

		// LOG ACTIVITY: XÓA BUỔI HỌC
		String formattedDate = sessionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		String description = String.format("đã xóa buổi học ngày %s (%s - %s) của môn học %s tại phòng %s",
				formattedDate, startTime, endTime, subjectName, roomName);
		String meta = String.format(
				"{\"sessionId\":%d,\"subjectId\":%d,\"subjectName\":\"%s\",\"sessionDate\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomName\":\"%s\"}",
				sessionId, subjectId, escapeJson(subjectName), sessionDate, startTime, endTime, escapeJson(roomName));

		activityLogService.log(currentUser, ActivityActionType.DELETE, ActivityTargetType.SCHEDULE, sessionId,
				description, meta);

		Map<String, Object> result = new HashMap<>();
		result.put("message", "Đã xoá buổi học thành công");
		result.put("id", sessionId);
		return result;
	}

	/**
	 * Lấy session theo id (include room + schedule)
	 */
	@Transactional(readOnly = true)
	public SessionDTO getSessionById(Long sessionId) {
		Session s = sessionRepository.findByIdWithRoomAndSchedule(sessionId);
		if (s == null) {
			throw new NoSuchElementException("Session không tồn tại");
		}
		return toDto(s);
	}

	// helper mapper
	/*
	 * private SessionDTO toDto(Session s) {
	 * 
	 * RoomDTO roomDto = null; if (s.getRoom() != null) { roomDto = new
	 * RoomDTO(s.getRoom().getName()); }
	 * 
	 * ScheduleInfoDTO scheduleDto = null; if (s.getSchedule() != null) {
	 * scheduleDto = new ScheduleInfoDTO( s.getSchedule().getDayOfWeek(),
	 * s.getSchedule().getStartTime(), s.getSchedule().getEndTime() ); }
	 * 
	 * return new SessionDTO( s.getId(), s.getSubject() != null ?
	 * s.getSubject().getId() : null, s.getSchedule() != null ?
	 * s.getSchedule().getId() : null, s.getSessionDate(), s.getStartTime(),
	 * s.getEndTime(), s.getRoom() != null ? s.getRoom().getId() : null,
	 * s.getStatus(), roomDto, scheduleDto ); }
	 */

	private SessionDTO toDto(Session s) {
		String realtimeStatus = resolveStatus(s);

		RoomDTO roomDto = null;
		if (s.getRoom() != null) {
			roomDto = new RoomDTO(s.getRoom().getName());
		}

		ScheduleInfoDTO scheduleDto = null;
		if (s.getSchedule() != null) {
			scheduleDto = new ScheduleInfoDTO(s.getSchedule().getDayOfWeek(), s.getSchedule().getStartTime(),
					s.getSchedule().getEndTime());
		}

		return new SessionDTO(s.getId(), s.getSubject() != null ? s.getSubject().getId() : null,
				s.getSchedule() != null ? s.getSchedule().getId() : null, s.getSessionDate(), s.getStartTime(),
				s.getEndTime(), s.getRoom() != null ? s.getRoom().getId() : null, realtimeStatus, roomDto, scheduleDto);
	}

	private String resolveStatus(Session session) {
		String dbStatus = session.getStatus();

		if ("canceled".equals(dbStatus))
			return "canceled";
		if ("completed".equals(dbStatus))
			return "completed";

		LocalDate sessionDate = session.getSessionDate();
		LocalDate today = LocalDate.now();

		LocalTime now = LocalTime.now();
		LocalTime start = session.getStartTime();
		LocalTime end = session.getEndTime();

		if (sessionDate.isAfter(today))
			return "scheduled";
		if (sessionDate.isBefore(today))
			return "expired";

		// today
		if (now.isBefore(start))
			return "scheduled";
		if (now.isAfter(end))
			return "expired";

		return "ongoing";
	}

	/**
	 * Tạo lịch học và sinh sessions tự động
	 */
	@Transactional
	public Map<String, Object> createSubjectSchedule(CreateSubjectScheduleRequest req) {
		User currentUser = currentUserService.getCurrentUser();

		// 1️⃣ validate required
		if (req.getSubjectId() == null || req.getDayOfWeek() == null || req.getStartTime() == null
				|| req.getEndTime() == null || req.getStartDate() == null) {
			throw new IllegalArgumentException("Thiếu thông tin lịch học bắt buộc");
		}

		if (req.getEndTime() != null && !req.getEndTime().isAfter(req.getStartTime())) {
			throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
		}

		Subject subject = subjectRepository.findById(req.getSubjectId())
				.orElseThrow(() -> new NoSuchElementException("Lớp học không tồn tại"));

		Room room = null;
		if (req.getRoomId() != null) {
			room = roomRepository.findById(req.getRoomId())
					.orElseThrow(() -> new NoSuchElementException("Phòng học không tồn tại"));
		}

		// 2️⃣ Kiểm tra trùng lịch cùng phòng
		if (room != null) {
			boolean overlapRoom = scheduleRepository.existsByRoomAndDayOfWeekAndTimeOverlap(room, req.getDayOfWeek(),
					req.getStartTime(), req.getEndTime());
			if (overlapRoom) {
				throw new IllegalArgumentException("Phòng học này đã có lịch trong cùng ngày");
			}
		}

		// 3️⃣ Kiểm tra trùng lịch cùng lớp
		boolean overlapSubject = scheduleRepository.existsBySubjectAndDayOfWeekAndTimeOverlap(subject,
				req.getDayOfWeek(), req.getStartTime(), req.getEndTime());
		if (overlapSubject) {
			throw new IllegalArgumentException("Lớp học này đã có lịch trong cùng ngày");
		}

		// 4️⃣ Tạo SubjectSchedule mới
		SubjectSchedule schedule = new SubjectSchedule();
		schedule.setSubject(subject);
		schedule.setDayOfWeek(req.getDayOfWeek());
		schedule.setStartTime(req.getStartTime());
		schedule.setEndTime(req.getEndTime());
		schedule.setRoom(room);
		schedule.setStartDate(req.getStartDate());
		schedule.setEndDate(req.getEndDate());
		scheduleRepository.save(schedule);

		// 5️⃣ Sinh các Session theo ngày
		List<Session> sessions = generateSessions(schedule);

		// LOG ACTIVITY: TẠO LỊCH HỌC
		String dayOfWeekName = getDayOfWeekName(req.getDayOfWeek());
		String roomName = room != null ? room.getName() : "chưa có phòng";
		String description = String.format("đã tạo lịch học cho môn học %s (Khối %s): %s %s - %s tại phòng %s",
				subject.getName(), subject.getGrade(), dayOfWeekName, req.getStartTime(), req.getEndTime(), roomName);
		String meta = String.format(
				"{\"scheduleId\":%d,\"subjectId\":%d,\"subjectName\":\"%s\",\"subjectGrade\":\"%s\",\"dayOfWeek\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomId\":%s,\"roomName\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"sessionsGenerated\":%d}",
				schedule.getId(), subject.getId(), escapeJson(subject.getName()), subject.getGrade(),
				req.getDayOfWeek(), req.getStartTime(), req.getEndTime(), room != null ? room.getId() : "null",
				escapeJson(roomName), req.getStartDate(), req.getEndDate() != null ? req.getEndDate() : "null",
				sessions.size());

		activityLogService.log(currentUser, ActivityActionType.CREATE, ActivityTargetType.SCHEDULE, schedule.getId(),
				description, meta);

		// Convert to DTOs
		SubjectScheduleDTO scheduleDto = new SubjectScheduleDTO(schedule.getId(), schedule.getSubject().getId(),
				schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(),
				schedule.getRoom() != null ? schedule.getRoom().getId() : null, schedule.getStartDate(),
				schedule.getEndDate());

		List<SessionDTO> sessionDtos = sessions.stream().map(this::toDto).collect(Collectors.toList());

		Map<String, Object> result = new HashMap<>();
		result.put("schedule", scheduleDto);
		result.put("sessions", sessionDtos);
		return result;
	}

	private List<Session> generateSessions(SubjectSchedule schedule) {
		List<Session> sessionsToCreate = new ArrayList<>();
		LocalDate start = schedule.getStartDate();
		LocalDate end = schedule.getEndDate() != null ? schedule.getEndDate() : start;
		LocalTime startTime = schedule.getStartTime();
		LocalTime endTime = schedule.getEndTime();
		int dayOfWeek = schedule.getDayOfWeek();

		LocalDate current = start;
		while (!current.isAfter(end)) {
			if (current.getDayOfWeek().getValue() % 7 == dayOfWeek) { // Java: Monday=1 ... Sunday=7
				boolean exists = sessionRepository.existsBySubjectAndSessionDateAndStartTime(schedule.getSubject(),
						current, startTime);
				if (!exists && current.isAfter(LocalDate.now())) {
					Session session = new Session();
					session.setSubject(schedule.getSubject());
					session.setSchedule(schedule);
					session.setSessionDate(current);
					session.setStartTime(startTime);
					session.setEndTime(endTime);
					session.setRoom(schedule.getRoom());
					session.setStatus("scheduled");
					sessionsToCreate.add(session);
				}
			}
			current = current.plusDays(1);
		}

		if (!sessionsToCreate.isEmpty()) {

			List<Session> savedSessions = sessionRepository.saveAll(sessionsToCreate);

			for (Session session : savedSessions) {
				sessionTeacherSyncService.createMainTeacherAssignments(session);
			}

			return savedSessions;
		}

		return sessionsToCreate;
	}

	/**
	 * Thêm buổi học thủ công
	 */
	@Transactional
	public SessionDTO addManualSession(ManualSessionRequest req) {
		User currentUser = currentUserService.getCurrentUser();

		if (req.getSubjectId() == null || req.getSessionDate() == null || req.getStartTime() == null
				|| req.getEndTime() == null) {
			throw new IllegalArgumentException("Thiếu thông tin bắt buộc để tạo session");
		}

		if (!req.getEndTime().isAfter(req.getStartTime())) {
			throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
		}

		Subject subject = subjectRepository.findById(req.getSubjectId())
				.orElseThrow(() -> new NoSuchElementException("Lớp học không tồn tại"));

		Room room = null;
		if (req.getRoomId() != null) {
			room = roomRepository.findById(req.getRoomId())
					.orElseThrow(() -> new NoSuchElementException("Phòng học không tồn tại"));
		}

		// Kiểm tra trùng giờ phòng
		boolean overlapRoom = sessionRepository.existsByRoomAndDateAndTimeOverlap(room, req.getSessionDate(),
				req.getStartTime(), req.getEndTime());
		if (overlapRoom) {
			throw new IllegalArgumentException("Phòng này đã có buổi khác trong cùng ngày");
		}

		// Kiểm tra trùng giờ lớp
		boolean overlapSubject = sessionRepository.existsBySubjectAndDateAndTimeOverlap(subject, req.getSessionDate(),
				req.getStartTime(), req.getEndTime());
		if (overlapSubject) {
			throw new IllegalArgumentException("Lớp học này đã có buổi khác trong cùng ngày");
		}

		Session session = new Session();
		session.setSubject(subject);
		session.setSchedule(
				req.getScheduleId() != null ? scheduleRepository.findById(req.getScheduleId()).orElse(null) : null);
		session.setSessionDate(req.getSessionDate());
		session.setStartTime(req.getStartTime());
		session.setEndTime(req.getEndTime());
		session.setRoom(room);
		session.setStatus(req.getStatus() != null ? req.getStatus() : "scheduled");

		sessionRepository.save(session);

		sessionTeacherSyncService.createMainTeacherAssignments(session);
		// LOG ACTIVITY: THÊM BUỔI HỌC THỦ CÔNG
		String formattedDate = req.getSessionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		String roomName = room != null ? room.getName() : "chưa có phòng";
		String description = String.format(
				"đã thêm buổi học thủ công cho môn học %s (Khối %s) vào ngày %s (%s - %s) tại phòng %s",
				subject.getName(), subject.getGrade(), formattedDate, req.getStartTime(), req.getEndTime(), roomName);
		String meta = String.format(
				"{\"sessionId\":%d,\"subjectId\":%d,\"subjectName\":\"%s\",\"subjectGrade\":\"%s\",\"sessionDate\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomId\":%s,\"roomName\":\"%s\",\"scheduleId\":%s,\"type\":\"manual\"}",
				session.getId(), subject.getId(), escapeJson(subject.getName()), subject.getGrade(),
				req.getSessionDate(), req.getStartTime(), req.getEndTime(), room != null ? room.getId() : "null",
				escapeJson(roomName), req.getScheduleId() != null ? req.getScheduleId() : "null");

		activityLogService.log(currentUser, ActivityActionType.CREATE, ActivityTargetType.SCHEDULE, session.getId(),
				description, meta);

		return toDto(session);
	}

	/**
	 * Cập nhật buổi học
	 */
	@Transactional
	public SessionDTO updateSession(Long sessionId, ManualSessionRequest req) {
		User currentUser = currentUserService.getCurrentUser();

		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new NoSuchElementException("Buổi học không tồn tại"));

		// Lưu thông tin cũ để ghi log
		Long subjectId = session.getSubject() != null ? session.getSubject().getId() : null;
		String subjectName = session.getSubject() != null ? session.getSubject().getName() : "Không xác định";
		String subjectGrade = session.getSubject() != null ? session.getSubject().getGrade() : "Không xác định";
		LocalDate oldDate = session.getSessionDate();
		LocalTime oldStart = session.getStartTime();
		LocalTime oldEnd = session.getEndTime();
		String oldRoomName = session.getRoom() != null ? session.getRoom().getName() : "chưa có phòng";
		String oldStatus = session.getStatus();

		LocalDate newDate = req.getSessionDate() != null ? req.getSessionDate() : session.getSessionDate();
		LocalTime newStart = req.getStartTime() != null ? req.getStartTime() : session.getStartTime();
		LocalTime newEnd = req.getEndTime() != null ? req.getEndTime() : session.getEndTime();
		Room room = req.getRoomId() != null ? roomRepository.findById(req.getRoomId()).orElse(null) : session.getRoom();

		if (!newEnd.isAfter(newStart)) {
			throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
		}

		// Kiểm tra trùng giờ phòng (trừ chính nó)
		boolean overlapRoom = sessionRepository.existsByRoomAndDateAndTimeOverlapExcludingId(room, newDate, newStart,
				newEnd, sessionId);
		if (overlapRoom) {
			throw new IllegalArgumentException("Phòng này đã có buổi khác trong cùng ngày");
		}

		// Kiểm tra trùng giờ lớp (trừ chính nó)
		boolean overlapSubject = sessionRepository.existsBySubjectAndDateAndTimeOverlapExcludingId(session.getSubject(),
				newDate, newStart, newEnd, sessionId);
		if (overlapSubject) {
			throw new IllegalArgumentException("Lớp học này đã có buổi khác trong cùng ngày");
		}

		session.setSessionDate(newDate);
		session.setStartTime(newStart);
		session.setEndTime(newEnd);
		session.setRoom(room);
		session.setStatus(req.getStatus() != null ? req.getStatus() : session.getStatus());

		sessionRepository.save(session);

		// LOG ACTIVITY: CẬP NHẬT BUỔI HỌC
		List<String> changes = new ArrayList<>();

		if (!oldDate.equals(newDate)) {
			String oldDateStr = oldDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
			String newDateStr = newDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
			changes.add(String.format("ngày từ %s thành %s", oldDateStr, newDateStr));
		}
		if (!oldStart.equals(newStart)) {
			changes.add(String.format("giờ bắt đầu từ %s thành %s", oldStart, newStart));
		}
		if (!oldEnd.equals(newEnd)) {
			changes.add(String.format("giờ kết thúc từ %s thành %s", oldEnd, newEnd));
		}

		String newRoomName = room != null ? room.getName() : "chưa có phòng";
		if (!oldRoomName.equals(newRoomName)) {
			changes.add(String.format("phòng từ \"%s\" thành \"%s\"", oldRoomName, newRoomName));
		}

		String newStatus = req.getStatus() != null ? req.getStatus() : oldStatus;
		if (!oldStatus.equals(newStatus)) {
			changes.add(String.format("trạng thái từ \"%s\" thành \"%s\"", oldStatus, newStatus));
		}

		String description;
		if (changes.isEmpty()) {
			description = String.format("đã cập nhật buổi học của môn học %s (Khối %s) - không có thay đổi",
					subjectName, subjectGrade);
		} else {
			description = String.format("đã cập nhật buổi học của môn học %s (Khối %s): %s", subjectName, subjectGrade,
					String.join(", ", changes));
		}

		String meta = String.format(
				"{\"sessionId\":%d,\"subjectId\":%d,\"subjectName\":\"%s\",\"oldValues\":{\"sessionDate\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomName\":\"%s\",\"status\":\"%s\"},\"newValues\":{\"sessionDate\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomName\":\"%s\",\"status\":\"%s\"}}",
				sessionId, subjectId, escapeJson(subjectName), oldDate, oldStart, oldEnd, escapeJson(oldRoomName),
				oldStatus, newDate, newStart, newEnd, escapeJson(newRoomName), newStatus);

		activityLogService.log(currentUser, ActivityActionType.UPDATE, ActivityTargetType.SCHEDULE, sessionId,
				description, meta);

		return toDto(session);
	}

	// Helper methods
	private String getDayOfWeekName(int dayOfWeek) {
		String[] days = { "", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật" };
		return dayOfWeek >= 1 && dayOfWeek <= 7 ? days[dayOfWeek] : "Không xác định";
	}

	private String escapeJson(String str) {
		if (str == null)
			return "";
		return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
				"\\t");
	}

}
