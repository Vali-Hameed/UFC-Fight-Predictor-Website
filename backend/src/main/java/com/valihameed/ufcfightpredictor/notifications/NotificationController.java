package com.valihameed.ufcfightpredictor.notifications;

import com.valihameed.ufcfightpredictor.models.Notification;
import com.valihameed.ufcfightpredictor.repository.NotificationRepository;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@AllArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> list(Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        return notificationRepository.findByUserIdOrderByReadAscCreatedAtDesc(currentUser.getId());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        long count = notificationRepository.findByUserId(currentUser.getId()).stream()
            .filter(n -> Boolean.FALSE.equals(n.getRead()))
            .count();
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        return notificationRepository.findById(id)
            .filter(notification -> notification.getUserId().equals(currentUser.getId()))
            .map(notification -> {
                notification.setRead(true);
                notificationRepository.save(notification);
                return ResponseEntity.ok(notification);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        List<Notification> notifications = notificationRepository.findByUserId(currentUser.getId());
        for (Notification n : notifications) {
            if (Boolean.FALSE.equals(n.getRead()) || n.getRead() == null) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        return ResponseEntity.ok(notificationService.createNotification(notification));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        return notificationRepository.findById(id)
            .filter(notification -> notification.getUserId().equals(currentUser.getId()))
            .map(notification -> {
                notificationRepository.delete(notification);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllNotifications(Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        List<Notification> notifications = notificationRepository.findByUserId(currentUser.getId());
        notificationRepository.deleteAll(notifications);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<Void> deleteBatchNotifications(@RequestBody List<Long> ids, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        List<Notification> notifications = notificationRepository.findAllById(ids).stream()
            .filter(notification -> notification.getUserId().equals(currentUser.getId()))
            .toList();
        notificationRepository.deleteAll(notifications);
        return ResponseEntity.ok().build();
    }
}