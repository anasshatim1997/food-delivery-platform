package com.user_service.mapper;

import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.DriverResponse;
import com.user_service.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfigCentral.class)
public interface DriverMapper {

    DriverResponse toDriverResponse(Driver driver);

    void updateDriver(UpdateDriverRequest request, @MappingTarget Driver driver);
}