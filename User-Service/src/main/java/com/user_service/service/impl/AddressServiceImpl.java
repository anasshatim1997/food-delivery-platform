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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private static final int MAX_LABEL_LENGTH = 50;
    private static final int MAX_STREET_LENGTH = 200;
    private static final int MAX_BUILDING_LENGTH = 50;
    private static final int MAX_FLOOR_LENGTH = 20;
    private static final int MAX_APARTMENT_LENGTH = 20;
    private static final int MAX_CITY_LENGTH = 100;
    private static final int MAX_INSTRUCTIONS_LENGTH = 500;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(
            0, Integer.MAX_VALUE,
            Sort.by(Sort.Direction.DESC, "isDefault", "createdAt")
    );

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public AddressResponse createAddress(UUID customerId, CreateAddressRequest request) {
        validateAddressInput(request);

        Customer customer = findCustomerOrThrow(customerId);

        long currentCount = addressRepository.countByCustomerId(customerId);
        if (currentCount >= MAX_ADDRESSES_PER_CUSTOMER) {
            throw new IllegalStateException(
                    "Maximum of " + MAX_ADDRESSES_PER_CUSTOMER + " addresses allowed per customer"
            );
        }

        boolean shouldBeDefault = request.isDefault() || currentCount == 0;

        if (shouldBeDefault) {
            addressRepository.clearAllDefaults(customerId);
        }

        Address address = buildAddress(customer, request);
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
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId, DEFAULT_PAGEABLE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddressResponse> getAddressesPaginated(UUID customerId, Pageable pageable) {
        assertCustomerExists(customerId);
        return addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(UUID customerId, UUID addressId) {
        assertCustomerExists(customerId);
        return toResponse(findAddressOrThrow(customerId, addressId));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID customerId, UUID addressId, UpdateAddressRequest request) {
        validateUpdateAddressInput(request);
        assertCustomerExists(customerId);
        Address address = findAddressOrThrow(customerId, addressId);

        if (request.getLabel() != null)                address.setLabel(request.getLabel());
        if (request.getStreet() != null)               address.setStreet(request.getStreet());
        if (request.getBuilding() != null)             address.setBuilding(request.getBuilding());
        if (request.getFloor() != null)                address.setFloor(request.getFloor());
        if (request.getApartment() != null)            address.setApartment(request.getApartment());
        if (request.getCity() != null)                 address.setCity(request.getCity());
        if (request.getLatitude() != null)             address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)            address.setLongitude(request.getLongitude());
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
            addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customerId, DEFAULT_PAGEABLE)
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

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            return toResponse(address);
        }

        addressRepository.lockAllByCustomerId(customerId);
        addressRepository.clearAllDefaults(customerId);
        addressRepository.flush();

        address.setIsDefault(true);
        address = addressRepository.save(address);

        customer.setDefaultAddressId(address.getId());
        customerRepository.save(customer);

        return toResponse(address);
    }

    private Address buildAddress(Customer customer, CreateAddressRequest request) {
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
        return address;
    }

    private void validateFields(String label, String street, String building,
                                String floor, String apartment, String city,
                                String deliveryInstructions) {
        if (label != null && label.length() > MAX_LABEL_LENGTH)
            throw new IllegalArgumentException("Label exceeds maximum length of " + MAX_LABEL_LENGTH);
        if (street != null && street.length() > MAX_STREET_LENGTH)
            throw new IllegalArgumentException("Street exceeds maximum length of " + MAX_STREET_LENGTH);
        if (building != null && building.length() > MAX_BUILDING_LENGTH)
            throw new IllegalArgumentException("Building exceeds maximum length of " + MAX_BUILDING_LENGTH);
        if (floor != null && floor.length() > MAX_FLOOR_LENGTH)
            throw new IllegalArgumentException("Floor exceeds maximum length of " + MAX_FLOOR_LENGTH);
        if (apartment != null && apartment.length() > MAX_APARTMENT_LENGTH)
            throw new IllegalArgumentException("Apartment exceeds maximum length of " + MAX_APARTMENT_LENGTH);
        if (city != null && city.length() > MAX_CITY_LENGTH)
            throw new IllegalArgumentException("City exceeds maximum length of " + MAX_CITY_LENGTH);
        if (deliveryInstructions != null && deliveryInstructions.length() > MAX_INSTRUCTIONS_LENGTH)
            throw new IllegalArgumentException("Delivery instructions exceed maximum length of " + MAX_INSTRUCTIONS_LENGTH);
    }

    private void validateAddressInput(CreateAddressRequest request) {
        validateFields(request.getLabel(), request.getStreet(), request.getBuilding(),
                request.getFloor(), request.getApartment(), request.getCity(),
                request.getDeliveryInstructions());
    }

    private void validateUpdateAddressInput(UpdateAddressRequest request) {
        validateFields(request.getLabel(), request.getStreet(), request.getBuilding(),
                request.getFloor(), request.getApartment(), request.getCity(),
                request.getDeliveryInstructions());
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