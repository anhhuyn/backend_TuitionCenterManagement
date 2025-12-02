package com.management.student_center.repository;

import com.management.student_center.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
}
