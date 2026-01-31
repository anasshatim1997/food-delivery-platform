package com.user_service.service;


import com.user_service.dto.response.OAuthUserInfo;

public interface IOAuthService {
    OAuthUserInfo verifyGoogleToken(String token);
    OAuthUserInfo verifyFacebookToken(String token);
}