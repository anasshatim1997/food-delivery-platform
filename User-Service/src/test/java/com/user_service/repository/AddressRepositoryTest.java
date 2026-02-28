package com.user_service.repository;

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
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Customer customer;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("customer@mail.com");
        user.setPassword("password");
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

    // -------------------------------------------------------------------------
    // findByCustomerIdOrderByIsDefaultDescCreatedAtDesc
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByCustomerIdOrderByIsDefaultDescCreatedAtDesc - default address comes first, then non-defaults ordered by date desc")
    void findByCustomerIdOrderByIsDefaultDescCreatedAtDesc_sortedCorrectly() throws InterruptedException {
        // --- Arrange ---
        // Save addresses one at a time with a small delay so @CreatedDate
        // assigns distinct timestamps (oldest → newest: Home, Work, Other).
        Address home  = addressRepository.save(buildAddress("Home",  false)); Thread.sleep(20);
        Address work  = addressRepository.save(buildAddress("Work",  true));  Thread.sleep(20);
        Address other = addressRepository.save(buildAddress("Other", false));
        entityManager.flush();
        entityManager.clear();

        // --- Act ---
        List<Address> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId());

        // --- Assert ---
        // Expected order: Work (default=true) → Other (non-default, newer) → Home (non-default, oldest)
        assertThat(addresses).hasSize(3);
        assertThat(addresses.get(0).getLabel()).isEqualTo("Work");  // only default
        assertThat(addresses.get(1).getLabel()).isEqualTo("Other"); // most recent non-default
        assertThat(addresses.get(2).getLabel()).isEqualTo("Home");  // oldest non-default
    }

    @Test
    @DisplayName("findByCustomerIdOrderByIsDefaultDescCreatedAtDesc - returns empty list when customer has no addresses")
    void findByCustomerIdOrderByIsDefaultDescCreatedAtDesc_emptyList() {
        // --- Arrange --- (no addresses saved)

        // --- Act ---
        List<Address> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId());

        // --- Assert ---
        assertThat(addresses).isEmpty();
    }

    @Test
    @DisplayName("findByCustomerIdOrderByIsDefaultDescCreatedAtDesc - when multiple defaults, most recently created comes first")
    void findByCustomerIdOrderByIsDefaultDescCreatedAtDesc_multipleDefaults() throws InterruptedException {
        // --- Arrange ---
        // Work is saved first (older), Home is saved second (newer) — both are default.
        Address work = addressRepository.save(buildAddress("Work", true)); Thread.sleep(20);
        Address home = addressRepository.save(buildAddress("Home", true));
        entityManager.flush();
        entityManager.clear();

        // --- Act ---
        List<Address> addresses = addressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(customer.getId());

        // --- Assert ---
        // Both are default=true, so the secondary sort (createdAt DESC) decides: Home > Work
        assertThat(addresses.get(0).getLabel()).isEqualTo("Home");
        assertThat(addresses.get(1).getLabel()).isEqualTo("Work");
    }

    // -------------------------------------------------------------------------
    // findByIdAndCustomerId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByIdAndCustomerId - returns address when both id and customerId match")
    void findByIdAndCustomerId_found() {
        // --- Arrange ---
        Address address = addressRepository.save(buildAddress("Home", true));
        entityManager.flush();
        entityManager.clear();

        // --- Act ---
        Optional<Address> result = addressRepository
                .findByIdAndCustomerId(address.getId(), customer.getId());

        // --- Assert ---
        assertThat(result).isPresent();
        assertThat(result.get().getLabel()).isEqualTo("Home");
    }

    @Test
    @DisplayName("findByIdAndCustomerId - returns empty when customerId does not match")
    void findByIdAndCustomerId_customerMismatch() {
        // --- Arrange ---
        Address address = addressRepository.save(buildAddress("Home", true));

        // --- Act ---
        Optional<Address> result = addressRepository
                .findByIdAndCustomerId(address.getId(), UUID.randomUUID()); // wrong customer

        // --- Assert ---
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndCustomerId - returns empty when address id does not exist")
    void findByIdAndCustomerId_notFound() {
        // --- Arrange --- (no address saved)

        // --- Act ---
        Optional<Address> result = addressRepository
                .findByIdAndCustomerId(UUID.randomUUID(), customer.getId());

        // --- Assert ---
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // countByCustomerId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("countByCustomerId - returns correct count when customer has addresses")
    void countByCustomerId_correctCount() {
        // --- Arrange ---
        addressRepository.saveAll(List.of(
                buildAddress("Home",  true),
                buildAddress("Work",  false),
                buildAddress("Other", false)
        ));

        // --- Act ---
        int count = addressRepository.countByCustomerId(customer.getId());

        // --- Assert ---
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByCustomerId - returns zero when customer has no addresses")
    void countByCustomerId_zero() {
        // --- Arrange --- (no addresses saved)

        // --- Act ---
        int count = addressRepository.countByCustomerId(customer.getId());

        // --- Assert ---
        assertThat(count).isZero();
    }

    // -------------------------------------------------------------------------
    // findByCustomerIdAndIsDefaultTrue
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByCustomerIdAndIsDefaultTrue - returns the default address when one exists")
    void findByCustomerIdAndIsDefaultTrue_found() {
        // --- Arrange ---
        addressRepository.save(buildAddress("Work",  false));
        Address defaultAddr = addressRepository.save(buildAddress("Home", true));
        addressRepository.save(buildAddress("Other", false));

        // --- Act ---
        Optional<Address> result = addressRepository
                .findByCustomerIdAndIsDefaultTrue(customer.getId());

        // --- Assert ---
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(defaultAddr.getId());
        assertThat(result.get().getLabel()).isEqualTo("Home");
    }

    @Test
    @DisplayName("findByCustomerIdAndIsDefaultTrue - returns empty when no address is marked default")
    void findByCustomerIdAndIsDefaultTrue_noDefault() {
        // --- Arrange ---
        addressRepository.saveAll(List.of(
                buildAddress("Home", false),
                buildAddress("Work", false)
        ));

        // --- Act ---
        Optional<Address> result = addressRepository
                .findByCustomerIdAndIsDefaultTrue(customer.getId());

        // --- Assert ---
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // clearDefaultExcept
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("clearDefaultExcept - sets isDefault=false for all addresses except the specified one")
    void clearDefaultExcept_clearsOthers() {
        // --- Arrange ---
        Address addr1 = addressRepository.save(buildAddress("Home",  true));
        Address addr2 = addressRepository.save(buildAddress("Work",  true)); // this one should stay default
        Address addr3 = addressRepository.save(buildAddress("Other", true));
        entityManager.flush();

        // --- Act ---
        addressRepository.clearDefaultExcept(customer.getId(), addr2.getId());
        entityManager.flush();
        entityManager.clear(); // evict stale cache so findById hits the DB

        // --- Assert ---
        assertThat(addressRepository.findById(addr1.getId()).get().getIsDefault()).isFalse();
        assertThat(addressRepository.findById(addr2.getId()).get().getIsDefault()).isTrue();
        assertThat(addressRepository.findById(addr3.getId()).get().getIsDefault()).isFalse();
    }

    @Test
    @DisplayName("clearDefaultExcept - does not change the excluded address when it is the only one")
    void clearDefaultExcept_noOtherDefaults() {
        // --- Arrange ---
        Address addr = addressRepository.save(buildAddress("Home", true));
        entityManager.flush();

        // --- Act ---
        addressRepository.clearDefaultExcept(customer.getId(), addr.getId());
        entityManager.flush();
        entityManager.clear();

        // --- Assert ---
        assertThat(addressRepository.findById(addr.getId()).get().getIsDefault()).isTrue();
    }

    // -------------------------------------------------------------------------
    // clearAllDefaults
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("clearAllDefaults - sets isDefault=false for every address belonging to the customer")
    void clearAllDefaults_clearsAll() {
        // --- Arrange ---
        Address addr1 = addressRepository.save(buildAddress("Home",  true));
        Address addr2 = addressRepository.save(buildAddress("Work",  true));
        Address addr3 = addressRepository.save(buildAddress("Other", false));
        entityManager.flush();

        // --- Act ---
        addressRepository.clearAllDefaults(customer.getId());
        entityManager.flush();
        entityManager.clear(); // evict stale cache so findById hits the DB

        // --- Assert ---
        assertThat(addressRepository.findById(addr1.getId()).get().getIsDefault()).isFalse();
        assertThat(addressRepository.findById(addr2.getId()).get().getIsDefault()).isFalse();
        assertThat(addressRepository.findById(addr3.getId()).get().getIsDefault()).isFalse();
    }

    @Test
    @DisplayName("clearAllDefaults - does nothing and leaves table empty when customer has no addresses")
    void clearAllDefaults_noAddresses() {
        // --- Arrange --- (no addresses saved)

        // --- Act ---
        addressRepository.clearAllDefaults(customer.getId());
        entityManager.flush();

        // --- Assert ---
        assertThat(addressRepository.count()).isZero();
    }

    // -------------------------------------------------------------------------
    // save / delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save - persists address and assigns a generated id")
    void save_success() {
        // --- Arrange ---
        Address address = buildAddress("Home", true);

        // --- Act ---
        Address saved = addressRepository.save(address);

        // --- Assert ---
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLabel()).isEqualTo("Home");
        assertThat(saved.getIsDefault()).isTrue();
    }

    @Test
    @DisplayName("delete - removes the address so it can no longer be found")
    void delete_success() {
        // --- Arrange ---
        Address address = addressRepository.save(buildAddress("Home", true));

        // --- Act ---
        addressRepository.delete(address);

        // --- Assert ---
        assertThat(addressRepository.findById(address.getId())).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Builds an unsaved Address entity associated with the shared {@code customer}.
     * createdAt / updatedAt are intentionally left unset — the @CreatedDate /
     * @LastModifiedDate auditing listener will populate them on save.
     */
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