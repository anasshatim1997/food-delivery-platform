package com.user_service.mapper;

import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;
import com.user_service.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfigCentral.class)
public interface AddressMapper {

    AddressResponse toAddressResponse(Address address);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Address toAddress(CreateAddressRequest request);

    void updateAddress(UpdateAddressRequest request, @MappingTarget Address address);
}