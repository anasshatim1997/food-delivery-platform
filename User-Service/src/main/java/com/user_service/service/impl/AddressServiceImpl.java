package com.user_service.service.impl;

import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;
import com.user_service.entity.Address;
import com.user_service.entity.Customer;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.AddressRepository;
import com.user_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.user_service.service.IAddressService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements IAddressService {

    private static final int MAX_ADDRESSES_PER_CUSTOMER = 10;

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public AddressResponse createAddress(UUID customerId, CreateAddressRequest request) {
        Customer customer = findCustomerOrThrow(customerId);

        if (addressRepository.countByCustomerId(customerId) >= MAX_ADDRESSES_PER_CUSTOMER) {
            throw new IllegalStateException(
                    "Maximum of " + MAX_ADDRESSES_PER_CUSTOMER + " addresses allowed per customer"
            );
        }

        if (request.isDefault()) {
            addressRepository.clearAllDefaults(customerId);
        }

        boolean hasNoAddresses = addressRepository.countByCustomerId(customerId) == 0;
        boolean shouldBeDefault = request.isDefault() || hasNoAddresses;

        Address address = new Address();
        address.setCustomer(customer);
        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setBuilding(request.getBuilding());
        address.setFloor(request.getFloor());
        address.setApartment(request.getApartment());
        address.setCity(request.getCity());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDeliveryInstructions(request.getDeliveryInstructions());
        address.setIsDefault(shouldBeDefault);

        address = addressRepository.save(address);

        if (shouldBeDefault) {
            customer.setDefaultAddressId(address.getId());
            customerRepository.save(customer);
        }

        log.info("Address created for customer {}: {}", customerId, address.getId());
        return toResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID customerId) {
        assertCustomerExists(customerId);
        return addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(UUID customerId, UUID addressId) {
        assertCustomerExists(customerId);
        Address address = findAddressOrThrow(customerId, addressId);
        return toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID customerId, UUID addressId, UpdateAddressRequest request) {
        assertCustomerExists(customerId);
        Address address = findAddressOrThrow(customerId, addressId);

        if (request.getLabel() != null)               address.setLabel(request.getLabel());
        if (request.getStreet() != null)              address.setStreet(request.getStreet());
        if (request.getBuilding() != null)            address.setBuilding(request.getBuilding());
        if (request.getFloor() != null)               address.setFloor(request.getFloor());
        if (request.getApartment() != null)           address.setApartment(request.getApartment());
        if (request.getCity() != null)                address.setCity(request.getCity());
        if (request.getLatitude() != null)            address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)           address.setLongitude(request.getLongitude());
        if (request.getDeliveryInstructions() != null) address.setDeliveryInstructions(request.getDeliveryInstructions());

        address = addressRepository.save(address);
        log.info("Address updated for customer {}: {}", customerId, addressId);
        return toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        Customer customer = findCustomerOrThrow(customerId);
        Address address = findAddressOrThrow(customerId, addressId);

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);

        if (wasDefault) {
            customer.setDefaultAddressId(null);
            addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        addressRepository.save(next);
                        customer.setDefaultAddressId(next.getId());
                    });
            customerRepository.save(customer);
        }

        log.info("Address deleted for customer {}: {}", customerId, addressId);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID customerId, UUID addressId) {
        Customer customer = findCustomerOrThrow(customerId);
        Address address = findAddressOrThrow(customerId, addressId);

        addressRepository.clearAllDefaults(customerId);
        address.setIsDefault(true);
        address = addressRepository.save(address);

        customer.setDefaultAddressId(address.getId());
        customerRepository.save(customer);

        log.info("Default address set for customer {}: {}", customerId, addressId);
        return toResponse(address);
    }

    private Customer findCustomerOrThrow(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
    }

    private void assertCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        }
    }

    private Address findAddressOrThrow(UUID customerId, UUID addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found: " + addressId + " for customer: " + customerId
                ));
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .customerId(address.getCustomer().getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .building(address.getBuilding())
                .floor(address.getFloor())
                .apartment(address.getApartment())
                .city(address.getCity())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .deliveryInstructions(address.getDeliveryInstructions())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}