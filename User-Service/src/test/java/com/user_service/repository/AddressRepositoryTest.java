package com.user_service.repository;

import com.user_service.config.JpaConfig;
import com.user_service.entity.Address;
import com.user_service.entity.Customer;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class AddressRepositoryTest {

    @Autowired private AddressRepository addressRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(
            0, Integer.MAX_VALUE,
            Sort.by(Sort.Direction.DESC, "isDefault", "createdAt")
    );

    private Customer customer;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("customer@mail.com");
        user.setPassword("SecurePass1!");
        user.setRole(Role.CUSTOMER);
        user.setStatus(Status.ACTIVE);
        user.setIsVerified(true);
        user.setProfileCompleted(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        customer = new Customer();
        customer.setUser(user);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);
        customer = customerRepository.save(customer);
    }

    @Test
    @DisplayName("findByCustomerIdOrderByIsDefaultDescCreatedAtDesc - default address comes first, then non-defaults ordered by date desc")
    void findByCustomerIdOrderByIsDefaultDescCreatedAtDesc_sortedCorrectly() throws InterruptedException {
        Address home  = addressRepository.save(buildAddress("Home",  false)); Thread.sleep(20);
        Address work  = addressRepository.save(buildAddress("Work",  true));  Thread.sleep(20);
        Address other = addressRepository.save(buildAddress("Other", false));
        entityManager.flush();
        entityManager.clear();

        List<Address> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId(), DEFAULT_PAGEABLE)
                .getContent();

        assertThat(addresses).hasSize(3);
        assertThat(addresses.get(0).getLabel()).isEqualTo("Work");
        assertThat(addresses.get(1).getLabel()).isEqualTo("Other");
        assertThat(addresses.get(2).getLabel()).isEqualTo("Home");
    }

    @Test
    @DisplayName("findByCustomerIdOrderByIsDefaultDescCreatedAtDesc - returns empty list when customer has no addresses")
    void findByCustomerIdOrderByIsDefaultDescCreatedAtDesc_emptyList() {
        List<Address> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId(), DEFAULT_PAGEABLE)
                .getContent();

        assertThat(addresses).isEmpty();
    }

    @Test
    @DisplayName("findByCustomerIdOrderByIsDefaultDescCreatedAtDesc - when multiple defaults, most recently created comes first")
    void findByCustomerIdOrderByIsDefaultDescCreatedAtDesc_multipleDefaults() throws InterruptedException {
        addressRepository.save(buildAddress("Work", true)); Thread.sleep(20);
        addressRepository.save(buildAddress("Home", true));
        entityManager.flush();
        entityManager.clear();

        List<Address> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId(), DEFAULT_PAGEABLE)
                .getContent();

        assertThat(addresses.get(0).getLabel()).isEqualTo("Home");
        assertThat(addresses.get(1).getLabel()).isEqualTo("Work");
    }

    @Test
    @DisplayName("findByIdAndCustomerId - returns address when both id and customerId match")
    void findByIdAndCustomerId_found() {
        Address address = addressRepository.save(buildAddress("Home", true));
        entityManager.flush();
        entityManager.clear();

        Optional<Address> result = addressRepository
                .findByIdAndCustomerId(address.getId(), customer.getId());

        assertThat(result).isPresent();
        assertThat(result).get().extracting(Address::getLabel).isEqualTo("Home");
    }

    @Test
    @DisplayName("findByIdAndCustomerId - returns empty when customerId does not match")
    void findByIdAndCustomerId_customerMismatch() {
        Address address = addressRepository.save(buildAddress("Home", true));

        Optional<Address> result = addressRepository
                .findByIdAndCustomerId(address.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndCustomerId - returns empty when address id does not exist")
    void findByIdAndCustomerId_notFound() {
        Optional<Address> result = addressRepository
                .findByIdAndCustomerId(UUID.randomUUID(), customer.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("countByCustomerId - returns correct count when customer has addresses")
    void countByCustomerId_correctCount() {
        addressRepository.saveAll(List.of(
                buildAddress("Home",  true),
                buildAddress("Work",  false),
                buildAddress("Other", false)
        ));

        long count = addressRepository.countByCustomerId(customer.getId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByCustomerId - returns zero when customer has no addresses")
    void countByCustomerId_zero() {
        long count = addressRepository.countByCustomerId(customer.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("findByCustomerIdAndIsDefaultTrue - returns the default address when one exists")
    void findByCustomerIdAndIsDefaultTrue_found() {
        addressRepository.save(buildAddress("Work",  false));
        Address defaultAddr = addressRepository.save(buildAddress("Home", true));
        addressRepository.save(buildAddress("Other", false));

        Optional<Address> result = addressRepository
                .findByCustomerIdAndIsDefaultTrue(customer.getId());

        assertThat(result).isPresent();
        assertThat(result).get().extracting(Address::getId).isEqualTo(defaultAddr.getId());
        assertThat(result).get().extracting(Address::getLabel).isEqualTo("Home");
    }

    @Test
    @DisplayName("findByCustomerIdAndIsDefaultTrue - returns empty when no address is marked default")
    void findByCustomerIdAndIsDefaultTrue_noDefault() {
        addressRepository.saveAll(List.of(
                buildAddress("Home", false),
                buildAddress("Work", false)
        ));

        Optional<Address> result = addressRepository
                .findByCustomerIdAndIsDefaultTrue(customer.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("clearDefaultExcept - sets isDefault=false for all addresses except the specified one")
    void clearDefaultExcept_clearsOthers() {
        Address addr1 = addressRepository.save(buildAddress("Home",  true));
        Address addr2 = addressRepository.save(buildAddress("Work",  true));
        Address addr3 = addressRepository.save(buildAddress("Other", true));
        entityManager.flush();

        addressRepository.clearDefaultExcept(customer.getId(), addr2.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(addressRepository.findById(addr1.getId())).get().extracting(Address::getIsDefault).isEqualTo(false);
        assertThat(addressRepository.findById(addr2.getId())).get().extracting(Address::getIsDefault).isEqualTo(true);
        assertThat(addressRepository.findById(addr3.getId())).get().extracting(Address::getIsDefault).isEqualTo(false);
    }

    @Test
    @DisplayName("clearDefaultExcept - does not change the excluded address when it is the only one")
    void clearDefaultExcept_noOtherDefaults() {
        Address addr = addressRepository.save(buildAddress("Home", true));
        entityManager.flush();

        addressRepository.clearDefaultExcept(customer.getId(), addr.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(addressRepository.findById(addr.getId())).get().extracting(Address::getIsDefault).isEqualTo(true);
    }

    @Test
    @DisplayName("clearAllDefaults - sets isDefault=false for every address belonging to the customer")
    void clearAllDefaults_clearsAll() {
        Address addr1 = addressRepository.save(buildAddress("Home",  true));
        Address addr2 = addressRepository.save(buildAddress("Work",  true));
        Address addr3 = addressRepository.save(buildAddress("Other", false));
        entityManager.flush();

        addressRepository.clearAllDefaults(customer.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(addressRepository.findById(addr1.getId())).get().extracting(Address::getIsDefault).isEqualTo(false);
        assertThat(addressRepository.findById(addr2.getId())).get().extracting(Address::getIsDefault).isEqualTo(false);
        assertThat(addressRepository.findById(addr3.getId())).get().extracting(Address::getIsDefault).isEqualTo(false);
    }

    @Test
    @DisplayName("clearAllDefaults - does nothing and leaves table empty when customer has no addresses")
    void clearAllDefaults_noAddresses() {
        addressRepository.clearAllDefaults(customer.getId());
        entityManager.flush();

        assertThat(addressRepository.count()).isZero();
    }

    @Test
    @DisplayName("save - persists address and assigns a generated id")
    void save_success() {
        Address address = buildAddress("Home", true);

        Address saved = addressRepository.save(address);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLabel()).isEqualTo("Home");
        assertThat(saved.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("delete - removes the address so it can no longer be found")
    void delete_success() {
        Address address = addressRepository.save(buildAddress("Home", true));

        addressRepository.delete(address);

        assertThat(addressRepository.findById(address.getId())).isEmpty();
    }

    private Address buildAddress(String label, boolean isDefault) {
        Address address = new Address();
        address.setCustomer(customer);
        address.setLabel(label);
        address.setBuilding("Building 1");
        address.setStreet("Main St");
        address.setCity("Casablanca");
        address.setLatitude(new BigDecimal("33.5731"));
        address.setLongitude(new BigDecimal("-7.5898"));
        address.setIsDefault(isDefault);
        return address;
    }
}