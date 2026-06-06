package com.valihameed.ufcfightpredictor.admin;

import com.valihameed.ufcfightpredictor.models.Notification;
import com.valihameed.ufcfightpredictor.repository.NotificationRepository;
import com.valihameed.ufcfightpredictor.notifications.NotificationService;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@AllArgsConstructor
public class AdminModerationController {
    private final userRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @PostMapping("/{id}/warn")
    public ResponseEntity<?> warnUser(@PathVariable Long id, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        if (currentUser.getId().equals(id)) return ResponseEntity.badRequest().body("You cannot moderate yourself.");

        Optional<user> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) return ResponseEntity.notFound().build();
        
        user u = optionalUser.get();
        if ("ROLE_ADMIN".equals(u.getRole().getName())) {
            return ResponseEntity.badRequest().body("You cannot moderate another admin.");
        }
        u.setWarningCount(u.getWarningCount() + 1);
        userRepository.save(u);

        Notification n = Notification.builder()
            .userId(u.getId())
            .type("WARNING")
            .message("You have received an official warning from a moderator. Please adhere to the community guidelines.")
            .build();
        notificationService.createNotification(n);

        return ResponseEntity.ok().body("User warned. Total warnings: " + u.getWarningCount());
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id, @RequestParam(required = false) Integer durationDays, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        if (currentUser.getId().equals(id)) return ResponseEntity.badRequest().body("You cannot moderate yourself.");

        Optional<user> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) return ResponseEntity.notFound().build();
        
        user u = optionalUser.get();
        if ("ROLE_ADMIN".equals(u.getRole().getName())) {
            return ResponseEntity.badRequest().body("You cannot moderate another admin.");
        }
        if (durationDays == null) {
            // Permanent ban (e.g. year 9999)
            u.setBannedFromForumUntil(OffsetDateTime.parse("9999-12-31T23:59:59Z"));
        } else {
            OffsetDateTime currentBanEnd = u.getBannedFromForumUntil();
            if (currentBanEnd != null && currentBanEnd.isAfter(OffsetDateTime.now()) && currentBanEnd.getYear() < 9999) {
                u.setBannedFromForumUntil(currentBanEnd.plusDays(durationDays));
            } else if (currentBanEnd == null || currentBanEnd.isBefore(OffsetDateTime.now())) {
                u.setBannedFromForumUntil(OffsetDateTime.now().plusDays(durationDays));
            }
        }
        userRepository.save(u);

        String banMessage;
        String formattedDate = null;
        if (u.getBannedFromForumUntil() != null && u.getBannedFromForumUntil().getYear() >= 9999) {
            banMessage = "You have been permanently banned from the forum. Contact support if you believe this is an error.";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z");
            formattedDate = u.getBannedFromForumUntil().atZoneSameInstant(java.time.ZoneId.of("UTC")).format(formatter);
            banMessage = "You have been banned from the forum until " + formattedDate + ". Contact support if you believe this is an error.";
        }

        Notification n = Notification.builder()
            .userId(u.getId())
            .type("BAN")
            .message(banMessage)
            .build();
        notificationService.createNotification(n);

        return ResponseEntity.ok().body(u.getBannedFromForumUntil() != null && u.getBannedFromForumUntil().getYear() >= 9999 ? "User permanently banned." : "User banned until " + formattedDate);
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long id, Authentication authentication) {
        user currentUser = (user) authentication.getPrincipal();
        if (currentUser.getId().equals(id)) return ResponseEntity.badRequest().body("You cannot moderate yourself.");

        Optional<user> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) return ResponseEntity.notFound().build();
        
        user u = optionalUser.get();
        u.setBannedFromForumUntil(null);
        userRepository.save(u);

        Notification n = Notification.builder()
            .userId(u.getId())
            .type("UNBAN")
            .message("Your forum ban has been lifted. Welcome back.")
            .build();
        notificationService.createNotification(n);

        return ResponseEntity.ok().body("User unbanned.");
    }
}
