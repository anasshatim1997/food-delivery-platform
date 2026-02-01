package com.user_service.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CustomerProfileResponse.class, name = "customer"),
        @JsonSubTypes.Type(value = DriverProfileResponse.class, name = "driver")
})
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String phone;
    private Role role;
    private Status status;
    private Boolean isVerified;
    private String oauthProvider;
    private String oauthProviderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}