package com.user_service.service.impl;

import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;
import com.user_service.entity.Address;
import com.user_service.entity.Customer;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.AddressRepository;
import com.user_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private UUID customerId;
    private UUID addressId;
    private Customer customer;
    private Address address;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId  = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);

        address = new Address();
        address.setId(addressId);
        address.setCustomer(customer);
        address.setLabel("Home");
        address.setStreet("Main St");
        address.setBuilding("Building 1");
        address.setCity("Casablanca");
        address.setLatitude(new BigDecimal("33.57"));
        address.setLongitude(new BigDecimal("-7.58"));
        address.setIsDefault(true);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createAddress")
    class CreateAddress {

        @Test
        @DisplayName("saves address, clears old defaults when isDefault=true, updates customer default id")
        void createAddress_success_withDefault() {
            CreateAddressRequest request = buildCreateRequest("Home", true);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.countByCustomerId(customerId)).thenReturn(1);
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
                Address a = inv.getArgument(0);
                a.setId(addressId);
                return a;
            });

            AddressResponse response = addressService.createAddress(customerId, request);

            assertThat(response.getLabel()).isEqualTo("Home");
            assertThat(response.getIsDefault()).isTrue();
            verify(addressRepository).clearAllDefaults(customerId);
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("auto-sets default when customer has no addresses yet")
        void createAddress_firstAddress_becomesDefault() {
            CreateAddressRequest request = buildCreateRequest("Work", false);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.countByCustomerId(customerId)).thenReturn(0);
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
                Address a = inv.getArgument(0);
                a.setId(addressId);
                return a;
            });

            AddressResponse response = addressService.createAddress(customerId, request);

            assertThat(response.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("throws when customer has reached the 10-address limit")
        void createAddress_limitReached() {
            CreateAddressRequest request = buildCreateRequest("Other", false);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.countByCustomerId(customerId)).thenReturn(10);

            assertThatThrownBy(() -> addressService.createAddress(customerId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Maximum of 10");
        }

        @Test
        @DisplayName("throws when customer does not exist")
        void createAddress_customerNotFound() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.createAddress(customerId, buildCreateRequest("Home", false)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAddresses")
    class GetAddresses {

        @Test
        @DisplayName("returns all addresses for the customer ordered by default then date")
        void getAddresses_success() {
            when(customerRepository.existsById(customerId)).thenReturn(true);
            when(addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(eq(customerId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(address)));

            List<AddressResponse> responses = addressService.getAddresses(customerId);

            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().getLabel()).isEqualTo("Home");
        }

        @Test
        @DisplayName("throws when customer does not exist")
        void getAddresses_customerNotFound() {
            when(customerRepository.existsById(customerId)).thenReturn(false);

            assertThatThrownBy(() -> addressService.getAddresses(customerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAddress")
    class GetAddress {

        @Test
        @DisplayName("returns the address when both ids match")
        void getAddress_success() {
            when(customerRepository.existsById(customerId)).thenReturn(true);
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.of(address));

            AddressResponse response = addressService.getAddress(customerId, addressId);

            assertThat(response.getId()).isEqualTo(addressId);
            assertThat(response.getLabel()).isEqualTo("Home");
        }

        @Test
        @DisplayName("throws when address does not belong to customer")
        void getAddress_notFound() {
            when(customerRepository.existsById(customerId)).thenReturn(true);
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.getAddress(customerId, addressId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateAddress")
    class UpdateAddress {

        @Test
        @DisplayName("updates only the non-null fields and saves")
        void updateAddress_partialUpdate() {
            UpdateAddressRequest request = new UpdateAddressRequest();
            request.setLabel("Work");
            request.setCity("Rabat");

            when(customerRepository.existsById(customerId)).thenReturn(true);
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.of(address));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressResponse response = addressService.updateAddress(customerId, addressId, request);

            assertThat(response.getLabel()).isEqualTo("Work");
            assertThat(response.getCity()).isEqualTo("Rabat");
            assertThat(response.getStreet()).isEqualTo("Main St");
        }

        @Test
        @DisplayName("throws when address does not belong to customer")
        void updateAddress_notFound() {
            when(customerRepository.existsById(customerId)).thenReturn(true);
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.updateAddress(customerId, addressId, new UpdateAddressRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteAddress")
    class DeleteAddress {

        @Test
        @DisplayName("deletes address and reassigns default to next address when deleted was default")
        void deleteAddress_wasDefault_reassignsNext() {
            Address nextAddress = new Address();
            nextAddress.setId(UUID.randomUUID());
            nextAddress.setCustomer(customer);
            nextAddress.setIsDefault(false);

            address.setIsDefault(true);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.of(address));
            when(addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(eq(customerId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(nextAddress)));

            addressService.deleteAddress(customerId, addressId);

            verify(addressRepository).delete(address);
            verify(addressRepository).save(nextAddress);
            assertThat(nextAddress.getIsDefault()).isTrue();
            assertThat(customer.getDefaultAddressId()).isEqualTo(nextAddress.getId());
        }

        @Test
        @DisplayName("deletes non-default address without touching customer default")
        void deleteAddress_notDefault_noReassignment() {
            address.setIsDefault(false);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.of(address));

            addressService.deleteAddress(customerId, addressId);

            verify(addressRepository).delete(address);
            verify(addressRepository, never()).findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("clears customer defaultAddressId when last address is deleted")
        void deleteAddress_lastAddress_clearsDefault() {
            address.setIsDefault(true);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.of(address));
            when(addressRepository.findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(eq(customerId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            addressService.deleteAddress(customerId, addressId);

            assertThat(customer.getDefaultAddressId()).isNull();
            verify(customerRepository).save(customer);
        }
    }

    @Nested
    @DisplayName("setDefaultAddress")
    class SetDefaultAddress {

        @Test
        @DisplayName("clears all defaults, sets new default, and updates customer")
        void setDefaultAddress_success() {
            address.setIsDefault(false);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.of(address));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressResponse response = addressService.setDefaultAddress(customerId, addressId);

            assertThat(response.getIsDefault()).isTrue();
            verify(addressRepository).clearAllDefaults(customerId);
            assertThat(customer.getDefaultAddressId()).isEqualTo(addressId);
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("throws when address does not belong to customer")
        void setDefaultAddress_notFound() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.findByIdAndCustomerId(addressId, customerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.setDefaultAddress(customerId, addressId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private CreateAddressRequest buildCreateRequest(String label, boolean isDefault) {
        CreateAddressRequest r = new CreateAddressRequest();
        r.setLabel(label);
        r.setStreet("Main St");
        r.setBuilding("Building 1");
        r.setCity("Casablanca");
        r.setLatitude(new BigDecimal("33.57"));
        r.setLongitude(new BigDecimal("-7.58"));
        r.setDefault(isDefault);
        return r;
    }
}