package com.user_service.service;

import com.user_service.dto.response.AuthResponse;
import com.user_service.enums.Role;

public interface IOAuthService {
    AuthResponse loginWithGoogle(String accessToken, Role targetRole);
    AuthResponse loginWithFacebook(String accessToken, Role targetRole);
}