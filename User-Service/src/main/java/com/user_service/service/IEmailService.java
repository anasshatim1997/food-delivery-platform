package com.user_service.service;

public interface IEmailService {
    void sendVerificationEmail(String to, String verificationCode);
    void sendPasswordResetEmail(String to, String resetToken);
    void sendWelcomeEmail(String to, String firstName);
    void sendPasswordChangedNotification(String to);
}