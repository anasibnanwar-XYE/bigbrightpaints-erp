package com.bigbrightpaints.erp.core.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("critical")
@ExtendWith(MockitoExtension.class)
class DataInitializerSecurityTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void mockInitializer_requiresPasswordWhenSeedingNewAdmin() {
        MockDataInitializer initializer = new MockDataInitializer();
        Company company = company("MOCK");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role("ROLE_ADMIN")));
        when(roleRepository.findByName("ROLE_ACCOUNTING")).thenReturn(Optional.of(role("ROLE_ACCOUNTING")));
        when(roleRepository.findByName("ROLE_SALES")).thenReturn(Optional.of(role("ROLE_SALES")));
        when(userRepository.findByEmailIgnoreCase("mock.admin@bbp.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                initializer,
                "seedRolesAndUsers",
                roleRepository,
                userRepository,
                passwordEncoder,
                company,
                "mock.admin@bbp.com",
                "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("erp.seed.mock-admin.password is required");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void benchmarkInitializer_requiresPasswordWhenSeedingNewAdmin() {
        BenchmarkDataInitializer initializer = new BenchmarkDataInitializer();
        Company company = company("BBP");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role("ROLE_ADMIN")));
        when(roleRepository.findByName("ROLE_ACCOUNTING")).thenReturn(Optional.of(role("ROLE_ACCOUNTING")));
        when(roleRepository.findByName("ROLE_SALES")).thenReturn(Optional.of(role("ROLE_SALES")));
        when(roleRepository.findByName("ROLE_FACTORY")).thenReturn(Optional.of(role("ROLE_FACTORY")));
        when(userRepository.findByEmailIgnoreCase("benchmark.admin@bbp.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                initializer,
                "seedRolesAndUsers",
                roleRepository,
                userRepository,
                passwordEncoder,
                company,
                "benchmark.admin@bbp.com",
                " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("erp.seed.benchmark-admin.password is required");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void mockInitializer_doesNotRequirePasswordWhenAdminAlreadyExists() {
        MockDataInitializer initializer = new MockDataInitializer();
        Company company = company("MOCK");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role("ROLE_ADMIN")));
        when(roleRepository.findByName("ROLE_ACCOUNTING")).thenReturn(Optional.of(role("ROLE_ACCOUNTING")));
        when(roleRepository.findByName("ROLE_SALES")).thenReturn(Optional.of(role("ROLE_SALES")));
        when(userRepository.findByEmailIgnoreCase("mock.admin@bbp.com"))
                .thenReturn(Optional.of(new UserAccount("mock.admin@bbp.com", "existing-hash", "Mock Admin")));

        ReflectionTestUtils.invokeMethod(
                initializer,
                "seedRolesAndUsers",
                roleRepository,
                userRepository,
                passwordEncoder,
                company,
                "mock.admin@bbp.com",
                "   ");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void benchmarkInitializer_doesNotRequirePasswordWhenAdminAlreadyExists() {
        BenchmarkDataInitializer initializer = new BenchmarkDataInitializer();
        Company company = company("BBP");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role("ROLE_ADMIN")));
        when(roleRepository.findByName("ROLE_ACCOUNTING")).thenReturn(Optional.of(role("ROLE_ACCOUNTING")));
        when(roleRepository.findByName("ROLE_SALES")).thenReturn(Optional.of(role("ROLE_SALES")));
        when(roleRepository.findByName("ROLE_FACTORY")).thenReturn(Optional.of(role("ROLE_FACTORY")));
        when(userRepository.findByEmailIgnoreCase("benchmark.admin@bbp.com"))
                .thenReturn(Optional.of(new UserAccount("benchmark.admin@bbp.com", "existing-hash", "Benchmark Admin")));

        ReflectionTestUtils.invokeMethod(
                initializer,
                "seedRolesAndUsers",
                roleRepository,
                userRepository,
                passwordEncoder,
                company,
                "benchmark.admin@bbp.com",
                "   ");

        verify(passwordEncoder, never()).encode(anyString());
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private Company company(String code) {
        Company company = new Company();
        company.setCode(code);
        company.setName(code);
        company.setTimezone("UTC");
        return company;
    }
}
