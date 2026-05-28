package com.management.student_center.controller;

import com.management.student_center.dto.*;
import com.management.student_center.entity.Device;
import com.management.student_center.entity.Room;
import com.management.student_center.enums.RoomManualStatus;
import com.management.student_center.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/v1/api")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms() {
        try {
            List<RoomListDTO> rooms = roomService.getAllRooms();
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Lấy danh sách phòng học thành công");
            body.put("data", rooms);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra khi lấy danh sách phòng học", "error", e.getMessage()));
        }
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getRoomById(@PathVariable Long roomId) {
        try {
            RoomListDTO room = roomService.getRoomById(roomId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lấy thông tin phòng thành công", "data", room));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/rooms/{roomId}/schedule")
    public ResponseEntity<?> getRoomSchedule(@PathVariable Long roomId, 
                                             @RequestParam String startDate,
                                             @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<RoomScheduleDTO> schedule = roomService.getRoomSchedule(roomId, start, end);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Lấy lịch phòng thành công");
            body.put("data", schedule);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Có lỗi xảy ra khi lấy lịch phòng", "error", e.getMessage()));
        }
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> createRoom(@RequestBody RoomRequestDTO req) {
        try {
            Room room = roomService.createRoom(req.getName(), req.getSeatCapacity(), req.getManualStatus());
            return ResponseEntity.ok(Map.of("success", true, "message", "Tạo phòng thành công", "data", room));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Tạo phòng thất bại", "error", e.getMessage()));
        }
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody RoomRequestDTO req) {
        try {
            Room updated = roomService.updateRoom(id, req.getName(), req.getSeatCapacity(), 
                    req.getManualStatus(), req.getDevices());
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật phòng thành công", "data", updated));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Cập nhật thất bại", "error", e.getMessage()));
        }
    }

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<?> updateManualStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("manualStatus");
            RoomManualStatus manualStatus = RoomManualStatus.valueOf(status);
            Room updated = roomService.updateManualStatus(id, manualStatus);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật trạng thái phòng thành công", "data", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Trạng thái không hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Cập nhật thất bại", "error", e.getMessage()));
        }
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa phòng thành công", "id", id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Xóa phòng thất bại", "error", e.getMessage()));
        }
    }

    @GetMapping("/rooms/{roomId}/devices")
    public ResponseEntity<?> getRoomDevices(@PathVariable Long roomId) {
        try {
            List<DeviceDTO> devices = roomService.getRoomDevices(roomId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lấy danh sách thiết bị thành công", "data", devices));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/rooms/{roomId}/devices")
    public ResponseEntity<?> addDevice(@PathVariable Long roomId, @RequestBody DeviceRequestDTO request) {
        try {
            Device device = roomService.addDevice(roomId, request.getType());
            return ResponseEntity.ok(Map.of("success", true, "message", "Thêm thiết bị thành công", "data", device));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Thêm thiết bị thất bại", "error", e.getMessage()));
        }
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> removeDevice(@PathVariable Long deviceId) {
        try {
            roomService.removeDevice(deviceId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa thiết bị thành công", "deviceId", deviceId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Xóa thiết bị thất bại", "error", e.getMessage()));
        }
    }
}