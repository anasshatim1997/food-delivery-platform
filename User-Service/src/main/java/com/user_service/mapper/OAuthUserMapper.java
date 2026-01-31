package com.user_service.mapper;

import com.user_service.dto.response.OAuthRawUser;
import com.user_service.dto.response.OAuthUserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OAuthUserMapper {

    OAuthUserInfo toOAuthUserInfo(OAuthRawUser rawUser);
}
