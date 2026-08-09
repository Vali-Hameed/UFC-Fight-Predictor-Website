package com.valihameed.ufcfightpredictor.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements EmailSender {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final Resend resend;
    private final String apiKey;
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${resend.api-key:re_default1234567890}") String apiKey,
            @Value("${resend.from-address:noreply@fightpicks.net}") String fromAddress) {
        this.mailSender = mailSender;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        if (isRealResendKey(apiKey)) {
            this.resend = new Resend(apiKey);
        } else {
            this.resend = null;
        }
    }

    private boolean isRealResendKey(String key) {
        return key != null && !key.isBlank() && !key.startsWith("re_default") && !key.contains("change-me");
    }

    @Override
    @Async
    public void sendEmail(String to, String emailContent, String subject) {
        String formattedFrom = this.fromAddress.contains("<") ? this.fromAddress : "FightPicks <" + this.fromAddress + ">";

        // Attempt via Resend if real key is present
        if (this.resend != null) {
            try {
                CreateEmailOptions params = CreateEmailOptions.builder()
                        .from(formattedFrom)
                        .to(to)
                        .subject(subject)
                        .html(emailContent)
                        .build();

                CreateEmailResponse response = resend.emails().send(params);
                LOGGER.info("Successfully sent email to {} via Resend. ID: {}", to, response.getId());
                return;
            } catch (Exception e) {
                LOGGER.warn("Failed to send email via Resend to {}: {}. Falling back to MailHog/Local SMTP...", to, e.getMessage());
            }
        }

        // Fallback or Local Dev: Send via MailHog / JavaMailSender
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(formattedFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailContent, true);

            mailSender.send(message);
            LOGGER.info("Successfully sent email to {} via MailHog/SMTP.", to);
        } catch (Exception e) {
            LOGGER.error("Failed to send email to {} via MailHog/SMTP: {}", to, e.getMessage(), e);
        }

        // Always print log entry for developer visibility on local
        LOGGER.info("\n--- [LOCAL EMAIL LOG] ---\nTo: {}\nSubject: {}\nContent:\n{}\n-------------------------", to, subject, emailContent);
    }
}
