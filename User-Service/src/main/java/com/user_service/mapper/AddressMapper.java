
package com.user_service.mapper;

import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;
import com.user_service.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AddressMapper {

    AddressResponse toAddressResponse(Address address);

    Address toAddress(CreateAddressRequest request);

    void updateAddress(UpdateAddressRequest request, @MappingTarget Address address);
}