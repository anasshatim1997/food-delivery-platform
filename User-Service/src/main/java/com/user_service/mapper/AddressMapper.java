package com.user_service.mapper;

import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;
import com.user_service.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }

        return AddressResponse.builder()
                .id(address.getId() != null ? address.getId().toString() : null)
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

    public Address toAddress(CreateAddressRequest request) {
        if (request == null) {
            return null;
        }

        Address address = new Address();
        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setBuilding(request.getBuilding());
        address.setFloor(request.getFloor());
        address.setApartment(request.getApartment());
        address.setCity(request.getCity());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDeliveryInstructions(request.getDeliveryInstructions());
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);

        return address;
    }

    public void updateAddress(UpdateAddressRequest request, Address address) {
        if (request == null || address == null) {
            return;
        }

        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }
        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getBuilding() != null) {
            address.setBuilding(request.getBuilding());
        }
        if (request.getFloor() != null) {
            address.setFloor(request.getFloor());
        }
        if (request.getApartment() != null) {
            address.setApartment(request.getApartment());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }
        if (request.getDeliveryInstructions() != null) {
            address.setDeliveryInstructions(request.getDeliveryInstructions());
        }
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
    }
}