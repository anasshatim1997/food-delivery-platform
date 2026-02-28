package com.user_service.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@atlaseats.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8001");
    }

    // =========================================================================
    // sendVerificationEmail
    // =========================================================================

    @Nested
    @DisplayName("sendVerificationEmail")
    class SendVerificationEmail {

        @Test
        @DisplayName("sends HTML email with verification link containing the token")
        void sendVerificationEmail_containsVerifyLink() {
            // --- Arrange ---
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            // --- Act ---
            emailService.sendVerificationEmail("user@mail.com", "abc-token-123");

            // --- Assert ---
            verify(emailSender).sendHtmlEmail(
                    eq("noreply@atlaseats.com"),
                    eq("user@mail.com"),
                    eq("Verify Your Email Address"),
                    bodyCaptor.capture()
            );
            assertThat(bodyCaptor.getValue())
                    .contains("abc-token-123")
                    .contains("http://localhost:8001/api/auth/v1/verify-email");
        }
    }

    // =========================================================================
    // sendPasswordResetEmail
    // =========================================================================

    @Nested
    @DisplayName("sendPasswordResetEmail")
    class SendPasswordResetEmail {

        @Test
        @DisplayName("sends HTML email with password reset link containing the token")
        void sendPasswordResetEmail_containsResetLink() {
            // --- Arrange ---
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            // --- Act ---
            emailService.sendPasswordResetEmail("user@mail.com", "reset-token-xyz");

            // --- Assert ---
            verify(emailSender).sendHtmlEmail(
                    eq("noreply@atlaseats.com"),
                    eq("user@mail.com"),
                    eq("Reset Your Password"),
                    bodyCaptor.capture()
            );
            assertThat(bodyCaptor.getValue())
                    .contains("reset-token-xyz")
                    .contains("http://localhost:8001/api/auth/v1/reset-password");
        }
    }

    // =========================================================================
    // sendWelcomeEmail
    // =========================================================================

    @Nested
    @DisplayName("sendWelcomeEmail")
    class SendWelcomeEmail {

        @Test
        @DisplayName("sends welcome email with the user's first name")
        void sendWelcomeEmail_withFirstName() {
            // --- Arrange ---
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            // --- Act ---
            emailService.sendWelcomeEmail("user@mail.com", "Alice");

            // --- Assert ---
            verify(emailSender).sendHtmlEmail(
                    eq("noreply@atlaseats.com"),
                    eq("user@mail.com"),
                    eq("Welcome!"),
                    bodyCaptor.capture()
            );
            assertThat(bodyCaptor.getValue()).contains("Alice");
        }

        @Test
        @DisplayName("falls back to 'there' when first name is null")
        void sendWelcomeEmail_nullFirstName_fallsBackToThere() {
            // --- Arrange ---
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            // --- Act ---
            emailService.sendWelcomeEmail("user@mail.com", null);

            // --- Assert ---
            verify(emailSender).sendHtmlEmail(any(), any(), any(), bodyCaptor.capture());
            assertThat(bodyCaptor.getValue()).contains("there");
        }

        @Test
        @DisplayName("falls back to 'there' when first name is blank")
        void sendWelcomeEmail_blankFirstName_fallsBackToThere() {
            // --- Arrange ---
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            // --- Act ---
            emailService.sendWelcomeEmail("user@mail.com", "   ");

            // --- Assert ---
            verify(emailSender).sendHtmlEmail(any(), any(), any(), bodyCaptor.capture());
            assertThat(bodyCaptor.getValue()).contains("there");
        }
    }

    // =========================================================================
    // sendPasswordChangedNotification
    // =========================================================================

    @Nested
    @DisplayName("sendPasswordChangedNotification")
    class SendPasswordChangedNotification {

        @Test
        @DisplayName("sends password changed notification to the correct recipient")
        void sendPasswordChangedNotification_success() {
            // --- Act ---
            emailService.sendPasswordChangedNotification("user@mail.com");

            // --- Assert ---
            verify(emailSender).sendHtmlEmail(
                    eq("noreply@atlaseats.com"),
                    eq("user@mail.com"),
                    eq("Your Password Was Changed"),
                    any()
            );
        }
    }

    // helper for eq() with non-Mockito import conflict
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}