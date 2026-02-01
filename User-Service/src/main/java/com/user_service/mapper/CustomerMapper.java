package com.user_service.mapper;

import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.response.CustomerResponse;
import com.user_service.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfigCentral.class)
public interface CustomerMapper {

    CustomerResponse toCustomerResponse(Customer customer);

    void updateCustomer(UpdateCustomerRequest request, @MappingTarget Customer customer);
}