package com.user_service.mapper;

import com.user_service.dto.request.CreateCustomerRequest;
import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.response.CustomerResponse;
import com.user_service.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    CustomerResponse toCustomerResponse(Customer customer);

    Customer toCustomer(CreateCustomerRequest request);

    void updateCustomer(UpdateCustomerRequest request, @MappingTarget Customer customer);
}