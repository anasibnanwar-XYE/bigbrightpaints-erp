package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TenantOnboardingServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountingPeriodService accountingPeriodService;

    @Mock
    private CoATemplateService coATemplateService;

    @Mock
    private EmailService emailService;

    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @Test
    void initializeDefaultSystemSettings_doesNotPersistCorsOrigin() {
        TenantOnboardingService service = new TenantOnboardingService(
                companyRepository,
                userAccountRepository,
                roleService,
                passwordEncoder,
                accountRepository,
                accountingPeriodService,
                coATemplateService,
                emailService,
                systemSettingsRepository);
        when(systemSettingsRepository.existsById(anyString())).thenReturn(false);

        Boolean changed = ReflectionTestUtils.invokeMethod(service, "initializeDefaultSystemSettings");

        assertThat(changed).isTrue();
        verify(systemSettingsRepository).save(argThat(setting ->
                setting != null
                        && "auto-approval.enabled".equals(setting.getKey())
                        && "true".equals(setting.getValue())));
        verify(systemSettingsRepository).save(argThat(setting ->
                setting != null
                        && "period-lock.enforced".equals(setting.getKey())
                        && "true".equals(setting.getValue())));
        verify(systemSettingsRepository, times(2)).save(argThat(setting -> setting != null));
        verify(systemSettingsRepository, never()).save(argThat(setting ->
                setting != null && "cors.allowed-origins".equals(setting.getKey())));
    }

    @Test
    void createTenantAdmin_usesFixedAdminRoleCatalogEntry() {
        TenantOnboardingService service = new TenantOnboardingService(
                companyRepository,
                userAccountRepository,
                roleService,
                passwordEncoder,
                accountRepository,
                accountingPeriodService,
                coATemplateService,
                emailService,
                systemSettingsRepository);
        Company company = new Company();
        company.setCode("TENANT-A");
        company.setName("Tenant A");
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        when(roleService.requireFixedSystemRole("ROLE_ADMIN")).thenReturn(adminRole);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        ReflectionTestUtils.invokeMethod(service, "createTenantAdmin", company, "admin@bbp.com", "Admin");

        verify(roleService).requireFixedSystemRole("ROLE_ADMIN");
        verify(userAccountRepository).save(argThat(user ->
                user != null
                        && user.getRoles().contains(adminRole)
                        && user.isMustChangePassword()
                        && user.getCompanies().contains(company)));
        verify(emailService).sendUserCredentialsEmail(
                org.mockito.ArgumentMatchers.eq("admin@bbp.com"),
                org.mockito.ArgumentMatchers.eq("Admin"),
                anyString(),
                org.mockito.ArgumentMatchers.eq("TENANT-A"));
    }
}
