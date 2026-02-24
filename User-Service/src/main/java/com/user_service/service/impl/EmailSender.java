package com.user_service.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;
    private final FailedEmailStore failedEmailStore;

    @Async("emailTaskExecutor")
    @Retryable(
            retryFor = {MessagingException.class, RuntimeException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendHtmlEmail(String from, String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.warn("Email send attempt failed to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }

    @Recover
    public void recoverFailedEmail(RuntimeException e, String from, String to, String subject, String htmlBody) {
        log.error("All retry attempts exhausted for email to {}: subject='{}'. Storing for manual review.", to, subject);
        failedEmailStore.store(from, to, subject, htmlBody, e.getMessage());
    }
}