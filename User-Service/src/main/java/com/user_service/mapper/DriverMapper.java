package com.user_service.mapper;

import com.user_service.dto.request.CreateDriverRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.DriverResponse;
import com.user_service.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DriverMapper {

    DriverResponse toDriverResponse(Driver driver);

    Driver toDriver(CreateDriverRequest request);

    void updateDriver(UpdateDriverRequest request, @MappingTarget Driver driver);
}