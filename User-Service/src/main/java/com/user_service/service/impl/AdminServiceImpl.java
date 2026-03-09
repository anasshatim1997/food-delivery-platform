package com.user_service.service.impl;

import com.user_service.dto.request.UpdateStatusRequest;
import com.user_service.dto.request.UpdateVerificationRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.UserMapper;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.security.RoleAnnotations;
import com.user_service.service.IAdminService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@RoleAnnotations.IsAdmin
public class AdminServiceImpl implements IAdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String email, Role role, Status status, Pageable pageable) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (email != null && !email.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"
                ));
            }

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable)
                .map(userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(UUID userId, UpdateStatusRequest request) {
        User user = findUserOrThrow(userId);

        log.info("Admin updating user {} status from {} to {}. Reason: {}",
                userId, user.getStatus(), request.getStatus(), request.getReason());

        user.setStatus(request.getStatus());
        userRepository.save(user);

        log.info("User {} status updated to {}", userId, request.getStatus());
    }

    @Override
    @Transactional
    public void updateDriverVerification(UUID driverId, UpdateVerificationRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        log.info("Admin updating driver {} verification from {} to {}. Reason: {}",
                driverId, driver.getVerificationStatus(), request.getStatus(), request.getReason());

        driver.setVerificationStatus(request.getStatus());
        driverRepository.save(driver);

        log.info("Driver {} verification updated to {}", driverId, request.getStatus());
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}