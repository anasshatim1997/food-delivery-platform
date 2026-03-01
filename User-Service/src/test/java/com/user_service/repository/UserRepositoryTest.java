package com.user_service.repository;

import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.testcontainers.enabled=false"
})
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private User buildUser(String email) {
        return User.builder()
                .email(email)
                .password("SecurePass1!") // Added special character
                .phone(null)
                .status(Status.ACTIVE)
                .role(Role.USER)
                .isVerified(false)
                .profileCompleted(false)
                .build();
    }

    private User buildFullUser(String email, String phone, String verificationCode,
                               String oauthProvider, String oauthProviderId,
                               String passwordResetToken) {
        return User.builder()
                .email(email)
                .password("SecurePass1!") // Added special character
                .phone(phone)
                .status(Status.ACTIVE)
                .role(Role.USER)
                .isVerified(false)
                .profileCompleted(false)
                .verificationCode(verificationCode)
                .oauthProvider(oauthProvider)
                .oauthProviderId(oauthProviderId)
                .passwordResetToken(passwordResetToken)
                .build();
    }

    // =========================================================
    // findByEmail
    // =========================================================

    @Test
    @DisplayName("findByEmail - returns user when email exists")
    void findByEmail_exists() {
        userRepository.save(buildUser("test@mail.com"));

        Optional<User> result = userRepository.findByEmail("test@mail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@mail.com");
    }

    @Test
    @DisplayName("findByEmail - returns empty when email does not exist")
    void findByEmail_notFound() {
        Optional<User> result = userRepository.findByEmail("ghost@mail.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByEmail - is case sensitive")
    void findByEmail_caseSensitive() {
        userRepository.save(buildUser("lower@mail.com"));

        Optional<User> result = userRepository.findByEmail("LOWER@mail.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByEmail - returns correct user among multiple")
    void findByEmail_returnsCorrectAmongMany() {
        userRepository.save(buildUser("a@mail.com"));
        userRepository.save(buildUser("b@mail.com"));
        userRepository.save(buildUser("c@mail.com"));

        Optional<User> result = userRepository.findByEmail("b@mail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("b@mail.com");
    }

    @Test
    @DisplayName("findByEmail - returns empty on blank string")
    void findByEmail_blankString() {
        Optional<User> result = userRepository.findByEmail("");

        assertThat(result).isEmpty();
    }

    // =========================================================
    // existsByEmail
    // =========================================================

    @Test
    @DisplayName("existsByEmail - returns true when email exists")
    void existsByEmail_true() {
        userRepository.save(buildUser("exists@mail.com"));

        assertThat(userRepository.existsByEmail("exists@mail.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - returns false when email does not exist")
    void existsByEmail_false() {
        assertThat(userRepository.existsByEmail("missing@mail.com")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail - returns false after user is deleted")
    void existsByEmail_falseAfterDelete() {
        User user = userRepository.save(buildUser("delete@mail.com"));
        userRepository.delete(user);

        assertThat(userRepository.existsByEmail("delete@mail.com")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail - is case sensitive")
    void existsByEmail_caseSensitive() {
        userRepository.save(buildUser("case@mail.com"));

        assertThat(userRepository.existsByEmail("CASE@mail.com")).isFalse();
    }

    // =========================================================
    // existsByPhone
    // =========================================================

    @Test
    @DisplayName("existsByPhone - returns true when phone exists")
    void existsByPhone_true() {
        userRepository.save(buildFullUser("phone@mail.com", "+212612345678", null, null, null, null));

        assertThat(userRepository.existsByPhone("+212612345678")).isTrue();
    }

    @Test
    @DisplayName("existsByPhone - returns false when phone does not exist")
    void existsByPhone_false() {
        assertThat(userRepository.existsByPhone("+212699999999")).isFalse();
    }

    @Test
    @DisplayName("existsByPhone - returns false when phone is null stored")
    void existsByPhone_nullStored() {
        userRepository.save(buildFullUser("nophone@mail.com", null, null, null, null, null));

        assertThat(userRepository.existsByPhone(null)).isFalse();
    }

    @Test
    @DisplayName("existsByPhone - returns false after user deleted")
    void existsByPhone_falseAfterDelete() {
        User user = userRepository.save(buildFullUser("del@mail.com", "+212611111111", null, null, null, null));
        userRepository.delete(user);

        assertThat(userRepository.existsByPhone("+212611111111")).isFalse();
    }

    @Test
    @DisplayName("existsByPhone - two users different phones both found")
    void existsByPhone_multipleUsers() {
        userRepository.save(buildFullUser("u1@mail.com", "+212611111111", null, null, null, null));
        userRepository.save(buildFullUser("u2@mail.com", "+212622222222", null, null, null, null));

        assertThat(userRepository.existsByPhone("+212611111111")).isTrue();
        assertThat(userRepository.existsByPhone("+212622222222")).isTrue();
    }

    // =========================================================
    // findByVerificationCode
    // =========================================================

    @Test
    @DisplayName("findByVerificationCode - returns user when code exists")
    void findByVerificationCode_found() {
        userRepository.save(buildFullUser("verify@mail.com", null, "CODE123", null, null, null));

        Optional<User> result = userRepository.findByVerificationCode("CODE123");

        assertThat(result).isPresent();
        assertThat(result.get().getVerificationCode()).isEqualTo("CODE123");
    }

    @Test
    @DisplayName("findByVerificationCode - returns empty when code does not exist")
    void findByVerificationCode_notFound() {
        Optional<User> result = userRepository.findByVerificationCode("INVALID");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByVerificationCode - returns empty after code cleared")
    void findByVerificationCode_emptyAfterCodeCleared() {
        User user = userRepository.save(buildFullUser("clear@mail.com", null, "CLEAR123", null, null, null));
        user.setVerificationCode(null);
        userRepository.save(user);

        Optional<User> result = userRepository.findByVerificationCode("CLEAR123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByVerificationCode - is case sensitive")
    void findByVerificationCode_caseSensitive() {
        userRepository.save(buildFullUser("caseV@mail.com", null, "abc123", null, null, null));

        Optional<User> result = userRepository.findByVerificationCode("ABC123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByVerificationCode - returns correct user among many")
    void findByVerificationCode_correctAmongMany() {
        userRepository.save(buildFullUser("v1@mail.com", null, "CODE_A", null, null, null));
        userRepository.save(buildFullUser("v2@mail.com", null, "CODE_B", null, null, null));

        Optional<User> result = userRepository.findByVerificationCode("CODE_B");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("v2@mail.com");
    }

    // =========================================================
    // findByOauthProviderAndOauthProviderId
    // =========================================================

    @Test
    @DisplayName("findByOauthProviderAndOauthProviderId - returns user when both match")
    void findByOauth_bothMatch() {
        userRepository.save(buildFullUser("oauth@mail.com", null, null, "google", "google-123", null));

        Optional<User> result = userRepository.findByOauthProviderAndOauthProviderId("google", "google-123");

        assertThat(result).isPresent();
        assertThat(result.get().getOauthProvider()).isEqualTo("google");
        assertThat(result.get().getOauthProviderId()).isEqualTo("google-123");
    }

    @Test
    @DisplayName("findByOauthProviderAndOauthProviderId - returns empty when provider mismatches")
    void findByOauth_providerMismatch() {
        userRepository.save(buildFullUser("oauth2@mail.com", null, null, "google", "id-123", null));

        Optional<User> result = userRepository.findByOauthProviderAndOauthProviderId("facebook", "id-123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOauthProviderAndOauthProviderId - returns empty when providerId mismatches")
    void findByOauth_providerIdMismatch() {
        userRepository.save(buildFullUser("oauth3@mail.com", null, null, "google", "correct-id", null));

        Optional<User> result = userRepository.findByOauthProviderAndOauthProviderId("google", "wrong-id");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOauthProviderAndOauthProviderId - returns empty when both null stored")
    void findByOauth_bothNullStored() {
        userRepository.save(buildFullUser("nooauth@mail.com", null, null, null, null, null));

        Optional<User> result = userRepository.findByOauthProviderAndOauthProviderId("google", "some-id");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOauthProviderAndOauthProviderId - distinguishes between different providers same id")
    void findByOauth_sameIdDifferentProviders() {
        userRepository.save(buildFullUser("g@mail.com", null, null, "google", "shared-id", null));
        userRepository.save(buildFullUser("f@mail.com", null, null, "facebook", "shared-id", null));

        Optional<User> google = userRepository.findByOauthProviderAndOauthProviderId("google", "shared-id");
        Optional<User> facebook = userRepository.findByOauthProviderAndOauthProviderId("facebook", "shared-id");

        assertThat(google).isPresent();
        assertThat(google.get().getEmail()).isEqualTo("g@mail.com");
        assertThat(facebook).isPresent();
        assertThat(facebook.get().getEmail()).isEqualTo("f@mail.com");
    }

    // =========================================================
    // findByPasswordResetToken
    // =========================================================

    @Test
    @DisplayName("findByPasswordResetToken - returns user when token exists")
    void findByPasswordResetToken_found() {
        userRepository.save(buildFullUser("reset@mail.com", null, null, null, null, "RESET123"));

        Optional<User> result = userRepository.findByPasswordResetToken("RESET123");

        assertThat(result).isPresent();
        assertThat(result.get().getPasswordResetToken()).isEqualTo("RESET123");
    }

    @Test
    @DisplayName("findByPasswordResetToken - returns empty when token not found")
    void findByPasswordResetToken_notFound() {
        Optional<User> result = userRepository.findByPasswordResetToken("INVALID");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPasswordResetToken - returns empty after token cleared")
    void findByPasswordResetToken_emptyAfterCleared() {
        User user = userRepository.save(buildFullUser("clearReset@mail.com", null, null, null, null, "TOKEN_X"));
        user.setPasswordResetToken(null);
        userRepository.save(user);

        Optional<User> result = userRepository.findByPasswordResetToken("TOKEN_X");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPasswordResetToken - is case sensitive")
    void findByPasswordResetToken_caseSensitive() {
        userRepository.save(buildFullUser("caseReset@mail.com", null, null, null, null, "token123"));

        Optional<User> result = userRepository.findByPasswordResetToken("TOKEN123");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPasswordResetToken - returns correct user among many")
    void findByPasswordResetToken_correctAmongMany() {
        userRepository.save(buildFullUser("r1@mail.com", null, null, null, null, "TOKEN_A"));
        userRepository.save(buildFullUser("r2@mail.com", null, null, null, null, "TOKEN_B"));

        Optional<User> result = userRepository.findByPasswordResetToken("TOKEN_A");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("r1@mail.com");
    }

    // =========================================================
    // Uniqueness constraints
    // =========================================================

    @Test
    @DisplayName("save - throws on duplicate email")
    void save_duplicateEmail_throws() {
        userRepository.save(buildUser("dup@mail.com"));

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(buildUser("dup@mail.com"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("save - throws on duplicate phone")
    void save_duplicatePhone_throws() {
        userRepository.save(buildFullUser("p1@mail.com", "+212611111111", null, null, null, null));

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(buildFullUser("p2@mail.com", "+212611111111", null, null, null, null));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("save - defaults applied correctly")
    void save_defaultsApplied() {
        User user = userRepository.save(buildUser("defaults@mail.com"));

        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getIsVerified()).isFalse();
        assertThat(user.isProfileCompleted()).isFalse();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("save - can store multiple users with null phone")
    void save_multipleNullPhones() {
        userRepository.save(buildFullUser("np1@mail.com", null, null, null, null, null));
        userRepository.save(buildFullUser("np2@mail.com", null, null, null, null, null));

        List<User> all = userRepository.findAll();
        assertThat(all).hasSize(2);
    }
}