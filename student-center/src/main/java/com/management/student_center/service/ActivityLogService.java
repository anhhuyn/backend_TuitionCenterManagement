package com.management.student_center.service;

import com.management.student_center.dto.ActivityLogResponse;
import com.management.student_center.entity.ActivityLog;
import com.management.student_center.entity.User;
import com.management.student_center.enums.ActivityActionType;
import com.management.student_center.enums.ActivityTargetType;
import com.management.student_center.repository.ActivityLogRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    // =========================
    // CREATE LOG
    // =========================

    public void log(
            User user,
            ActivityActionType actionType,
            ActivityTargetType targetType,
            Long targetId,
            String description,
            String meta
    ) {

        ActivityLog activityLog = new ActivityLog();

        activityLog.setUser(user);

        activityLog.setActionType(actionType.name());

        activityLog.setTargetType(targetType.name());

        activityLog.setTargetId(targetId);

        activityLog.setDescription(description);

        activityLog.setMeta(meta);

        activityLogRepository.save(activityLog);
    }

    // =========================
    // ADMIN ACTIVITIES
    // =========================

    public List<ActivityLogResponse> getAdminActivities(int limit) {

        return activityLogRepository
                .findByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // =========================
    // TEACHER ACTIVITIES
    // =========================

    public List<ActivityLogResponse> getTeacherActivities(
            Long userId,
            int limit
    ) {

        return activityLogRepository
                .getTeacherActivities(
                        userId,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // =========================
    // DTO MAPPER
    // =========================

    private ActivityLogResponse mapToDTO(ActivityLog log) {

        ActivityLogResponse dto = new ActivityLogResponse();

        dto.setId(log.getId());
        
        dto.setUserId(log.getUser().getId());

        dto.setUserName(
                log.getUser() != null
                        ? log.getUser().getFullName()
                        : null
        );

        dto.setUserImage(
                log.getUser() != null
                        ? log.getUser().getImage()
                        : null
        );

        dto.setActionType(log.getActionType());

        dto.setTargetType(log.getTargetType());

        dto.setTargetId(log.getTargetId());

        dto.setDescription(log.getDescription());

        dto.setMeta(log.getMeta());

        dto.setCreatedAt(log.getCreatedAt());

        return dto;
    }
}