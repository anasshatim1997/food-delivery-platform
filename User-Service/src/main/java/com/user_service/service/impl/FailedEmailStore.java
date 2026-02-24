package com.user_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class FailedEmailStore {

    private final ConcurrentLinkedQueue<FailedEmail> queue = new ConcurrentLinkedQueue<>();

    public void store(String from, String to, String subject, String htmlBody, String reason) {
        FailedEmail failedEmail = FailedEmail.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .htmlBody(htmlBody)
                .reason(reason)
                .failedAt(LocalDateTime.now())
                .build();
        queue.offer(failedEmail);
        log.error("FAILED_EMAIL stored | to={} subject='{}' reason={}", to, subject, reason);
    }

    public List<FailedEmail> drainAll() {
        List<FailedEmail> drained = new ArrayList<>();
        FailedEmail email;
        while ((email = queue.poll()) != null) {
            drained.add(email);
        }
        return Collections.unmodifiableList(drained);
    }

    public int pendingCount() {
        return queue.size();
    }

    @lombok.Builder
    @lombok.Getter
    public static class FailedEmail {
        private final String from;
        private final String to;
        private final String subject;
        private final String htmlBody;
        private final String reason;
        private final LocalDateTime failedAt;
    }
}