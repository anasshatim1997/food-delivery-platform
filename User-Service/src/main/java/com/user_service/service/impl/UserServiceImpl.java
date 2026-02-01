package com.user_service.service.impl;

import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.UserProfileResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.CustomerMapper;
import com.user_service.mapper.DriverMapper;
import com.user_service.mapper.UserMapper;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final DriverMapper driverMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = getUser(userId);

        return switch (user.getRole()) {
            case CUSTOMER -> {
                Customer customer = getCustomerByUserId(userId);
                yield userMapper.toCustomerProfileResponse(user, customer);
            }
            case DRIVER -> {
                Driver driver = getDriverByUserId(userId);
                yield userMapper.toDriverProfileResponse(user, driver);
            }
            default -> userMapper.toUserProfileResponse(user);
        };
    }

    @Override
    @Transactional
    public UserProfileResponse updateCustomerProfile(UUID userId, UpdateCustomerRequest request) {
        User user = getUser(userId);
        Customer customer = getCustomerByUserId(userId);

        customerMapper.updateCustomer(request, customer);
        customerRepository.save(customer);

        return userMapper.toCustomerProfileResponse(user, customer);
    }

    @Override
    @Transactional
    public UserProfileResponse updateDriverProfile(UUID userId, UpdateDriverRequest request) {
        User user = getUser(userId);
        Driver driver = getDriverByUserId(userId);

        driverMapper.updateDriver(request, driver);
        driverRepository.save(driver);

        return userMapper.toDriverProfileResponse(user, driver);
    }

    @Override
    @Transactional
    public String uploadProfileImage(UUID userId, MultipartFile image) {
        User user = getUser(userId);

        String imageUrl = uploadImageToStorage(image);

        switch (user.getRole()) {
            case CUSTOMER -> {
                Customer customer = getCustomerByUserId(userId);
                customer.setProfileImage(imageUrl);
                customerRepository.save(customer);
            }
            case DRIVER -> {
                Driver driver = getDriverByUserId(userId);
                driver.setProfileImage(imageUrl);
                driverRepository.save(driver);
            }
        }

        return imageUrl;
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
    }

    private Customer getCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "userId", userId.toString()));
    }

    private Driver getDriverByUserId(UUID userId) {
        return driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "userId", userId.toString()));
    }

    private String uploadImageToStorage(MultipartFile image) {
        return "https://example.com/uploads/" + image.getOriginalFilename();
    }
}