package com.user_service.repository;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("findByUserId - returns customer when exists")
    void findByUserId_found() {
        User user = createAndSaveUser("customer@mail.com");
        Customer customer = createAndSaveCustomer(user, "John", "Doe");

        Optional<Customer> result = customerRepository.findByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(customer.getId());
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getFirstName()).isEqualTo("John");
        assertThat(result.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("findByUserId - returns empty when not found")
    void findByUserId_notFound() {
        Optional<Customer> result = customerRepository.findByUserId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserId - returns empty when user has no customer profile")
    void findByUserId_userExistsButNoCustomer() {
        User user = createAndSaveUser("driver@mail.com");

        Optional<Customer> result = customerRepository.findByUserId(user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save - persists customer successfully")
    void save_success() {
        User user = createAndSaveUser("new@mail.com");
        Customer customer = buildCustomer(user, "Jane", "Smith");

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Smith");
        assertThat(saved.getWalletBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(saved.getTotalOrders()).isZero();
    }

    @Test
    @DisplayName("save - updates existing customer")
    void save_update() {
        User user = createAndSaveUser("update@mail.com");
        Customer customer = createAndSaveCustomer(user, "Old", "Name");

        customer.setFirstName("New");
        customer.setLastName("Name");
        customer.setTotalOrders(5);
        customer.setWalletBalance(new BigDecimal("100.00"));

        Customer updated = customerRepository.save(customer);

        assertThat(updated.getFirstName()).isEqualTo("New");
        assertThat(updated.getLastName()).isEqualTo("Name");
        assertThat(updated.getTotalOrders()).isEqualTo(5);
        assertThat(updated.getWalletBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("delete - removes customer")
    void delete_success() {
        User user = createAndSaveUser("delete@mail.com");
        Customer customer = createAndSaveCustomer(user, "Delete", "Me");

        customerRepository.delete(customer);

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    @DisplayName("findById - returns customer when exists")
    void findById_found() {
        User user = createAndSaveUser("findid@mail.com");
        Customer customer = createAndSaveCustomer(user, "Find", "Me");

        Optional<Customer> result = customerRepository.findById(customer.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("findById - returns empty when not found")
    void findById_notFound() {
        Optional<Customer> result = customerRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save - cascades to user relationship")
    void save_cascadesToUser() {
        User user = createAndSaveUser("cascade@mail.com");
        Customer customer = buildCustomer(user, "Cascade", "Test");

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getUser()).isNotNull();
        assertThat(saved.getUser().getEmail()).isEqualTo("cascade@mail.com");
    }

    @Test
    @DisplayName("findByUserId - handles multiple customers")
    void findByUserId_multipleCustomers() {
        User user1 = createAndSaveUser("customer1@mail.com");
        User user2 = createAndSaveUser("customer2@mail.com");
        Customer customer1 = createAndSaveCustomer(user1, "Customer", "One");
        Customer customer2 = createAndSaveCustomer(user2, "Customer", "Two");

        Optional<Customer> result1 = customerRepository.findByUserId(user1.getId());
        Optional<Customer> result2 = customerRepository.findByUserId(user2.getId());

        assertThat(result1).isPresent();
        assertThat(result1.get().getId()).isEqualTo(customer1.getId());
        assertThat(result2).isPresent();
        assertThat(result2.get().getId()).isEqualTo(customer2.getId());
    }

    @Test
    @DisplayName("save - persists profile image")
    void save_withProfileImage() {
        User user = createAndSaveUser("image@mail.com");
        Customer customer = buildCustomer(user, "Image", "Test");
        customer.setProfileImage("https://example.com/image.jpg");

        Customer saved = customerRepository.save(customer);

        assertThat(saved.getProfileImage()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("save - persists default address reference")
    void save_withDefaultAddress() {
        User user = createAndSaveUser("address@mail.com");
        Customer customer = createAndSaveCustomer(user, "Address", "Test");

        UUID defaultAddressId = UUID.randomUUID();
        customer.setDefaultAddressId(defaultAddressId);
        Customer updated = customerRepository.save(customer);

        assertThat(updated.getDefaultAddressId()).isEqualTo(defaultAddressId);
    }

    @Test
    @DisplayName("count - returns correct count")
    void count_correctCount() {
        User user1 = createAndSaveUser("count1@mail.com");
        User user2 = createAndSaveUser("count2@mail.com");
        createAndSaveCustomer(user1, "One", "Customer");
        createAndSaveCustomer(user2, "Two", "Customer");

        long count = customerRepository.count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsById - returns true when exists")
    void existsById_true() {
        User user = createAndSaveUser("exists@mail.com");
        Customer customer = createAndSaveCustomer(user, "Exists", "Test");

        boolean exists = customerRepository.existsById(customer.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById - returns false when not exists")
    void existsById_false() {
        boolean exists = customerRepository.existsById(UUID.randomUUID());
        assertThat(exists).isFalse();
    }

    private User createAndSaveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("SecurePass1!");
        user.setRole(Role.CUSTOMER);
        user.setStatus(Status.ACTIVE);
        user.setIsVerified(true);
        user.setProfileCompleted(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Customer buildCustomer(User user, String firstName, String lastName) {
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);
        return customer;
    }

    private Customer createAndSaveCustomer(User user, String firstName, String lastName) {
        return customerRepository.save(buildCustomer(user, firstName, lastName));
    }
}