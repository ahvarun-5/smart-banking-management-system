package com.smartbank.repository;

import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new User(
                "Varun Sharma",
                "varun.sharma@example.com",
                "9876543210",
                "$2a$10$hashedpasswordstringsample",
                "123 Innovation Drive, Tech Park",
                Role.ROLE_CUSTOMER
        );
    }

    @Test
    @DisplayName("Should save user and retrieve by email successfully")
    void testSaveAndFindByEmail() {
        User savedUser = userRepository.save(sampleCustomer);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        Optional<User> foundUser = userRepository.findByEmail("varun.sharma@example.com");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFullName()).isEqualTo("Varun Sharma");
        assertThat(foundUser.get().getRole()).isEqualTo(Role.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("Should check existence by email")
    void testExistsByEmail() {
        userRepository.save(sampleCustomer);

        boolean exists = userRepository.existsByEmail("varun.sharma@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should count users by role")
    void testCountByRole() {
        userRepository.save(sampleCustomer);

        User adminUser = new User(
                "Admin User",
                "admin.user@example.com",
                "9898989898",
                "$2a$10$hashedpasswordstringsample",
                "Admin Office, Central Tower",
                Role.ROLE_ADMIN
        );
        userRepository.save(adminUser);

        long customerCount = userRepository.countByRole(Role.ROLE_CUSTOMER);
        long adminCount = userRepository.countByRole(Role.ROLE_ADMIN);

        assertThat(customerCount).isEqualTo(1L);
        assertThat(adminCount).isEqualTo(1L);

        long activeCustomerCount = userRepository.countByRoleAndEnabled(Role.ROLE_CUSTOMER, true);
        long inactiveCustomerCount = userRepository.countByRoleAndEnabled(Role.ROLE_CUSTOMER, false);
        assertThat(activeCustomerCount).isEqualTo(1L);
        assertThat(inactiveCustomerCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should search users by name or email")
    void testSearchUsers() {
        userRepository.save(sampleCustomer);

        Page<User> result = userRepository.searchUsers("Varun", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Varun Sharma");

        Page<User> emailResult = userRepository.searchUsers("varun.sharma", PageRequest.of(0, 10));
        assertThat(emailResult.getContent()).hasSize(1);
    }
}
