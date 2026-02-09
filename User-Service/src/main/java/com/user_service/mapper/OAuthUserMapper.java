package com.user_service.mapper;

import com.user_service.dto.response.OAuthRawUser;
import com.user_service.dto.response.OAuthUserInfo;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfigCentral.class)
public interface OAuthUserMapper {

    OAuthUserInfo toOAuthUserInfo(OAuthRawUser rawUser);
}