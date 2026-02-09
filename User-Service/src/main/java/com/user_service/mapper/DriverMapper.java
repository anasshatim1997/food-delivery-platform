package com.user_service.mapper;

import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        config = MapperConfigCentral.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DriverMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "currentLat", ignore = true)
    @Mapping(target = "currentLng", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "totalDeliveries", ignore = true)
    @Mapping(target = "walletBalance", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "verificationDocuments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateDriver(UpdateDriverRequest request, @MappingTarget Driver driver);
}