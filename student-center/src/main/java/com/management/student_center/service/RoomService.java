package com.management.student_center.service;

import com.management.student_center.dto.ActiveSubjectDTO;
import com.management.student_center.dto.DeviceDTO;
import com.management.student_center.dto.DeviceUpdateDTO;
import com.management.student_center.dto.RoomListDTO;
import com.management.student_center.dto.RoomScheduleDTO;
import com.management.student_center.entity.Device;
import com.management.student_center.entity.Room;
import com.management.student_center.entity.Session;
import com.management.student_center.entity.User;
import com.management.student_center.enums.ActivityActionType;
import com.management.student_center.enums.ActivityTargetType;
import com.management.student_center.enums.DeviceType;
import com.management.student_center.enums.RoomManualStatus;
import com.management.student_center.repository.DeviceRepository;
import com.management.student_center.repository.RoomRepository;
import com.management.student_center.repository.SessionRepository;
import com.management.student_center.repository.StudentSubjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final SessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;

    public RoomService(RoomRepository roomRepository,
                       SessionRepository sessionRepository,
                       DeviceRepository deviceRepository,
                       StudentSubjectRepository studentSubjectRepository,
                       ActivityLogService activityLogService,
                       CurrentUserService currentUserService) {
        this.roomRepository = roomRepository;
        this.sessionRepository = sessionRepository;
        this.deviceRepository = deviceRepository;
        this.studentSubjectRepository = studentSubjectRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
    }
    
    // Helper method to determine room status
    private String determineRoomStatus(Room room) {
        if (room.getManualStatus() == RoomManualStatus.MAINTENANCE) {
            return "MAINTENANCE";
        }
        if (room.getManualStatus() == RoomManualStatus.DISABLED) {
            return "DISABLED";
        }
        
        Long activeSessions = sessionRepository.countActiveSessionsNow(room.getId());
        if (activeSessions > 0) {
            return "ACTIVE";
        }
        return "DISABLED";
    }
    
    private List<DeviceDTO> getDeviceDTOs(Long roomId) {
        return deviceRepository.findByRoomId(roomId)
            .stream()
            .map(d -> new DeviceDTO(d.getId(), d.getType() != null ? d.getType().name() : null))
            .collect(Collectors.toList());
    }
    
    public List<RoomListDTO> getAllRooms() {
        return roomRepository.findAll()
            .stream()
            .map(r -> new RoomListDTO(
                r.getId(), r.getName(), r.getSeatCapacity(),
                r.getManualStatus(), determineRoomStatus(r),
                getDeviceDTOs(r.getId()), getActiveSubjectByRoom(r))
            )
            .collect(Collectors.toList());
    }
    
    public RoomListDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return new RoomListDTO(
            room.getId(), room.getName(), room.getSeatCapacity(),
            room.getManualStatus(), determineRoomStatus(room),
            getDeviceDTOs(room.getId()), getActiveSubjectByRoom(room)
        );
    }
    
    public List<RoomScheduleDTO> getRoomSchedule(Long roomId, LocalDate startDate, LocalDate endDate) {
        if (!roomRepository.existsById(roomId)) {
            throw new RuntimeException("Không tìm thấy phòng");
        }
        return sessionRepository.findRoomSchedule(roomId, startDate, endDate);
    }
    
    // CREATE ROOM
    @Transactional
    public Room createRoom(String name, Integer seatCapacity, RoomManualStatus manualStatus) {
        User currentUser = currentUserService.getCurrentUser();
        
        Room room = new Room();
        room.setName(name);
        room.setSeatCapacity(seatCapacity);
        if (manualStatus != null) {
            room.setManualStatus(manualStatus);
        }
        Room savedRoom = roomRepository.save(room);
        
        // LOG ACTIVITY: TẠO PHÒNG
        String description = String.format("đã tạo phòng học mới \"%s\" (Sức chứa: %d)", name, seatCapacity);
        String meta = String.format(
            "{\"roomId\":%d,\"name\":\"%s\",\"seatCapacity\":%d,\"manualStatus\":\"%s\"}",
            savedRoom.getId(), escapeJson(name), seatCapacity,
            manualStatus != null ? manualStatus.name() : "ACTIVE"
        );
        
        activityLogService.log(currentUser, ActivityActionType.CREATE, ActivityTargetType.CLASSROOM,
                savedRoom.getId(), description, meta);
        
        return savedRoom;
    }
    
    // UPDATE ROOM
    @Transactional
    public Room updateRoom(Long id, String name, Integer seatCapacity,
                           RoomManualStatus manualStatus, List<DeviceUpdateDTO> devices) {
        User currentUser = currentUserService.getCurrentUser();
        
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        
        // Lưu thông tin cũ
        String oldName = room.getName();
        Integer oldSeatCapacity = room.getSeatCapacity();
        RoomManualStatus oldManualStatus = room.getManualStatus();
        
        List<String> changes = new ArrayList<>();
        
        // Cập nhật tên
        if (name != null && !name.equals(oldName)) {
            room.setName(name);
            changes.add(String.format("tên từ \"%s\" thành \"%s\"", oldName, name));
        }
        
        // Cập nhật sức chứa
        if (seatCapacity != null && !seatCapacity.equals(oldSeatCapacity)) {
            room.setSeatCapacity(seatCapacity);
            changes.add(String.format("sức chứa từ %d thành %d", oldSeatCapacity, seatCapacity));
        }
        
        // Cập nhật trạng thái manual
        if (manualStatus != null && manualStatus != oldManualStatus) {
            room.setManualStatus(manualStatus);
            String oldStatusName = getStatusName(oldManualStatus);
            String newStatusName = getStatusName(manualStatus);
            changes.add(String.format("trạng thái từ \"%s\" thành \"%s\"", oldStatusName, newStatusName));
        }
        
        // Xử lý devices
        List<String> deviceChanges = new ArrayList<>();
        if (devices != null) {
            List<Device> currentDevices = deviceRepository.findByRoomId(id);
            List<Long> currentDeviceIds = currentDevices.stream()
                .map(Device::getId)
                .collect(Collectors.toList());
            
            for (DeviceUpdateDTO deviceDTO : devices) {
                if ("ADD".equalsIgnoreCase(deviceDTO.getAction())) {
                    Device newDevice = new Device();
                    try {
                        DeviceType deviceType = DeviceType.valueOf(deviceDTO.getType().toUpperCase());
                        newDevice.setType(deviceType);
                        newDevice.setRoom(room);
                        deviceRepository.save(newDevice);
                        deviceChanges.add(String.format("thêm thiết bị \"%s\"", deviceDTO.getType()));
                    } catch (Exception e) {
                        throw new RuntimeException("Loại thiết bị không hợp lệ: " + deviceDTO.getType());
                    }
                } else if ("DELETE".equalsIgnoreCase(deviceDTO.getAction()) && deviceDTO.getId() != null) {
                    if (currentDeviceIds.contains(deviceDTO.getId())) {
                        Device deviceToDelete = deviceRepository.findById(deviceDTO.getId()).orElse(null);
                        if (deviceToDelete != null) {
                            String deviceType = deviceToDelete.getType() != null ? deviceToDelete.getType().name() : "unknown";
                            deviceRepository.deleteById(deviceDTO.getId());
                            deviceChanges.add(String.format("xóa thiết bị \"%s\"", deviceType));
                        }
                    }
                }
            }
        }
        
        changes.addAll(deviceChanges);
        
        Room updatedRoom = roomRepository.save(room);
        
        // LOG ACTIVITY: CẬP NHẬT PHÒNG
        String description;
        if (changes.isEmpty()) {
            description = String.format("đã cập nhật phòng \"%s\" - không có thay đổi", room.getName());
        } else {
            description = String.format("đã cập nhật phòng \"%s\": %s", room.getName(), String.join(", ", changes));
        }
        
        String meta = String.format(
            "{\"roomId\":%d,\"oldValues\":{\"name\":\"%s\",\"seatCapacity\":%d,\"manualStatus\":\"%s\"},\"newValues\":{\"name\":\"%s\",\"seatCapacity\":%d,\"manualStatus\":\"%s\"},\"deviceChanges\":%s}",
            id, escapeJson(oldName), oldSeatCapacity, oldManualStatus != null ? oldManualStatus.name() : "ACTIVE",
            escapeJson(room.getName()), room.getSeatCapacity(), room.getManualStatus() != null ? room.getManualStatus().name() : "ACTIVE",
            escapeJson(deviceChanges.toString())
        );
        
        activityLogService.log(currentUser, ActivityActionType.UPDATE, ActivityTargetType.CLASSROOM,
                id, description, meta);
        
        return updatedRoom;
    }
    
    // UPDATE MANUAL STATUS ONLY
    @Transactional
    public Room updateManualStatus(Long id, RoomManualStatus manualStatus) {
        User currentUser = currentUserService.getCurrentUser();
        
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        
        RoomManualStatus oldStatus = room.getManualStatus();
        
        if (manualStatus == RoomManualStatus.MAINTENANCE) {
            room.setManualStatus(RoomManualStatus.MAINTENANCE);
        } else if (manualStatus == RoomManualStatus.ACTIVE) {
            room.setManualStatus(RoomManualStatus.ACTIVE);
        } else {
            throw new RuntimeException("Chỉ được phép cập nhật trạng thái MAINTENANCE hoặc ACTIVE");
        }
        
        Room updatedRoom = roomRepository.save(room);
        
        // LOG ACTIVITY: CẬP NHẬT TRẠNG THÁI PHÒNG
        String oldStatusName = getStatusName(oldStatus);
        String newStatusName = getStatusName(manualStatus);
        String description = String.format("đã thay đổi trạng thái phòng \"%s\" từ \"%s\" thành \"%s\"",
                room.getName(), oldStatusName, newStatusName);
        String meta = String.format(
            "{\"roomId\":%d,\"roomName\":\"%s\",\"oldManualStatus\":\"%s\",\"newManualStatus\":\"%s\"}",
            id, escapeJson(room.getName()), oldStatus != null ? oldStatus.name() : "ACTIVE", manualStatus.name()
        );
        
        activityLogService.log(currentUser, ActivityActionType.UPDATE, ActivityTargetType.CLASSROOM,
                id, description, meta);
        
        return updatedRoom;
    }
    
    // DELETE ROOM
    @Transactional
    public void deleteRoom(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));
        
        String roomName = room.getName();
        Integer seatCapacity = room.getSeatCapacity();
        
        // Xóa tất cả devices trước
        deviceRepository.deleteByRoomId(id);
        // Xóa room
        roomRepository.deleteById(id);
        
        // LOG ACTIVITY: XÓA PHÒNG
        String description = String.format("đã xóa phòng học \"%s\" (Sức chứa: %d)", roomName, seatCapacity);
        String meta = String.format(
            "{\"roomId\":%d,\"name\":\"%s\",\"seatCapacity\":%d}",
            id, escapeJson(roomName), seatCapacity
        );
        
        activityLogService.log(currentUser, ActivityActionType.DELETE, ActivityTargetType.CLASSROOM,
                id, description, meta);
    }
    
    // DEVICE MANAGEMENT
    @Transactional
    public Device addDevice(Long roomId, String type) {
        User currentUser = currentUserService.getCurrentUser();
        
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        
        Device device = new Device();
        DeviceType deviceType;
        try {
            deviceType = DeviceType.valueOf(type.toUpperCase());
            device.setType(deviceType);
        } catch (Exception e) {
            throw new RuntimeException("Loại thiết bị không hợp lệ");
        }
        
        device.setRoom(room);
        Device savedDevice = deviceRepository.save(device);
        
        // LOG ACTIVITY: THÊM THIẾT BỊ
        String description = String.format("đã thêm thiết bị \"%s\" vào phòng \"%s\"", 
                type.toUpperCase(), room.getName());
        String meta = String.format(
            "{\"deviceId\":%d,\"deviceType\":\"%s\",\"roomId\":%d,\"roomName\":\"%s\"}",
            savedDevice.getId(), deviceType.name(), roomId, escapeJson(room.getName())
        );
        
        activityLogService.log(currentUser, ActivityActionType.CREATE, ActivityTargetType.CLASSROOM,
                roomId, description, meta);
        
        return savedDevice;
    }
    
    @Transactional
    public void removeDevice(Long deviceId) {
        User currentUser = currentUserService.getCurrentUser();
        
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị"));
        
        String deviceType = device.getType() != null ? device.getType().name() : "unknown";
        Long roomId = device.getRoom() != null ? device.getRoom().getId() : null;
        String roomName = device.getRoom() != null ? device.getRoom().getName() : "unknown";
        
        deviceRepository.deleteById(deviceId);
        
        // LOG ACTIVITY: XÓA THIẾT BỊ
        String description = String.format("đã xóa thiết bị \"%s\" khỏi phòng \"%s\"", deviceType, roomName);
        String meta = String.format(
            "{\"deviceId\":%d,\"deviceType\":\"%s\",\"roomId\":%d,\"roomName\":\"%s\"}",
            deviceId, deviceType, roomId != null ? roomId : 0, escapeJson(roomName)
        );
        
        activityLogService.log(currentUser, ActivityActionType.DELETE, ActivityTargetType.CLASSROOM,
                roomId != null ? roomId : 0, description, meta);
    }
    
    public List<DeviceDTO> getRoomDevices(Long roomId) {
        return getDeviceDTOs(roomId);
    }
    
    private ActiveSubjectDTO getActiveSubjectByRoom(Room room) {
        Session session = sessionRepository.findActiveSessionByRoomId(room.getId());
        if (session == null) return null;
        
        Long studentCount = studentSubjectRepository.countBySubjectId(session.getSubject().getId());
        
        return new ActiveSubjectDTO(
            session.getSubject().getId(),
            session.getSubject().getName(),
            session.getStartTime().toString(),
            session.getEndTime().toString(),
            studentCount
        );
    }
    
    // Helper methods
    private String getStatusName(RoomManualStatus status) {
        if (status == null) return "ACTIVE";
        switch (status) {
            case MAINTENANCE: return "Bảo trì";
            case DISABLED: return "Vô hiệu hóa";
            case ACTIVE: return "Hoạt động";
            default: return status.name();
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}