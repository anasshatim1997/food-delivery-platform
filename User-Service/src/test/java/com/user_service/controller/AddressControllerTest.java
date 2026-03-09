package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.CreateAddressRequest;
import com.user_service.dto.request.UpdateAddressRequest;
import com.user_service.dto.response.AddressResponse;
import com.user_service.service.IAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressController Unit Tests")
class AddressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IAddressService addressService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AddressController addressController;

    private ObjectMapper objectMapper;

    private UUID customerId;
    private UUID addressId;
    private CreateAddressRequest createRequest;
    private UpdateAddressRequest updateRequest;
    private AddressResponse addressResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController).build();
        objectMapper = new ObjectMapper();

        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        // Setup mock authentication
        when(authentication.getName()).thenReturn(customerId.toString());

        // Setup test data
        createRequest = new CreateAddressRequest();
        createRequest.setLabel("Home");
        createRequest.setStreet("123 Main Street");
        createRequest.setBuilding("Building A");
        createRequest.setFloor("5");
        createRequest.setApartment("501");
        createRequest.setCity("Cairo");
        createRequest.setLatitude(new BigDecimal("30.0444"));
        createRequest.setLongitude(new BigDecimal("31.2357"));
        createRequest.setDeliveryInstructions("Ring doorbell twice");
        createRequest.setDefault(false);

        updateRequest = new UpdateAddressRequest();
        updateRequest.setLabel("Work");
        updateRequest.setStreet("456 Business Avenue");
        updateRequest.setBuilding("Tower B");
        updateRequest.setCity("Giza");

        addressResponse = AddressResponse.builder()
                .id(addressId)
                .label("Home")
                .street("123 Main Street")
                .building("Building A")
                .floor("5")
                .apartment("501")
                .city("Cairo")
                .latitude(new BigDecimal("30.0444"))
                .longitude(new BigDecimal("31.2357"))
                .deliveryInstructions("Ring doorbell twice")
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create address successfully")
    void testCreateAddress_Success() throws Exception {
        // Given
        when(addressService.createAddress(eq(customerId), any(CreateAddressRequest.class)))
                .thenReturn(addressResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/users/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Address created successfully"))
                .andExpect(jsonPath("$.data.id").value(addressId.toString()))
                .andExpect(jsonPath("$.data.label").value("Home"))
                .andExpect(jsonPath("$.data.street").value("123 Main Street"))
                .andExpect(jsonPath("$.data.building").value("Building A"))
                .andExpect(jsonPath("$.data.city").value("Cairo"));

        verify(addressService, times(1)).createAddress(eq(customerId), any(CreateAddressRequest.class));
    }

    @Test
    @DisplayName("Should fail to create address with invalid label")
    void testCreateAddress_InvalidLabel() throws Exception {
        // Given
        createRequest.setLabel("InvalidLabel");

        // When & Then
        mockMvc.perform(post("/api/users/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(UUID.class), any(CreateAddressRequest.class));
    }

    @Test
    @DisplayName("Should fail to create address with missing required fields")
    void testCreateAddress_MissingRequiredFields() throws Exception {
        // Given
        CreateAddressRequest invalidRequest = new CreateAddressRequest();
        invalidRequest.setLabel("Home");
        // Missing street, building, city, latitude, longitude

        // When & Then
        mockMvc.perform(post("/api/users/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(UUID.class), any(CreateAddressRequest.class));
    }

    @Test
    @DisplayName("Should fail to create address with invalid latitude")
    void testCreateAddress_InvalidLatitude() throws Exception {
        // Given
        createRequest.setLatitude(new BigDecimal("95.0")); // Invalid: > 90

        // When & Then
        mockMvc.perform(post("/api/users/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(UUID.class), any(CreateAddressRequest.class));
    }

    @Test
    @DisplayName("Should fail to create address with invalid longitude")
    void testCreateAddress_InvalidLongitude() throws Exception {
        // Given
        createRequest.setLongitude(new BigDecimal("185.0")); // Invalid: > 180

        // When & Then
        mockMvc.perform(post("/api/users/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(any(UUID.class), any(CreateAddressRequest.class));
    }

    @Test
    @DisplayName("Should get all addresses successfully")
    void testGetAddresses_Success() throws Exception {
        // Given
        AddressResponse address2 = AddressResponse.builder()
                .id(UUID.randomUUID())
                .label("Work")
                .street("456 Office Road")
                .building("Tower B")
                .city("Giza")
                .latitude(new BigDecimal("30.0131"))
                .longitude(new BigDecimal("31.2089"))
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<AddressResponse> addresses = Arrays.asList(addressResponse, address2);
        when(addressService.getAddresses(customerId)).thenReturn(addresses);

        // When & Then
        mockMvc.perform(get("/api/users/v1/addresses")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Addresses retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].label").value("Home"))
                .andExpect(jsonPath("$.data[1].label").value("Work"));

        verify(addressService, times(1)).getAddresses(customerId);
    }

    @Test
    @DisplayName("Should get paginated addresses successfully")
    void testGetAddressesPaginated_Success() throws Exception {
        // Given
        List<AddressResponse> addresses = Arrays.asList(addressResponse);
        Page<AddressResponse> addressPage = new PageImpl<>(addresses, PageRequest.of(0, 10), 1);

        when(addressService.getAddressesPaginated(eq(customerId), any(Pageable.class)))
                .thenReturn(addressPage);

        // When & Then
        mockMvc.perform(get("/api/users/v1/addresses/paginated")
                        .param("page", "0")
                        .param("size", "10")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Addresses retrieved successfully"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.number").value(0));

        verify(addressService, times(1)).getAddressesPaginated(eq(customerId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get paginated addresses with custom page size")
    void testGetAddressesPaginated_CustomPageSize() throws Exception {
        // Given
        List<AddressResponse> addresses = Arrays.asList(addressResponse);
        Page<AddressResponse> addressPage = new PageImpl<>(addresses, PageRequest.of(1, 5), 10);

        when(addressService.getAddressesPaginated(eq(customerId), any(Pageable.class)))
                .thenReturn(addressPage);

        // When & Then
        mockMvc.perform(get("/api/users/v1/addresses/paginated")
                        .param("page", "1")
                        .param("size", "5")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(5));

        verify(addressService, times(1)).getAddressesPaginated(eq(customerId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get single address successfully")
    void testGetAddress_Success() throws Exception {
        // Given
        when(addressService.getAddress(customerId, addressId)).thenReturn(addressResponse);

        // When & Then
        mockMvc.perform(get("/api/users/v1/addresses/{addressId}", addressId)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Address retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(addressId.toString()))
                .andExpect(jsonPath("$.data.label").value("Home"))
                .andExpect(jsonPath("$.data.street").value("123 Main Street"));

        verify(addressService, times(1)).getAddress(customerId, addressId);
    }

    @Test
    @DisplayName("Should update address successfully")
    void testUpdateAddress_Success() throws Exception {
        // Given
        AddressResponse updatedResponse = AddressResponse.builder()
                .id(addressId)
                .label("Work")
                .street("456 Business Avenue")
                .building("Tower B")
                .city("Giza")
                .latitude(new BigDecimal("30.0444"))
                .longitude(new BigDecimal("31.2357"))
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(addressService.updateAddress(eq(customerId), eq(addressId), any(UpdateAddressRequest.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(patch("/api/users/v1/addresses/{addressId}", addressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Address updated successfully"))
                .andExpect(jsonPath("$.data.id").value(addressId.toString()))
                .andExpect(jsonPath("$.data.label").value("Work"))
                .andExpect(jsonPath("$.data.street").value("456 Business Avenue"));

        verify(addressService, times(1)).updateAddress(eq(customerId), eq(addressId), any(UpdateAddressRequest.class));
    }

    @Test
    @DisplayName("Should delete address successfully")
    void testDeleteAddress_Success() throws Exception {
        // Given
        doNothing().when(addressService).deleteAddress(customerId, addressId);

        // When & Then
        mockMvc.perform(delete("/api/users/v1/addresses/{addressId}", addressId)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Address deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(addressService, times(1)).deleteAddress(customerId, addressId);
    }

    @Test
    @DisplayName("Should set default address successfully")
    void testSetDefaultAddress_Success() throws Exception {
        // Given
        AddressResponse defaultAddressResponse = AddressResponse.builder()
                .id(addressId)
                .label("Home")
                .street("123 Main Street")
                .building("Building A")
                .city("Cairo")
                .latitude(new BigDecimal("30.0444"))
                .longitude(new BigDecimal("31.2357"))
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(addressService.setDefaultAddress(customerId, addressId))
                .thenReturn(defaultAddressResponse);

        // When & Then
        mockMvc.perform(put("/api/users/v1/addresses/{addressId}/default", addressId)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Default address updated successfully"))
                .andExpect(jsonPath("$.data.id").value(addressId.toString()))
                .andExpect(jsonPath("$.data.isDefault").value(true));

        verify(addressService, times(1)).setDefaultAddress(customerId, addressId);
    }

    @Test
    @DisplayName("Should handle authentication extraction correctly")
    void testAuthenticationExtraction() throws Exception {
        // Given
        UUID testCustomerId = UUID.randomUUID();
        when(authentication.getName()).thenReturn(testCustomerId.toString());
        when(addressService.getAddresses(testCustomerId)).thenReturn(Arrays.asList(addressResponse));

        // When & Then
        mockMvc.perform(get("/api/users/v1/addresses")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk());

        verify(addressService, times(1)).getAddresses(testCustomerId);
    }

    @Test
    @DisplayName("Should use default pagination parameters when not provided")
    void testGetAddressesPaginated_DefaultParameters() throws Exception {
        // Given
        Page<AddressResponse> addressPage = new PageImpl<>(
                Arrays.asList(addressResponse),
                PageRequest.of(0, 10),
                1
        );

        when(addressService.getAddressesPaginated(eq(customerId), any(Pageable.class)))
                .thenReturn(addressPage);

        // When & Then
        mockMvc.perform(get("/api/users/v1/addresses/paginated")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        verify(addressService, times(1)).getAddressesPaginated(eq(customerId), any(Pageable.class));
    }
}