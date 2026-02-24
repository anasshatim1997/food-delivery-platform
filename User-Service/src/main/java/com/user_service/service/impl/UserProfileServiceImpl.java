package com.user_service.service.impl;

import com.user_service.dto.request.CompleteCustomerProfileRequest;
import com.user_service.dto.request.CompleteDriverProfileRequest;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.VehicleType;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.service.IUserProfileService;
import com.user_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final StorageService storageService;

    @Override
    @Transactional
    public void createCustomerProfile(User user, String firstName, String lastName) {
        Customer customer = new Customer();
        customer.setId(user.getId());
        customer.setUser(user);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);
        customerRepository.save(customer);
        log.info("Customer profile created for user: {}", user.getId());
    }

    @Override
    @Transactional
    public void createDriverProfile(User user, String firstName, String lastName,
                                    String vehicleType, String vehicleNumber, String licenseNumber) {
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFirstName(firstName);
        driver.setLastName(lastName);
        driver.setVehicleType(VehicleType.valueOf(vehicleType));
        driver.setVehicleNumber(vehicleNumber);
        driver.setLicenseNumber(licenseNumber);
        driver.setIsAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setVerificationDocuments(new HashMap<>());
        driverRepository.save(driver);
        log.info("Driver profile created for user: {}", user.getId());
    }

    @Override
    @Transactional
    public void completeCustomerProfile(UUID userId, User user, CompleteCustomerProfileRequest request) {
        Customer customer = customerRepository.findById(userId).orElseGet(() -> {
            Customer c = new Customer();
            c.setId(user.getId());
            c.setUser(user);
            c.setWalletBalance(BigDecimal.ZERO);
            c.setTotalOrders(0);
            return c;
        });
        customer.setProfileImage(uploadIfPresent(request.getProfileImage(), "customers/profiles"));
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customerRepository.save(customer);
        log.info("Customer profile completed for user: {}", userId);
    }

    @Override
    @Transactional
    public void completeDriverProfile(UUID userId, User user, CompleteDriverProfileRequest request) {
        Driver driver = driverRepository.findByUserId(userId).orElseGet(() -> {
            Driver d = new Driver();
            d.setUser(user);
            d.setIsAvailable(false);
            d.setRating(BigDecimal.ZERO);
            d.setTotalDeliveries(0);
            d.setWalletBalance(BigDecimal.ZERO);
            d.setVerificationDocuments(new HashMap<>());
            return d;
        });
        driver.setProfileImage(uploadIfPresent(request.getProfileImage(), "drivers/profiles"));
        driver.setLicenseImage(uploadIfPresent(request.getLicenseImage(), "drivers/licenses"));
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driverRepository.save(driver);
        log.info("Driver profile completed for user: {}", userId);
    }

    @Override
    @Transactional
    public String uploadProfileImage(UUID userId, MultipartFile file) {
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + userId));
        String oldImage = customer.getProfileImage();
        if (oldImage != null) {
            storageService.deleteFile(oldImage);
        }
        String imageUrl = storageService.uploadFile(file, "customers/profiles");
        customer.setProfileImage(imageUrl);
        customerRepository.save(customer);
        log.info("Profile image updated for customer: {}", userId);
        return imageUrl;
    }

    @Override
    @Transactional
    public String uploadDriverProfileImage(UUID userId, MultipartFile file) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + userId));
        String oldImage = driver.getProfileImage();
        if (oldImage != null) {
            storageService.deleteFile(oldImage);
        }
        String imageUrl = storageService.uploadFile(file, "drivers/profiles");
        driver.setProfileImage(imageUrl);
        driverRepository.save(driver);
        log.info("Profile image updated for driver: {}", userId);
        return imageUrl;
    }

    @Override
    @Transactional
    public String uploadDriverLicenseImage(UUID userId, MultipartFile file) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + userId));
        String oldImage = driver.getLicenseImage();
        if (oldImage != null) {
            storageService.deleteFile(oldImage);
        }
        String imageUrl = storageService.uploadFile(file, "drivers/licenses");
        driver.setLicenseImage(imageUrl);
        driverRepository.save(driver);
        log.info("License image updated for driver: {}", userId);
        return imageUrl;
    }

    private String uploadIfPresent(MultipartFile file, String path) {
        return (file != null && !file.isEmpty()) ? storageService.uploadFile(file, path) : null;
    }
}