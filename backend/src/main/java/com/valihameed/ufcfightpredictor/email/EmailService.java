package com.valihameed.ufcfightpredictor.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements EmailSender {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    
    private final Resend resend;
    private final String fromAddress;

    public EmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-address}") String fromAddress) {
        this.resend = new Resend(apiKey);
        this.fromAddress = fromAddress;
    }

    @Override
    @Async
    public void sendEmail(String to, String emailContent, String subject) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(this.fromAddress)
                    .to(to)
                    .subject(subject)
                    .html(emailContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            LOGGER.info("Successfully sent email to {} via Resend. ID: {}", to, response.getId());

        } catch (ResendException e) {
            LOGGER.error("Failed to send email via Resend to {}. Error: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Failed to send email", e);
        }
    }
}
