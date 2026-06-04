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

    @GetMapping
    public List<Notification> list(Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        return notificationRepository.findByUserId(currentUser.getId());
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

    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        notification.setCreatedAt(OffsetDateTime.now());
        notification.setRead(false);
        return ResponseEntity.ok(notificationRepository.save(notification));
    }
}