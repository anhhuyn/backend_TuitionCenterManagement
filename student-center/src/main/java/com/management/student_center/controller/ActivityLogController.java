package com.management.student_center.controller;

import com.management.student_center.dto.ActivityLogResponse;
import com.management.student_center.service.ActivityLogService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/activity-logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(
            ActivityLogService activityLogService
    ) {
        this.activityLogService = activityLogService;
    }

    // =========================
    // ADMIN FEED
    // =========================

    @GetMapping("/recent/admin")
    public List<ActivityLogResponse> getAdminActivities(
            @RequestParam(defaultValue = "10") int limit
    ) {

        return activityLogService.getAdminActivities(limit);
    }

    // =========================
    // TEACHER FEED
    // =========================

    @GetMapping("/recent/teacher/{userId}")
    public List<ActivityLogResponse> getTeacherActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit
    ) {

        return activityLogService.getTeacherActivities(
                userId,
                limit
        );
    }
}