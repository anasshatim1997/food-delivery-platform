package com.user_service.mapper;

import com.user_service.dto.request.RegisterRequest;
import com.user_service.dto.response.CustomerProfileResponse;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.dto.response.UserProfileResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = MapperConfigCentral.class)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "profileCompleted", ignore = true)
    @Mapping(target = "verificationCode", ignore = true)
    @Mapping(target = "verificationCodeExpiresAt", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "passwordResetTokenExpiresAt", ignore = true)
    @Mapping(target = "oauthProvider", ignore = true)
    @Mapping(target = "oauthProviderId", ignore = true)
    User toUser(RegisterRequest request, String encodedPassword);

    UserResponse toUserResponse(User user);

    UserProfileResponse toUserProfileResponse(User user);

    @BeanMapping(resultType = CustomerProfileResponse.class)
    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "user.role", target = "role")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.isVerified", target = "isVerified")
    @Mapping(source = "user.oauthProvider", target = "oauthProvider")
    @Mapping(source = "user.oauthProviderId", target = "oauthProviderId")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.updatedAt", target = "updatedAt")
    @Mapping(source = "customer.firstName", target = "firstName")
    @Mapping(source = "customer.lastName", target = "lastName")
    @Mapping(source = "customer.profileImage", target = "profileImage")
    @Mapping(source = "customer.walletBalance", target = "walletBalance")
    @Mapping(source = "customer.totalOrders", target = "totalOrders")
    CustomerProfileResponse toCustomerProfileResponse(User user, Customer customer);

    @BeanMapping(resultType = DriverProfileResponse.class)
    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "user.role", target = "role")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.isVerified", target = "isVerified")
    @Mapping(source = "user.oauthProvider", target = "oauthProvider")
    @Mapping(source = "user.oauthProviderId", target = "oauthProviderId")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.updatedAt", target = "updatedAt")
    @Mapping(source = "driver.firstName", target = "firstName")
    @Mapping(source = "driver.lastName", target = "lastName")
    @Mapping(source = "driver.profileImage", target = "profileImage")
    @Mapping(source = "driver.vehicleType", target = "vehicleType")
    @Mapping(source = "driver.vehicleNumber", target = "vehicleNumber")
    @Mapping(source = "driver.licenseNumber", target = "licenseNumber")
    @Mapping(source = "driver.isAvailable", target = "isAvailable")
    @Mapping(source = "driver.currentLat", target = "currentLat")
    @Mapping(source = "driver.currentLng", target = "currentLng")
    @Mapping(source = "driver.rating", target = "rating")
    @Mapping(source = "driver.totalDeliveries", target = "totalDeliveries")
    @Mapping(source = "driver.walletBalance", target = "walletBalance")
    @Mapping(source = "driver.verificationStatus", target = "verificationStatus")
    @Mapping(source = "driver.verificationDocuments", target = "verificationDocuments")
    DriverProfileResponse toDriverProfileResponse(User user, Driver driver);
}