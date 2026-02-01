package com.user_service.mapper;

import com.user_service.dto.request.RegisterRequest;
import com.user_service.dto.response.CustomerProfileResponse;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.dto.response.UserProfileResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfigCentral.class)
public interface UserMapper {

    UserResponse toUserResponse(User user);

    UserProfileResponse toUserProfileResponse(User user);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "isVerified", source = "user.isVerified")
    @Mapping(target = "oauthProvider", source = "user.oauthProvider")
    @Mapping(target = "oauthProviderId", source = "user.oauthProviderId")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "updatedAt", source = "user.updatedAt")
    @Mapping(target = "firstName", source = "customer.firstName")
    @Mapping(target = "lastName", source = "customer.lastName")
    @Mapping(target = "profileImage", source = "customer.profileImage")
    @Mapping(target = "walletBalance", source = "customer.walletBalance")
    @Mapping(target = "totalOrders", source = "customer.totalOrders")
    CustomerProfileResponse toCustomerProfileResponse(User user, Customer customer);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "isVerified", source = "user.isVerified")
    @Mapping(target = "oauthProvider", source = "user.oauthProvider")
    @Mapping(target = "oauthProviderId", source = "user.oauthProviderId")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "updatedAt", source = "user.updatedAt")
    @Mapping(target = "firstName", source = "driver.firstName")
    @Mapping(target = "lastName", source = "driver.lastName")
    @Mapping(target = "profileImage", source = "driver.profileImage")
    @Mapping(target = "vehicleType", source = "driver.vehicleType")
    @Mapping(target = "vehicleNumber", source = "driver.vehicleNumber")
    @Mapping(target = "licenseNumber", source = "driver.licenseNumber")
    @Mapping(target = "isAvailable", source = "driver.isAvailable")
    @Mapping(target = "currentLat", source = "driver.currentLat")
    @Mapping(target = "currentLng", source = "driver.currentLng")
    @Mapping(target = "rating", source = "driver.rating")
    @Mapping(target = "totalDeliveries", source = "driver.totalDeliveries")
    @Mapping(target = "walletBalance", source = "driver.walletBalance")
    @Mapping(target = "verificationStatus", source = "driver.verificationStatus")
    @Mapping(target = "verificationDocuments", source = "driver.verificationDocuments")
    DriverProfileResponse toDriverProfileResponse(User user, Driver driver);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "isVerified", constant = "false")
    @Mapping(target = "verificationCode", ignore = true)
    @Mapping(target = "verificationCodeExpiresAt", ignore = true)
    @Mapping(target = "oauthProvider", ignore = true)
    @Mapping(target = "oauthProviderId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(RegisterRequest request, String encodedPassword);
}