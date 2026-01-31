package com.user_service.mapper;

import com.user_service.dto.request.CreateCustomerRequest;
import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.response.CustomerResponse;
import com.user_service.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(customer.getId() != null ? customer.getId().toString() : null)
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .profileImage(customer.getProfileImage())
                .walletBalance(customer.getWalletBalance())
                .totalOrders(customer.getTotalOrders())
                .status(customer.getStatus() != null ? customer.getStatus().name() : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public Customer toCustomer(CreateCustomerRequest request) {
        if (request == null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setProfileImage(request.getProfileImage());

        return customer;
    }

    public void updateCustomer(UpdateCustomerRequest request, Customer customer) {
        if (request == null || customer == null) {
            return;
        }

        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getProfileImage() != null) {
            customer.setProfileImage(request.getProfileImage());
        }
    }
}