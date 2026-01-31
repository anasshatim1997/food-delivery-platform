package com.user_service.service;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;

public interface IAuthService {

    AuthResponse registerCustomer(RegisterRequest request);

    AuthResponse registerDriver(RegisterRequest request);

    void updateCustomerProfile(String email, CreateCustomerRequest request);

    void updateDriverProfile(String email, CreateDriverRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse oauthLogin(OAuthLoginRequest request);
}