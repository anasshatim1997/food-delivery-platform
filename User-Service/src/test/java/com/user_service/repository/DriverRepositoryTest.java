package com.user_service.repository;

import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VehicleType;
import com.user_service.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        driverRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("findByUserId - returns driver when exists")
    void findByUserId_found() {
        User user = createAndSaveUser("driver@mail.com");
        Driver driver = createAndSaveDriver(user, "John", "Doe");

        Optional<Driver> result = driverRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getFirstName()).isEqualTo("John");
        assertThat(result.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("findByUserId - returns empty when not found")
    void findByUserId_notFound() {
        Optional<Driver> result = driverRepository.findByUserId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserId - returns empty when user has no driver profile")
    void findByUserId_userExistsButNoDriver() {
        User user = createAndSaveUser("customer@mail.com");

        Optional<Driver> result = driverRepository.findByUserId(user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save - persists driver successfully")
    void save_success() {
        User user = createAndSaveUser("new@mail.com");
        Driver driver = buildDriver(user, "Jane", "Smith");

        Driver saved = driverRepository.save(driver);

        assertThat(saved.getUser().getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Smith");
        assertThat(saved.getVehicleType()).isEqualTo(VehicleType.BIKE);
        assertThat(saved.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(saved.getIsAvailable()).isFalse();
    }

    @Test
    @DisplayName("save - updates existing driver")
    void save_update() {
        User user = createAndSaveUser("update@mail.com");
        Driver driver = createAndSaveDriver(user, "Old", "Name");

        driver.setFirstName("New");
        driver.setLastName("Name");
        driver.setIsAvailable(true);
        driver.setVerificationStatus(VerificationStatus.APPROVED);
        driver.setRating(new BigDecimal("4.8"));
        driver.setTotalDeliveries(100);

        Driver updated = driverRepository.save(driver);

        assertThat(updated.getFirstName()).isEqualTo("New");
        assertThat(updated.getLastName()).isEqualTo("Name");
        assertThat(updated.getIsAvailable()).isTrue();
        assertThat(updated.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(updated.getRating()).isEqualByComparingTo("4.8");
        assertThat(updated.getTotalDeliveries()).isEqualTo(100);
    }

    @Test
    @DisplayName("delete - removes driver")
    void delete_success() {
        User user = createAndSaveUser("delete@mail.com");
        Driver driver = createAndSaveDriver(user, "Delete", "Me");

        driverRepository.delete(driver);

        assertThat(driverRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("findById - returns driver when exists")
    void findById_found() {
        User user = createAndSaveUser("findid@mail.com");
        Driver driver = createAndSaveDriver(user, "Find", "Me");

        Optional<Driver> result = driverRepository.findById(driver.getUser().getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(driver.getUser().getId());
    }

    @Test
    @DisplayName("findById - returns empty when not found")
    void findById_notFound() {
        Optional<Driver> result = driverRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save - cascades to user relationship")
    void save_cascadesToUser() {
        User user = createAndSaveUser("cascade@mail.com");
        Driver driver = buildDriver(user, "Cascade", "Test");

        Driver saved = driverRepository.save(driver);

        assertThat(saved.getUser()).isNotNull();
        assertThat(saved.getUser().getEmail()).isEqualTo("cascade@mail.com");
    }

    @Test
    @DisplayName("findByUserId - handles multiple drivers")
    void findByUserId_multipleDrivers() {
        User user1 = createAndSaveUser("driver1@mail.com");
        User user2 = createAndSaveUser("driver2@mail.com");
        Driver driver1 = createAndSaveDriver(user1, "Driver", "One");
        Driver driver2 = createAndSaveDriver(user2, "Driver", "Two");

        Optional<Driver> result1 = driverRepository.findByUserId(user1.getId());
        Optional<Driver> result2 = driverRepository.findByUserId(user2.getId());

        assertThat(result1).isPresent();
        assertThat(result1.get().getUser().getId()).isEqualTo(user1.getId());
        assertThat(result2).isPresent();
        assertThat(result2.get().getUser().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("save - persists profile and license images")
    void save_withImages() {
        User user = createAndSaveUser("images@mail.com");
        Driver driver = buildDriver(user, "Image", "Test");
        driver.setProfileImage("https://example.com/profile.jpg");
        driver.setLicenseImage("https://example.com/license.jpg");

        Driver saved = driverRepository.save(driver);

        assertThat(saved.getProfileImage()).isEqualTo("https://example.com/profile.jpg");
        assertThat(saved.getLicenseImage()).isEqualTo("https://example.com/license.jpg");
    }

    @Test
    @DisplayName("save - persists current location")
    void save_withLocation() {
        User user = createAndSaveUser("location@mail.com");
        Driver driver = createAndSaveDriver(user, "Location", "Test");

        driver.setCurrentLat(new BigDecimal("33.5731"));
        driver.setCurrentLng(new BigDecimal("-7.5898"));
        Driver updated = driverRepository.save(driver);

        assertThat(updated.getCurrentLat()).isEqualByComparingTo("33.5731");
        assertThat(updated.getCurrentLng()).isEqualByComparingTo("-7.5898");
    }

    @Test
    @DisplayName("save - persists verification documents")
    void save_withVerificationDocuments() {
        User user = createAndSaveUser("docs@mail.com");
        Driver driver = buildDriver(user, "Docs", "Test");
        driver.getVerificationDocuments().put("ID_CARD", "https://example.com/id.jpg");
        driver.getVerificationDocuments().put("INSURANCE", "https://example.com/insurance.pdf");

        Driver saved = driverRepository.save(driver);

        assertThat(saved.getVerificationDocuments()).hasSize(2);
        assertThat(saved.getVerificationDocuments().get("ID_CARD")).isEqualTo("https://example.com/id.jpg");
        assertThat(saved.getVerificationDocuments().get("INSURANCE")).isEqualTo("https://example.com/insurance.pdf");
    }

    @Test
    @DisplayName("save - persists all vehicle types")
    void save_allVehicleTypes() {
        User user1 = createAndSaveUser("bike@mail.com");
        User user2 = createAndSaveUser("car@mail.com");
        User user3 = createAndSaveUser("scooter@mail.com");

        Driver bike = buildDriver(user1, "Bike", "Driver");
        bike.setVehicleType(VehicleType.BIKE);
        Driver car = buildDriver(user2, "Car", "Driver");
        car.setVehicleType(VehicleType.CAR);
        Driver scooter = buildDriver(user3, "Scooter", "Driver");
        scooter.setVehicleType(VehicleType.SCOOTER);

        Driver savedBike = driverRepository.save(bike);
        Driver savedCar = driverRepository.save(car);
        Driver savedScooter = driverRepository.save(scooter);

        assertThat(savedBike.getVehicleType()).isEqualTo(VehicleType.BIKE);
        assertThat(savedCar.getVehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(savedScooter.getVehicleType()).isEqualTo(VehicleType.SCOOTER);
    }

    @Test
    @DisplayName("save - persists all verification statuses")
    void save_allVerificationStatuses() {
        User user1 = createAndSaveUser("pending@mail.com");
        User user2 = createAndSaveUser("approved@mail.com");
        User user3 = createAndSaveUser("rejected@mail.com");

        Driver pending = buildDriver(user1, "Pending", "Driver");
        pending.setVerificationStatus(VerificationStatus.PENDING);
        Driver approved = buildDriver(user2, "Approved", "Driver");
        approved.setVerificationStatus(VerificationStatus.APPROVED);
        Driver rejected = buildDriver(user3, "Rejected", "Driver");
        rejected.setVerificationStatus(VerificationStatus.REJECTED);

        Driver savedPending = driverRepository.save(pending);
        Driver savedApproved = driverRepository.save(approved);
        Driver savedRejected = driverRepository.save(rejected);

        assertThat(savedPending.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(savedApproved.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(savedRejected.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("count - returns correct count")
    void count_correctCount() {
        User user1 = createAndSaveUser("count1@mail.com");
        User user2 = createAndSaveUser("count2@mail.com");
        createAndSaveDriver(user1, "One", "Driver");
        createAndSaveDriver(user2, "Two", "Driver");

        long count = driverRepository.count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsById - returns true when exists")
    void existsById_true() {
        User user = createAndSaveUser("exists@mail.com");
        Driver driver = createAndSaveDriver(user, "Exists", "Test");

        boolean exists = driverRepository.existsById(driver.getUser().getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById - returns false when not exists")
    void existsById_false() {
        boolean exists = driverRepository.existsById(UUID.randomUUID());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("save - updates wallet balance")
    void save_walletBalance() {
        User user = createAndSaveUser("wallet@mail.com");
        Driver driver = createAndSaveDriver(user, "Wallet", "Test");

        driver.setWalletBalance(new BigDecimal("250.50"));
        Driver updated = driverRepository.save(driver);

        assertThat(updated.getWalletBalance()).isEqualByComparingTo("250.50");
    }

    private User createAndSaveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.DRIVER);
        user.setStatus(Status.ACTIVE);
        user.setIsVerified(true);
        user.setProfileCompleted(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Driver buildDriver(User user, String firstName, String lastName) {
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFirstName(firstName);
        driver.setLastName(lastName);
        driver.setVehicleType(VehicleType.BIKE);
        driver.setVehicleNumber("ABC123");
        driver.setLicenseNumber("LIC123456");
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setIsAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driver.setVerificationDocuments(new HashMap<>());
        return driver;
    }

    private Driver createAndSaveDriver(User user, String firstName, String lastName) {
        return driverRepository.save(buildDriver(user, firstName, lastName));
    }
}