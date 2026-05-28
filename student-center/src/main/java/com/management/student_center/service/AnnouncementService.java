package com.management.student_center.service;

import com.management.student_center.dto.CreateAnnouncementRequest;
import com.management.student_center.dto.UpdateAnnouncementRequest;
import com.management.student_center.dto.AnnouncementResponse;
import com.management.student_center.entity.Announcement;
import com.management.student_center.entity.User;
import com.management.student_center.repository.AnnouncementRepository;
import com.management.student_center.repository.AnnouncementViewRepository;
import com.management.student_center.repository.AnnouncementLikeRepository;
import com.management.student_center.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.management.student_center.service.ActivityLogService;

import com.management.student_center.enums.ActivityActionType;

import com.management.student_center.enums.ActivityTargetType;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnnouncementService {

	private final AnnouncementRepository announcementRepo;
	private final UserRepository userRepo;
	private final AnnouncementLikeRepository likeRepo;
	private final AnnouncementViewRepository viewRepo;
	private final ActivityLogService activityLogService;

	private final String uploadDir = System.getProperty("user.dir") + "/uploads/announcements/";

	public AnnouncementService(
	        AnnouncementRepository announcementRepo,
	        UserRepository userRepo,
	        AnnouncementLikeRepository likeRepo,
	        AnnouncementViewRepository viewRepo,
	        ActivityLogService activityLogService
	) {

	    this.announcementRepo = announcementRepo;

	    this.userRepo = userRepo;

	    this.likeRepo = likeRepo;

	    this.viewRepo = viewRepo;

	    this.activityLogService = activityLogService;

	    File dir = new File(uploadDir);

	    if (!dir.exists()) {
	        dir.mkdirs();
	    }
	}

	// CREATE ANNOUNCEMENT + MULTIPLE ATTACHMENTS
	public Announcement createAnnouncement(CreateAnnouncementRequest req, MultipartFile imageFile,
			List<MultipartFile> attachmentFiles) throws IOException {

		User admin = userRepo.findById(req.getAdminId()).orElseThrow(() -> new RuntimeException("Admin not found"));

		Announcement a = new Announcement();
		a.setAdmin(admin);
		a.setTitle(req.getTitle());
		a.setContent(req.getContent());

		if (imageFile != null && !imageFile.isEmpty()) {
			String fileName = saveFile(imageFile);
			a.setImageURL("/uploads/announcements/" + fileName);
		} else if (req.getImageURL() != null) {
			a.setImageURL(req.getImageURL());
		}

		if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
			List<String> attachmentURLs = attachmentFiles.stream().map(file -> {
				try {
					String fileName = saveFile(file);
					return "/uploads/announcements/" + fileName;
				} catch (IOException e) {
					throw new RuntimeException("Attachment upload failed");
				}
			}).toList();

			a.setAttachments(attachmentURLs);
		}

		String status = req.getStatus() != null ? req.getStatus().trim().toLowerCase() : "active";
		a.setPinned(req.getPinned() != null ? req.getPinned() : false);

		if (!status.equals("active") && !status.equals("inactive") && !status.equals("draft")) {
			throw new RuntimeException("Invalid status. Valid values: active, inactive, draft");
		}
		a.setStatus(status);

		Announcement saved = announcementRepo.save(a);

		logAnnouncement(
		        admin,
		        ActivityActionType.CREATE,
		        saved,
		        " đã tạo thông báo: " + saved.getTitle()
		);

		return saved;
	}

	// UPDATE ANNOUNCEMENT + MULTIPLE ATTACHMENTS
	public Announcement updateAnnouncement(Long id, UpdateAnnouncementRequest req, MultipartFile imageFile,
			List<MultipartFile> attachmentFiles) throws IOException {

		Announcement a = announcementRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Announcement not found"));

		if (req.getTitle() != null)
			a.setTitle(req.getTitle());
		if (req.getContent() != null)
			a.setContent(req.getContent());

		// ----- STATUS -----
		if (req.getStatus() != null) {
			String newStatus = req.getStatus().trim().toLowerCase();
			if (!newStatus.equals("active") && !newStatus.equals("inactive") && !newStatus.equals("draft")) {
				throw new RuntimeException("Invalid status");
			}
			a.setStatus(newStatus);
		}

		if (req.getPinned() != null) {
			a.setPinned(req.getPinned());
		}

		// ----- IMAGE -----
		if (Boolean.TRUE.equals(req.getClearImage())) {
			// Xóa file vật lý
			deletePhysicalFile(a.getImageURL());
			a.setImageURL(null);
		} else if (imageFile != null && !imageFile.isEmpty()) {
			deletePhysicalFile(a.getImageURL());
			String newName = saveFile(imageFile);
			a.setImageURL("/uploads/announcements/" + newName);
		}

		// ----- ATTACHMENTS -----
		if (Boolean.TRUE.equals(req.getClearAttachments())) {
			if (a.getAttachments() != null) {
				for (String url : a.getAttachments())
					deletePhysicalFile(url);
			}
			a.setAttachments(null);
		} else if (attachmentFiles != null && !attachmentFiles.isEmpty()) {

			// Giữ file cũ nếu có
			List<String> updatedAttachments = a.getAttachments() != null ? new ArrayList<>(a.getAttachments())
					: new ArrayList<>();

			for (MultipartFile file : attachmentFiles) {
				String name = saveFile(file);
				updatedAttachments.add("/uploads/announcements/" + name);
			}

			a.setAttachments(updatedAttachments);
		}

		Announcement updated = announcementRepo.save(a);

		logAnnouncement(
		        a.getAdmin(),
		        ActivityActionType.UPDATE,
		        updated,
		        " đã cập nhật thông báo: " + updated.getTitle()
		);

		return updated;
	}

	private String saveFile(MultipartFile file) throws IOException {
		String original = file.getOriginalFilename();
		String ext = original != null && original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";

		String fileName = System.currentTimeMillis() + "-" + (int) (Math.random() * 1e9) + ext;
		Path path = Paths.get(uploadDir + fileName);

		Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		return fileName;
	}

	private void deletePhysicalFile(String url) {
		if (url == null || url.startsWith("http"))
			return;

		try {
			String filename = Paths.get(url).getFileName().toString();
			Path filePath = Paths.get(uploadDir, filename);

			if (Files.exists(filePath))
				Files.delete(filePath);

		} catch (Exception e) {
			System.err.println("Không thể xóa file: " + e.getMessage());
		}
	}

	public Page<Announcement> getAnnouncements(int page, int size) {
		Pageable pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("updatedAt")));
		return announcementRepo.findAll(pageable);

	}

	public Announcement getById(Long id) {
		return announcementRepo.findById(id).orElseThrow(() -> new RuntimeException("Announcement not found"));
	}

	public void deleteAnnouncement(Long id) {
		Announcement a = getById(id);

		deletePhysicalFile(a.getImageURL());

		if (a.getAttachments() != null) {
			for (String url : a.getAttachments()) {
				deletePhysicalFile(url);
			}
		}

		logAnnouncement(
		        a.getAdmin(),
		        ActivityActionType.DELETE,
		        a,
		        " đã xóa thông báo: " + a.getTitle()
		);
		announcementRepo.deleteById(id);
	}

	public List<Announcement> getAll() {
		return announcementRepo.findAll();
	}
	
	private void logAnnouncement(
	        User user,
	        ActivityActionType actionType,
	        Announcement announcement,
	        String description
	) {

	    String meta = """
	            {
	                "title": "%s",
	                "status": "%s",
	                "pinned": %s
	            }
	            """.formatted(
	            announcement.getTitle(),
	            announcement.getStatus(),
	            announcement.getPinned()
	    );

	    activityLogService.log(
	            user,
	            actionType,
	            ActivityTargetType.ANNOUNCEMENT,
	            announcement.getId(),
	            description,
	            meta
	    );
	}
}

