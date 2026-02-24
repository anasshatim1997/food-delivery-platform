package com.user_service.mapper;

import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        config = MapperConfigCentral.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "defaultAddressId", ignore = true)
    @Mapping(target = "walletBalance", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateCustomer(UpdateCustomerRequest request, @MappingTarget Customer customer);
}