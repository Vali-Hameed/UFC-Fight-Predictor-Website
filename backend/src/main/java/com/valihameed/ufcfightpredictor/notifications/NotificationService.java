package com.valihameed.ufcfightpredictor.notifications;

import com.valihameed.ufcfightpredictor.email.EmailSender;
import com.valihameed.ufcfightpredictor.models.Notification;
import com.valihameed.ufcfightpredictor.repository.NotificationRepository;
import com.valihameed.ufcfightpredictor.repository.userRepository;
import com.valihameed.ufcfightpredictor.users.user;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final userRepository userRepository;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public Notification createNotification(Notification notification) {
        notification.setCreatedAt(OffsetDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        sendEmailNotificationAsync(saved);

        return saved;
    }

    private void sendEmailNotificationAsync(Notification notification) {
        CompletableFuture.runAsync(() -> {
            try {
                Optional<user> userOpt = userRepository.findById(notification.getUserId());
                if (userOpt.isPresent()) {
                    user u = userOpt.get();
                    
                    if (u.isOptOutEmailNotifications()) {
                        log.debug("User {} has opted out of email notifications", u.getId());
                        return;
                    }
                    
                    if (u.getEmail() != null) {
                        String emailHtml = buildNotificationEmail(u.getFirstName(), notification);
                        emailSender.sendEmail(u.getEmail(), emailHtml, "New Notification: FightPicks");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send notification email for notification id: {}", notification.getId(), e);
            }
        });
    }

    private String buildNotificationEmail(String name, Notification notification) {
        String linkSection = "";
        if (notification.getLink() != null && !notification.getLink().isEmpty()) {
            String fullLink = notification.getLink();
            if (fullLink.startsWith("/")) {
                fullLink = frontendUrl + fullLink;
            }
            linkSection = "<div style=\"margin-top: 30px; text-align: center;\">\n" +
                          "  <a href=\"" + fullLink + "\" style=\"background-color: #dc2626; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 4px; font-weight: bold; font-size: 16px; display: inline-block;\">View Details</a>\n" +
                          "</div>\n";
        }

        return "<div style=\"font-family: Arial, sans-serif; background-color: #09090b; color: #ffffff; padding: 40px 20px; text-align: center;\">\n" +
               "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #121212; border: 1px solid #333; border-radius: 8px; overflow: hidden; text-align: left;\">\n" +
               "    <div style=\"background-color: #dc2626; padding: 20px; text-align: center;\">\n" +
               "      <h1 style=\"color: #ffffff; margin: 0; font-size: 24px;\">FightPicks</h1>\n" +
               "    </div>\n" +
               "    <div style=\"padding: 30px 20px;\">\n" +
               "      <h2 style=\"color: #d4af37; margin-top: 0; text-align: center;\">New Notification</h2>\n" +
               "      <p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 20px;\">Hi " + (name != null ? name : "User") + ",</p>\n" +
               "      <p style=\"font-size: 16px; line-height: 1.5; margin-bottom: 25px;\">" + escapeHtml(notification.getMessage()) + "</p>\n" +
               linkSection +
               "      <p style=\"font-size: 14px; color: #888; margin-top: 40px; text-align: center;\">You are receiving this email because you have notifications enabled.<br>You can opt out in your profile settings.</p>\n" +
               "    </div>\n" +
               "  </div>\n" +
               "</div>";
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#039;")
                   .replace("\n", "<br>");
    }
}
