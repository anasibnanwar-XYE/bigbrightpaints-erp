package com.bigbrightpaints.erp.modules.admin.service;

import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.admin.dto.CreateUserRequest;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.service.RefreshTokenService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import com.bigbrightpaints.erp.modules.rbac.service.RoleService;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private CompanyContextService companyContextService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private DealerRepository dealerRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TenantRuntimePolicyService tenantRuntimePolicyService;

    private AdminUserService service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(
                userRepository,
                companyContextService,
                roleRepository,
                roleService,
                passwordEncoder,
                emailService,
                tokenBlacklistService,
                refreshTokenService,
                dealerRepository,
                accountRepository,
                tenantRuntimePolicyService
        );
        company = new Company();
        ReflectionTestUtils.setField(company, "id", 1L);
        company.setCode("TEST");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            if (user.getId() == null) {
                ReflectionTestUtils.setField(user, "id", 200L);
            }
            return user;
        });
        when(roleService.ensureRoleExists("ROLE_DEALER")).thenAnswer(invocation -> {
            Role role = new Role();
            role.setName("ROLE_DEALER");
            return role;
        });
        when(dealerRepository.save(any(Dealer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createUser_relinksExistingDealerByEmailAndReactivatesReceivableAccount() {
        Dealer existingDealer = new Dealer();
        existingDealer.setCompany(company);
        ReflectionTestUtils.setField(existingDealer, "id", 44L);
        existingDealer.setCode("LEGACY44");
        existingDealer.setName("Legacy Dealer");
        existingDealer.setStatus("INACTIVE");
        existingDealer.setEmail("dealer@example.com");

        Account receivable = new Account();
        receivable.setCompany(company);
        receivable.setCode("AR-LEGACY44");
        receivable.setActive(false);
        existingDealer.setReceivableAccount(receivable);

        when(dealerRepository.findByCompanyAndPortalUserEmail(company, "dealer@example.com"))
                .thenReturn(Optional.empty());
        when(dealerRepository.findByCompanyAndEmailIgnoreCase(company, "dealer@example.com"))
                .thenReturn(Optional.of(existingDealer));

        service.createUser(new CreateUserRequest(
                "dealer@example.com",
                "Password@123",
                "Dealer User",
                List.of(1L),
                List.of("ROLE_DEALER")
        ));

        ArgumentCaptor<Dealer> dealerCaptor = ArgumentCaptor.forClass(Dealer.class);
        verify(dealerRepository).save(dealerCaptor.capture());
        Dealer savedDealer = dealerCaptor.getValue();
        assertThat(savedDealer.getId()).isEqualTo(44L);
        assertThat(savedDealer.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedDealer.getPortalUser()).isNotNull();
        assertThat(savedDealer.getPortalUser().getEmail()).isEqualTo("dealer@example.com");
        assertThat(receivable.isActive()).isTrue();
        verify(accountRepository).save(receivable);
    }
}
