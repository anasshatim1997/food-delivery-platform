package com.user_service.service;

import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface IAddressService {
    AddressResponse createAddress(UUID customerId, CreateAddressRequest request);
    List<AddressResponse> getAddresses(UUID customerId);
    AddressResponse getAddress(UUID customerId, UUID addressId);
    AddressResponse updateAddress(UUID customerId, UUID addressId, UpdateAddressRequest request);
    void deleteAddress(UUID customerId, UUID addressId);
    AddressResponse setDefaultAddress(UUID customerId, UUID addressId);
}