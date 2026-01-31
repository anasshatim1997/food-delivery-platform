package com.user_service.service.impl;

import com.user_service.dto.response.OAuthRawUser;
import com.user_service.dto.response.OAuthUserInfo;
import com.user_service.mapper.OAuthUserMapper;
import com.user_service.service.IOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthServiceImpl implements IOAuthService {

    private final OAuthUserMapper oauthUserMapper;

    @Override
    public OAuthUserInfo verifyGoogleToken(String token) {
        return verifyToken("GOOGLE", token, "gmail.com");
    }

    @Override
    public OAuthUserInfo verifyFacebookToken(String token) {
        return verifyToken("FACEBOOK", token, "facebook.com");
    }

    private OAuthUserInfo verifyToken(String provider, String token, String emailDomain) {
        log.info("Verifying {} token: {}", provider, token);

        OAuthRawUser rawUser = OAuthRawUser.builder()
                .email(provider.toLowerCase() + "_user_" + System.currentTimeMillis() + "@" + emailDomain)
                .name("User")
                .providerId(provider.toLowerCase() + "_" + token.hashCode())
                .provider(provider)
                .profileImage("https://via.placeholder.com/150")
                .build();

        return oauthUserMapper.toOAuthUserInfo(rawUser);
    }
}
