package com.user_service.service.impl;

import com.user_service.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final EmailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8001}")
    private String baseUrl;

    @Override
    public void sendVerificationEmail(String to, String verificationCode) {
        String subject = "Verify Your Email Address";
        String verifyUrl = baseUrl + "/api/auth/v1/verify-email?token=" + verificationCode;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
                  <h2 style="color:#2563eb;">Verify Your Email</h2>
                  <p>Thank you for registering. Please click the button below to verify your email address.</p>
                  <a href="%s" style="display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;margin:16px 0;">
                    Verify Email
                  </a>
                  <p style="color:#6b7280;font-size:14px;">This link expires in 24 hours. If you didn't create an account, you can safely ignore this email.</p>
                  <p style="color:#6b7280;font-size:12px;">Or copy this link: %s</p>
                </div>
                """.formatted(verifyUrl, verifyUrl);
        emailSender.sendHtmlEmail(fromEmail, to, subject, html);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "Reset Your Password";
        String resetUrl = baseUrl + "/api/auth/v1/reset-password?token=" + resetToken;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
                  <h2 style="color:#dc2626;">Password Reset Request</h2>
                  <p>We received a request to reset your password. Click the button below to proceed.</p>
                  <a href="%s" style="display:inline-block;padding:12px 24px;background:#dc2626;color:#fff;text-decoration:none;border-radius:6px;margin:16px 0;">
                    Reset Password
                  </a>
                  <p style="color:#6b7280;font-size:14px;">This link expires in 1 hour. If you didn't request a password reset, please ignore this email.</p>
                  <p style="color:#6b7280;font-size:12px;">Or copy this link: %s</p>
                </div>
                """.formatted(resetUrl, resetUrl);
        emailSender.sendHtmlEmail(fromEmail, to, subject, html);
    }

    @Override
    public void sendWelcomeEmail(String to, String firstName) {
        String subject = "Welcome!";
        String displayName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
                  <h2 style="color:#16a34a;">Welcome, %s!</h2>
                  <p>Your account has been successfully created. We're excited to have you on board.</p>
                  <p style="color:#6b7280;font-size:14px;">If you have any questions, feel free to reach out to our support team.</p>
                </div>
                """.formatted(displayName);
        emailSender.sendHtmlEmail(fromEmail, to, subject, html);
    }

    @Override
    public void sendPasswordChangedNotification(String to) {
        String subject = "Your Password Was Changed";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
                  <h2 style="color:#d97706;">Password Changed</h2>
                  <p>Your password has been successfully updated.</p>
                  <p style="color:#6b7280;font-size:14px;">If you did not make this change, please contact support immediately.</p>
                </div>
                """;
        emailSender.sendHtmlEmail(fromEmail, to, subject, html);
    }
}