package com.management.student_center.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người thực hiện hành động
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Loại thao tác: create, update, delete, approve...
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    // Loại đối tượng bị tác động: student, teacher, announcement, schedule, course...
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    // ID của đối tượng trong bảng gốc
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // Chuỗi tóm tắt hành động, dùng hiển thị feed nhanh
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Thông tin bổ sung, có thể là JSON dạng String
    @Column(name = "meta", columnDefinition = "TEXT")
    private String meta;

    // Thời điểm hành động xảy ra
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Constructor mặc định
    public ActivityLog() {}

    // Constructor đầy đủ
    public ActivityLog(User user, String actionType, String targetType, Long targetId, String description, String meta) {
        this.user = user;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
        this.meta = meta;
    }

    // Getter và Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}