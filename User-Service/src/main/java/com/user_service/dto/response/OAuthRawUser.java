package com.user_service.dto.response;
import lombok.*;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OAuthRawUser {
    private String email;
    private String name;
    private String providerId;
    private String provider;
    private String profileImage;
}