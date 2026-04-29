package com.bigbrightpaints.erp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

@TestPropertySource(properties = "erp.security.swagger-public=true")
public class OpenApiSnapshotIT extends AbstractIntegrationTest {

  private static final String SNAPSHOT_VERIFY_PROPERTY = "erp.openapi.snapshot.verify";
  private static final String SNAPSHOT_VERIFY_ENV = "ERP_OPENAPI_SNAPSHOT_VERIFY";
  private static final String SNAPSHOT_REFRESH_PROPERTY = "erp.openapi.snapshot.refresh";
  private static final String SNAPSHOT_REFRESH_ENV = "ERP_OPENAPI_SNAPSHOT_REFRESH";
  private static final ObjectMapper CANONICAL_JSON =
      new ObjectMapper()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  @Autowired private TestRestTemplate rest;

  @Test
  void auth_and_admin_contract_paths_preserve_expected_response_shapes() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/auth/login",
        "post",
        "#/components/schemas/LoginRequest",
        "200",
        "#/components/schemas/AuthResponse");
    assertOperationContract(
        root,
        "/api/v1/auth/refresh-token",
        "post",
        "#/components/schemas/RefreshTokenRequest",
        "200",
        "#/components/schemas/AuthResponse");
    assertOperationContract(
        root, "/api/v1/auth/logout", "post", "#/components/schemas/LogoutRequest", "204", null);
    assertOperationContract(
        root, "/api/v1/auth/me", "get", null, "200", "#/components/schemas/ApiResponseMeResponse");
    assertOperationContract(
        root,
        "/api/v1/auth/me/profile",
        "patch",
        "#/components/schemas/SelfProfileRequest",
        "200",
        "#/components/schemas/ApiResponseSelfProfileResponse");
    assertOperationContract(
        root,
        "/api/v1/auth/me/contact",
        "patch",
        "#/components/schemas/SelfContactRequest",
        "200",
        "#/components/schemas/ApiResponseSelfContactResponse");
    assertOperationContract(
        root,
        "/api/v1/auth/me/security",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSelfSecuritySummaryResponse");
    assertOperationContract(
        root,
        "/api/v1/auth/me/security-events",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePageResponseMapStringObject");
    assertOperationContract(
        root,
        "/api/v1/auth/password/change",
        "post",
        "#/components/schemas/ChangePasswordRequest",
        "200",
        "#/components/schemas/ApiResponseString");
    assertOperationContract(
        root,
        "/api/v1/auth/password/forgot",
        "post",
        "#/components/schemas/ForgotPasswordRequest",
        "200",
        "#/components/schemas/ApiResponseString");
    assertOperationMissing(root, "/api/v1/auth/password/forgot/superadmin", "post");
    assertOperationContract(
        root,
        "/api/v1/auth/password/reset",
        "post",
        "#/components/schemas/ResetPasswordRequest",
        "200",
        "#/components/schemas/ApiResponseString");
    assertOperationContract(
        root,
        "/api/v1/auth/mfa",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseMapStringObject");
    assertOperationContract(
        root,
        "/api/v1/auth/mfa/recovery-codes/regenerate",
        "post",
        "#/components/schemas/MfaDisableRequest",
        "200",
        "#/components/schemas/ApiResponseMapStringObject");
    assertOperationContract(
        root,
        "/api/v1/auth/sessions",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListMapStringObject");
    assertOperationContract(root, "/api/v1/auth/sessions/{sessionId}", "delete", null, "204", null);
    assertOperationContract(root, "/api/v1/auth/sessions/current", "delete", null, "204", null);
    assertOperationContract(root, "/api/v1/auth/sessions", "delete", null, "204", null);
    assertOperationContract(
        root,
        "/api/v1/auth/activation/verify",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseActivationVerifyResponse");
    assertOperationContract(
        root,
        "/api/v1/auth/activation/complete",
        "post",
        "#/components/schemas/ActivationCompleteRequest",
        "200",
        "#/components/schemas/ApiResponseActivationCompleteResponse");
    assertOperationContract(
        root,
        "/api/v1/setup/status",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseOwnerSetupStatusResponse");
    assertOperationContract(
        root,
        "/api/v1/setup/company-details",
        "put",
        "#/components/schemas/OwnerSetupCompanyDetailsRequest",
        "200",
        "#/components/schemas/ApiResponseOwnerSetupStatusResponse");
    assertOperationContract(
        root,
        "/api/v1/setup/gst",
        "put",
        "#/components/schemas/OwnerSetupGstRequest",
        "200",
        "#/components/schemas/ApiResponseOwnerSetupStatusResponse");
    assertOperationContract(
        root,
        "/api/v1/setup/accounting",
        "put",
        "#/components/schemas/OwnerSetupAccountingRequest",
        "200",
        "#/components/schemas/ApiResponseOwnerSetupStatusResponse");
    assertOperationContract(
        root,
        "/api/v1/setup/invite-team",
        "post",
        "#/components/schemas/OwnerSetupInviteTeamRequest",
        "200",
        "#/components/schemas/ApiResponseOwnerSetupStatusResponse");
    assertOperationContract(
        root,
        "/api/v1/setup/finish",
        "post",
        "#/components/schemas/OwnerSetupFinishRequest",
        "200",
        "#/components/schemas/ApiResponseOwnerSetupStatusResponse");
    assertOperationMissing(root, "/api/v1/auth/profile", "get");
    assertOperationMissing(root, "/api/v1/auth/profile", "post");
    assertOperationMissing(root, "/api/v1/auth/profile", "put");
    assertOperationMissing(root, "/api/v1/auth/profile", "patch");
    assertOperationMissing(root, "/api/v1/auth/profile", "delete");
    assertThat(root.path("components").path("schemas").has("SelfProfileResponse")).isTrue();
    assertThat(root.path("components").path("schemas").has("SelfProfileRequest")).isTrue();
    assertThat(root.path("components").path("schemas").has("ApiResponseSelfProfileResponse"))
        .isTrue();

    assertOperationContract(
        root,
        "/api/v1/superadmin/settings",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminPlatformSettingsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/settings",
        "put",
        "#/components/schemas/SuperAdminPlatformSettingsUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminPlatformSettingsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/profile",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminProfileDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/profile",
        "put",
        "#/components/schemas/SuperAdminProfileUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminProfileDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/profile/password",
        "post",
        "#/components/schemas/ChangePasswordRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminPasswordChangeResponseDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/profile/sessions",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListSuperAdminProfileSessionDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/profile/sessions/{sessionId}/revoke",
        "post",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminSessionRevokeResponseDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/roles",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListRoleDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/roles",
        "post",
        "#/components/schemas/CreateRoleRequest",
        "200",
        "#/components/schemas/ApiResponseRoleDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/roles/{roleKey}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseRoleDto");
    assertOperationMissing(root, "/api/v1/admin/settings", "get");
    assertOperationMissing(root, "/api/v1/admin/settings", "put");
    assertOperationMissing(root, "/api/v1/admin/roles", "get");
    assertOperationMissing(root, "/api/v1/admin/roles", "post");
    assertOperationMissing(root, "/api/v1/admin/roles/{roleKey}", "get");
    assertOperationContract(
        root,
        "/api/v1/admin/approvals",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseAdminApprovalInboxResponse");
    assertOperationContract(
        root,
        "/api/v1/admin/approvals/{originType}/{id}/decisions",
        "post",
        "#/components/schemas/AdminApprovalDecisionRequest",
        "200",
        "#/components/schemas/ApiResponseAdminApprovalItemDto");
    JsonNode approvalDecisionOperation =
        root.path("paths").path("/api/v1/admin/approvals/{originType}/{id}/decisions").path("post");
    assertThat(approvalDecisionOperation.path("description").asText(""))
        .contains("PERIOD_CLOSE_REQUEST")
        .contains("require");
    assertOperationContract(
        root,
        "/api/v1/admin/dashboard",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseAdminDashboardDto");
    assertOperationContract(
        root,
        "/api/v1/admin/self/settings",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseAdminSelfSettingsDto");
    assertOperationMissing(root, "/api/v1/admin/exports/pending", "get");
    assertOperationMissing(root, "/api/v1/superadmin/tenants/onboard", "post");
    assertOperationContract(
        root,
        "/api/v1/superadmin/dashboard",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseCompanySuperAdminDashboardDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePageResponseSuperAdminTenantSummaryDto");
    assertQueryParameter(root, "/api/v1/superadmin/tenants", "get", "status");
    assertQueryParameter(root, "/api/v1/superadmin/tenants", "get", "q");
    assertQueryParameter(root, "/api/v1/superadmin/tenants", "get", "page");
    assertQueryParameter(root, "/api/v1/superadmin/tenants", "get", "size");
    assertQueryParameter(root, "/api/v1/superadmin/tenants", "get", "sort");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/new",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminAddClientOptionsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants",
        "post",
        "#/components/schemas/SuperAdminAddClientCreateRequest",
        "201",
        "#/components/schemas/ApiResponseSuperAdminAddClientCreateResponse");
    JsonNode addClientCreateResponses =
        root.path("paths").path("/api/v1/superadmin/tenants").path("post").path("responses");
    assertThat(addClientCreateResponses.has("200"))
        .withFailMessage(
            "POST /api/v1/superadmin/tenants must not document a contradictory 200 success"
                + " response when runtime creates clients with 201 Created")
        .isFalse();
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantDetailDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/usage",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePlatformUsage");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/usage",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseTenantUsage");
    assertThat(
            root.path("components")
                .path("schemas")
                .path("TenantUsage")
                .path("properties")
                .has("operationalDimensions"))
        .withFailMessage(
            "Tenant usage schema must expose operationalDimensions for runtime API window,"
                + " rejected-request, and in-flight/concurrent readback")
        .isTrue();
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/usage/history",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseTenantUsageHistory");
    assertQueryParameter(
        root, "/api/v1/superadmin/tenants/{id}/usage/history", "get", "periodType");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/quota-policy",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseTenantQuotaPolicy");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/quota-check",
        "post",
        "#/components/schemas/QuotaActionRequest",
        "200",
        "#/components/schemas/ApiResponseQuotaActionResult");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/activation/send",
        "post",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminActivationActionResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/activation/resend",
        "post",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminActivationActionResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/activation/copy",
        "post",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminActivationCopyResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/activation/expire",
        "post",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminActivationActionResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/lifecycle",
        "put",
        "#/components/schemas/CompanyLifecycleStateRequest",
        "200",
        "#/components/schemas/ApiResponseCompanyLifecycleStateDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/limits",
        "put",
        "#/components/schemas/TenantLimitsUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantLimitsDto");
    assertThat(
            root.path("components")
                .path("schemas")
                .path("SuperAdminTenantLimitsDto")
                .path("properties")
                .has("burstRequestsPerMinute"))
        .isTrue();
    assertThat(
            root.path("components")
                .path("schemas")
                .path("Limits")
                .path("properties")
                .has("burstRequestsPerMinute"))
        .isTrue();
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/modules",
        "put",
        "#/components/schemas/TenantModulesUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseCompanyEnabledModulesDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/entitlements",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantEntitlementsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/plan",
        "put",
        "#/components/schemas/SuperAdminTenantPlanAssignmentRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantEntitlementsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/entitlements/overrides",
        "put",
        "#/components/schemas/SuperAdminTenantEntitlementOverrideRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantEntitlementsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/entitlements/overrides/{key}",
        "delete",
        "#/components/schemas/TenantEntitlementOverrideRemoveRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantEntitlementsDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/support/warnings",
        "post",
        "#/components/schemas/TenantSupportWarningRequest",
        "200",
        "#/components/schemas/ApiResponseCompanySupportWarningDto");
    assertOperationMissing(
        root, "/api/v1/superadmin/tenants/{id}/support/admin-password-reset", "post");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/support/context",
        "put",
        "#/components/schemas/TenantSupportContextUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantSupportContextDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePageResponseQueueItem");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "status");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "q");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "page");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "size");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "sort");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "category");
    assertQueryParameter(root, "/api/v1/superadmin/support/tickets", "get", "slaStatus");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/sla/refresh",
        "post",
        "#/components/schemas/SlaRefreshRequest",
        "200",
        "#/components/schemas/ApiResponseSlaRefreshResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseDetail");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}/messages",
        "post",
        "#/components/schemas/SupportTicketMessageRequest",
        "200",
        "#/components/schemas/ApiResponseSupportTicketMessageResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}/messages",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePageResponseSupportTicketMessageResponse");
    assertQueryParameter(
        root, "/api/v1/superadmin/support/tickets/{ticketId}/messages", "get", "page");
    assertQueryParameter(
        root, "/api/v1/superadmin/support/tickets/{ticketId}/messages", "get", "size");
    assertQueryParameter(
        root, "/api/v1/superadmin/support/tickets/{ticketId}/messages", "get", "includeInternal");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}/internal-notes",
        "post",
        "#/components/schemas/SupportTicketMessageRequest",
        "200",
        "#/components/schemas/ApiResponseSupportTicketMessageResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}/status",
        "post",
        "#/components/schemas/StatusUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseDetail");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}/convert-to-incident",
        "post",
        "#/components/schemas/ConvertToIncidentRequest",
        "200",
        "#/components/schemas/ApiResponseDetail");
    assertOperationContract(
        root,
        "/api/v1/superadmin/support/tickets/{ticketId}/timeline",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListTimelineItem");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/force-logout",
        "post",
        "#/components/schemas/TenantForceLogoutRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantForceLogoutDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/admins/main",
        "put",
        "#/components/schemas/TenantMainAdminUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseMainAdminSummaryDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/request",
        "post",
        "#/components/schemas/TenantAdminEmailChangeRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantAdminEmailChangeRequestDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/confirm",
        "post",
        "#/components/schemas/TenantAdminEmailChangeConfirmRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminTenantAdminEmailChangeConfirmationDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/plans",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListSuperAdminPlanTemplateDto");
    assertQueryParameter(root, "/api/v1/superadmin/plans", "get", "includeArchived");
    assertOperationContract(
        root,
        "/api/v1/superadmin/plans",
        "post",
        "#/components/schemas/SuperAdminPlanTemplateCreateRequest",
        "201",
        "#/components/schemas/ApiResponseSuperAdminPlanTemplateDto");
    assertOperationContract(
        root,
        "/api/v1/superadmin/plans/{stableId}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSuperAdminPlanTemplateDto");
    assertQueryParameter(root, "/api/v1/superadmin/plans/{stableId}", "get", "version");
    assertQueryParameter(root, "/api/v1/superadmin/plans/{stableId}", "get", "includeArchived");
    assertOperationContract(
        root,
        "/api/v1/superadmin/plans/{stableId}",
        "put",
        "#/components/schemas/SuperAdminPlanTemplateUpdateRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminPlanTemplateDto");
    assertPlanDefaultLimitsRequestSchema(root);
    assertOperationContract(
        root,
        "/api/v1/superadmin/plans/{stableId}/archive",
        "post",
        "#/components/schemas/SuperAdminPlanTemplateArchiveRequest",
        "200",
        "#/components/schemas/ApiResponseSuperAdminPlanTemplateDto");
    assertThat(root.path("paths").has("/api/v1/superadmin/plan-templates")).isFalse();
    assertThat(root.path("paths").has("/api/v1/superadmin/plan-templates/{stableId}")).isFalse();
    assertThat(root.path("paths").has("/api/v1/superadmin/plan-templates/{stableId}/archive"))
        .isFalse();
    assertOperationContract(
        root,
        "/api/v1/changelog",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePageResponseChangelogEntryResponse");
    assertQueryParameter(root, "/api/v1/changelog", "get", "page");
    assertQueryParameter(root, "/api/v1/changelog", "get", "size");
    assertOperationContract(
        root,
        "/api/v1/changelog/latest-highlighted",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseChangelogEntryResponse");
    assertOperationContract(
        root,
        "/api/v1/accounting/periods/{periodId}/request-close",
        "post",
        "#/components/schemas/PeriodCloseRequestActionRequest",
        "200",
        "#/components/schemas/ApiResponsePeriodCloseRequestDto");
    assertOperationContract(
        root,
        "/api/v1/accounting/periods/{periodId}/approve-close",
        "post",
        "#/components/schemas/PeriodCloseRequestActionRequest",
        "200",
        "#/components/schemas/ApiResponseAccountingPeriodDto");
    assertOperationContract(
        root,
        "/api/v1/accounting/periods/{periodId}/reject-close",
        "post",
        "#/components/schemas/PeriodCloseRequestActionRequest",
        "200",
        "#/components/schemas/ApiResponsePeriodCloseRequestDto");
    assertOperationContract(
        root,
        "/api/v1/accounting/periods/{periodId}/reopen",
        "post",
        "#/components/schemas/AccountingPeriodReopenRequest",
        "200",
        "#/components/schemas/ApiResponseAccountingPeriodDto");
    assertOperationContract(
        root,
        "/api/v1/exports/request",
        "post",
        "#/components/schemas/ExportRequestCreateRequest",
        "201",
        "#/components/schemas/ApiResponseExportRequestDto");
    assertBinaryOperationResponse(root, "/api/v1/exports/{requestId}/download", "get", "200");
    assertOperationContract(
        root,
        "/api/v1/superadmin/changelog",
        "post",
        "#/components/schemas/ChangelogEntryRequest",
        "200",
        "#/components/schemas/ApiResponseChangelogEntryResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/changelog/{id}",
        "put",
        "#/components/schemas/ChangelogEntryRequest",
        "200",
        "#/components/schemas/ApiResponseChangelogEntryResponse");
    assertOperationContract(
        root,
        "/api/v1/superadmin/changelog/{id}",
        "delete",
        null,
        "200",
        "#/components/schemas/ApiResponseVoid");
    assertOperationMissing(root, "/api/v1/admin/tenant-runtime/metrics", "get");
    assertOperationMissing(root, "/api/v1/admin/tenant-runtime/policy", "put");
    assertOperationMissing(root, "/api/v1/admin/changelog", "post");
    assertOperationMissing(root, "/api/v1/admin/changelog/{id}", "put");
    assertOperationMissing(root, "/api/v1/admin/changelog/{id}", "delete");
    assertOperationMissing(root, "/api/v1/companies", "post");
    assertOperationMissing(root, "/api/v1/companies/{id}", "delete");
    assertOperationMissing(root, "/api/v1/companies/{id}/lifecycle-state", "put");
    assertOperationMissing(root, "/api/v1/companies/{id}/tenant-metrics", "get");
    assertOperationMissing(root, "/api/v1/companies/{id}/tenant-runtime/policy", "put");
    assertOperationMissing(root, "/api/v1/companies/{id}/support/admin-password-reset", "post");
    assertOperationMissing(root, "/api/v1/companies/{id}/support/warnings", "post");
    assertOperationMissing(root, "/api/v1/companies/superadmin/tenants", "post");
    assertOperationMissing(root, "/api/v1/companies/superadmin/tenants/{id}", "put");
    assertOperationMissing(root, "/api/v1/superadmin/tenants/{id}/activate", "post");
    assertOperationMissing(root, "/api/v1/superadmin/tenants/{id}/deactivate", "post");
    assertOperationMissing(root, "/api/v1/superadmin/tenants/{id}/suspend", "post");
    assertOperationMissing(root, "/api/v1/superadmin/tenants/{id}/lifecycle-state", "put");
    assertSchemaPresence(root, "TenantOnboardingRequest", false);
    assertSchemaPresence(root, "TenantOnboardingResponse", false);
    assertSchemaPresence(root, "TenantAdminPasswordResetRequest", false);
    assertSchemaPresence(root, "CompanyAdminCredentialResetDto", false);
    assertOperationContract(
        root,
        "/api/v1/admin/users/{userId}/force-reset-password",
        "post",
        null,
        "200",
        "#/components/schemas/ApiResponseString");
    assertOperationContract(
        root,
        "/api/v1/admin/users/{userId}/status",
        "put",
        "#/components/schemas/UpdateUserStatusRequest",
        "200",
        "#/components/schemas/ApiResponseUserDto");
    assertOperationContract(root, "/api/v1/admin/users/{userId}/lock", "post", null, "204", null);
    assertOperationContract(root, "/api/v1/admin/users/{userId}/unlock", "post", null, "204", null);
    assertOperationMissing(root, "/api/v1/admin/users/{id}/suspend", "patch");
    assertOperationMissing(root, "/api/v1/admin/users/{id}/unsuspend", "patch");
    assertOperationContract(
        root, "/api/v1/admin/users/{id}/mfa/disable", "patch", null, "204", null);
    assertOperationContract(
        root, "/api/v1/admin/users/{userId}/sessions", "delete", null, "204", null);
    assertOperationContract(
        root,
        "/api/v1/admin/users/{userId}/security-events",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListMapStringObject");
    assertOperationContract(
        root,
        "/api/v1/admin/users/assignable-roles",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListString");
    assertOperationContract(
        root,
        "/api/v1/admin/users/{id}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseUserDto");
    assertOperationContract(
        root,
        "/api/v1/admin/users/{id}",
        "put",
        "#/components/schemas/UpdateUserRequest",
        "200",
        "#/components/schemas/ApiResponseUserDto");
    assertOperationMissing(root, "/api/v1/admin/users/{id}", "delete");

    assertOperationContract(
        root,
        "/api/v1/dealer-portal/credit-limit-requests",
        "post",
        "#/components/schemas/DealerPortalCreditLimitRequestCreateRequest",
        "201",
        "#/components/schemas/ApiResponseCreditLimitRequestDto");

    assertOperationContract(
        root,
        "/api/v1/sales/orders",
        "post",
        "#/components/schemas/SalesOrderRequest",
        "200",
        "#/components/schemas/ApiResponseSalesOrderDto");
    assertOperationResponse(
        root,
        "/api/v1/sales/orders",
        "post",
        "201",
        "#/components/schemas/ApiResponseSalesOrderDto");
    assertOperationResponse(
        root,
        "/api/v1/sales/orders",
        "post",
        "422",
        "#/components/schemas/ApiResponseMapStringObject");
  }

  @Test
  void superadmin_route_inventory_classifies_canonical_and_retired_v1_routes() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    List<String> canonicalRoutes =
        List.of(
            "GET /api/v1/superadmin/dashboard",
            "GET /api/v1/superadmin/tenants",
            "GET /api/v1/superadmin/tenants/new",
            "POST /api/v1/superadmin/tenants",
            "GET /api/v1/superadmin/tenants/{id}",
            "GET /api/v1/superadmin/usage",
            "GET /api/v1/superadmin/tenants/{id}/usage",
            "GET /api/v1/superadmin/tenants/{id}/usage/history",
            "GET /api/v1/superadmin/tenants/{id}/quota-policy",
            "POST /api/v1/superadmin/tenants/{id}/quota-check",
            "PUT /api/v1/superadmin/tenants/{id}/lifecycle",
            "PUT /api/v1/superadmin/tenants/{id}/limits",
            "PUT /api/v1/superadmin/tenants/{id}/modules",
            "GET /api/v1/superadmin/tenants/{id}/entitlements",
            "PUT /api/v1/superadmin/tenants/{id}/plan",
            "PUT /api/v1/superadmin/tenants/{id}/entitlements/overrides",
            "DELETE /api/v1/superadmin/tenants/{id}/entitlements/overrides/{key}",
            "POST /api/v1/superadmin/tenants/{id}/support/warnings",
            "PUT /api/v1/superadmin/tenants/{id}/support/context",
            "GET /api/v1/superadmin/tenants/{id}/review-intelligence",
            "PUT /api/v1/superadmin/tenants/{id}/review-intelligence",
            "POST /api/v1/superadmin/tenants/{id}/force-logout",
            "PUT /api/v1/superadmin/tenants/{id}/admins/main",
            "POST /api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/request",
            "POST /api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/confirm",
            "GET /api/v1/superadmin/plans",
            "POST /api/v1/superadmin/plans",
            "GET /api/v1/superadmin/plans/{stableId}",
            "PUT /api/v1/superadmin/plans/{stableId}",
            "POST /api/v1/superadmin/plans/{stableId}/archive",
            "GET /api/v1/superadmin/support/tickets",
            "GET /api/v1/superadmin/support/tickets/{ticketId}",
            "GET /api/v1/superadmin/support/tickets/{ticketId}/messages",
            "GET /api/v1/superadmin/support/tickets/{ticketId}/timeline",
            "POST /api/v1/superadmin/support/tickets/{ticketId}/convert-to-incident",
            "POST /api/v1/superadmin/support/tickets/{ticketId}/internal-notes",
            "POST /api/v1/superadmin/support/tickets/{ticketId}/messages",
            "POST /api/v1/superadmin/support/tickets/{ticketId}/status",
            "POST /api/v1/superadmin/support/tickets/sla/refresh",
            "GET /api/v1/superadmin/tenants/coa-templates",
            "GET /api/v1/superadmin/settings",
            "PUT /api/v1/superadmin/settings",
            "GET /api/v1/superadmin/audit/platform-events",
            "POST /api/v1/superadmin/changelog",
            "PUT /api/v1/superadmin/changelog/{id}",
            "DELETE /api/v1/superadmin/changelog/{id}",
            "POST /api/v1/superadmin/notify",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/refresh-token",
            "POST /api/v1/auth/logout",
            "GET /api/v1/auth/me",
            "POST /api/v1/auth/password/change",
            "POST /api/v1/auth/password/forgot",
            "POST /api/v1/auth/password/reset",
            "GET /api/v1/setup/status",
            "PUT /api/v1/setup/company-details",
            "PUT /api/v1/setup/gst",
            "PUT /api/v1/setup/accounting",
            "POST /api/v1/setup/invite-team",
            "POST /api/v1/setup/finish",
            "POST /api/v1/auth/mfa/setup",
            "POST /api/v1/auth/mfa/activate",
            "POST /api/v1/auth/mfa/disable",
            "GET /api/v1/admin/support/tickets",
            "POST /api/v1/admin/support/tickets",
            "GET /api/v1/admin/support/tickets/{ticketId}",
            "GET /api/v1/portal/support/tickets",
            "POST /api/v1/portal/support/tickets",
            "GET /api/v1/portal/support/tickets/{ticketId}",
            "GET /api/v1/dealer-portal/support/tickets",
            "POST /api/v1/dealer-portal/support/tickets",
            "GET /api/v1/dealer-portal/support/tickets/{ticketId}");
    List<String> actualRoutes = extractOperationSignatures(root.toString());
    assertThat(actualRoutes).containsAll(canonicalRoutes);

    List<String> retiredRoutes =
        List.of(
            "post /api/v1/superadmin/tenants/onboard",
            "post /api/v1/superadmin/tenants/{id}/support/admin-password-reset",
            "post /api/v1/superadmin/tenants/{id}/activate",
            "post /api/v1/superadmin/tenants/{id}/deactivate",
            "post /api/v1/superadmin/tenants/{id}/suspend",
            "put /api/v1/superadmin/tenants/{id}/lifecycle-state",
            "post /api/v1/companies/{id}/support/admin-password-reset",
            "post /api/v1/companies/superadmin/tenants",
            "get /api/v1/auth/profile",
            "put /api/v1/auth/profile",
            "get /api/v1/support/tickets",
            "post /api/v1/support/tickets");
    retiredRoutes.forEach(
        retired -> {
          int separator = retired.indexOf(' ');
          assertOperationMissing(
              root, retired.substring(separator + 1), retired.substring(0, separator));
        });

    String specText = root.toString();
    assertThat(specText)
        .doesNotContain("temporaryPassword")
        .doesNotContain("adminTemporaryPassword")
        .doesNotContain("credentialsEmailSent")
        .doesNotContain("TenantOnboardingRequest")
        .doesNotContain("CompanyAdminCredentialResetDto");
  }

  @Test
  void no_location_setup_scanner_is_scoped_to_v1_setup_contract() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    List<String> scopedOpenApiOperations =
        List.of(
            "GET /api/v1/superadmin/tenants/new",
            "POST /api/v1/superadmin/tenants",
            "GET /api/v1/superadmin/tenants",
            "GET /api/v1/superadmin/tenants/{id}",
            "GET /api/v1/setup/status",
            "PUT /api/v1/setup/company-details",
            "PUT /api/v1/setup/gst",
            "PUT /api/v1/setup/accounting",
            "POST /api/v1/setup/invite-team",
            "POST /api/v1/setup/finish");
    for (String signature : scopedOpenApiOperations) {
      int separator = signature.indexOf(' ');
      assertNoProhibitedSetupTerms(
          collectOperationAndSchemaText(
              root, signature.substring(separator + 1), signature.substring(0, separator)),
          "OpenAPI V1 setup scope " + signature);
    }

    List.of(
            "/api/v1/setup/branches",
            "/api/v1/setup/warehouses",
            "/api/v1/superadmin/tenants/{id}/branches",
            "/api/v1/superadmin/tenants/{id}/warehouses",
            "/api/v1/superadmin/tenants/{id}/setup/branches",
            "/api/v1/superadmin/tenants/{id}/setup/warehouses")
        .forEach(
            path -> {
              assertOperationMissing(root, path, "get");
              assertOperationMissing(root, path, "post");
              assertOperationMissing(root, path, "put");
            });

    List.of(
            "docs/frontend-portals/superadmin/api-contracts.md",
            "docs/frontend-portals/superadmin/workflows.md",
            "docs/frontend-portals/superadmin/README.md")
        .forEach(
            docPath -> {
              try {
                assertNoProhibitedSetupTerms(readRepoFile(docPath), "docs example " + docPath);
              } catch (IOException e) {
                throw new IllegalStateException("Unable to scan " + docPath, e);
              }
            });
  }

  @Test
  void auth_and_tenant_control_docs_match_the_hard_cut_route_story() throws IOException {
    String modulesAuth = readRepoFile("docs/modules/auth.md");
    assertThat(modulesAuth).contains("`GET /api/v1/auth/me`");
    assertThat(modulesAuth)
        .doesNotContain("`/api/v1/superadmin/tenants/{id}/support/admin-password-reset`");
    assertThat(modulesAuth)
        .doesNotContain("### UserProfileController — `/api/v1/auth/profile`")
        .doesNotContain("| GET | `/api/v1/auth/profile` |")
        .doesNotContain("| PUT | `/api/v1/auth/profile` |")
        .doesNotContain("| GET/HEAD | `/api/v1/auth/me`, `/api/v1/auth/profile` |");

    String flowAuthIdentity = readRepoFile("docs/flows/auth-identity.md");
    assertThat(flowAuthIdentity).contains("| `/me` | GET | `/api/v1/auth/me` |");
    assertThat(flowAuthIdentity)
        .doesNotContain("`/api/v1/superadmin/tenants/{id}/support/admin-password-reset`")
        .doesNotContain("| Profile read | GET | `/api/v1/auth/profile` |")
        .doesNotContain("| Profile update | PUT | `/api/v1/auth/profile` |")
        .doesNotContain("PUT `/api/v1/auth/profile`")
        .doesNotContain("`POST /api/v1/companies/{id}/support/admin-password-reset`");

    String codeReviewControlPlane =
        readRepoFile("docs/code-review/flows/company-tenant-control-plane.md");
    assertThat(codeReviewControlPlane)
        .contains("`PUT /api/v1/superadmin/tenants/{id}/lifecycle`")
        .contains("`PUT /api/v1/superadmin/tenants/{id}/limits`")
        .doesNotContain("`POST /api/v1/superadmin/tenants/{id}/support/admin-password-reset`")
        .doesNotContain("`GET /api/v1/admin/tenant-runtime/metrics`")
        .doesNotContain("`PUT /api/v1/admin/tenant-runtime/policy`")
        .doesNotContain("`PUT /api/v1/companies/{id}/tenant-runtime/policy`")
        .doesNotContain("CompanyService.updateTenantRuntimePolicy(...)")
        .doesNotContain("suspend, activate, deactivate, list usage");

    String authHardening = readRepoFile(".factory/library/auth-hardening.md");
    assertThat(authHardening)
        .contains("`GET /api/v1/auth/me`")
        .contains("`PUT /api/v1/superadmin/tenants/{id}/limits`")
        .doesNotContain("`GET /auth/profile`")
        .doesNotContain("`PUT /api/v1/companies/{id}/tenant-runtime/policy`")
        .doesNotContain("`PUT /api/v1/admin/tenant-runtime/policy`");

    String frontendHandoff = readRepoFile(".factory/library/frontend-handoff.md");
    assertThat(frontendHandoff)
        .contains("| GET | `/api/v1/auth/me` |")
        .contains("| PUT | `/api/v1/superadmin/tenants/{id}/lifecycle` |")
        .contains("| PUT | `/api/v1/superadmin/tenants/{id}/limits` |")
        .doesNotContain("| GET | `/api/v1/auth/profile` |")
        .doesNotContain("| PUT | `/api/v1/auth/profile` |")
        .doesNotContain("GET /api/v1/auth/profile")
        .doesNotContain("PUT /api/v1/auth/profile")
        .doesNotContain("`PUT /api/v1/companies/{id}/tenant-runtime/policy`")
        .doesNotContain("`PUT /api/v1/admin/tenant-runtime/policy`")
        .doesNotContain("`POST /api/v1/superadmin/tenants/{id}/suspend`")
        .doesNotContain("`POST /api/v1/superadmin/tenants/{id}/activate`")
        .doesNotContain("`POST /api/v1/superadmin/tenants/{id}/deactivate`");

    String frontendV2 = readRepoFile(".factory/library/frontend-v2.md");
    assertThat(frontendV2)
        .contains("`PUT /api/v1/superadmin/tenants/{id}/limits`")
        .doesNotContain("`PUT /api/v1/companies/{id}/tenant-runtime/policy`");

    String runtimeControlPlane = readRepoFile(".factory/library/tenant-runtime-control-plane.md");
    assertThat(runtimeControlPlane)
        .contains(
            "Canonical control-plane mutation path: `PUT /api/v1/superadmin/tenants/{id}/limits`")
        .doesNotContain("Public mutation path: `PUT /api/v1/companies/{id}/tenant-runtime/policy`");
  }

  @Test
  void catalog_surface_contract_exposes_only_canonical_public_routes() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/catalog/brands",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListCatalogBrandDto");
    assertQueryParameter(root, "/api/v1/catalog/brands", "get", "active");
    assertOperationContract(
        root,
        "/api/v1/catalog/brands",
        "post",
        "#/components/schemas/CatalogBrandRequest",
        "200",
        "#/components/schemas/ApiResponseCatalogBrandDto");

    assertMultipartBinaryRequest(root, "/api/v1/catalog/import", "post", "file");
    assertOperationResponse(
        root,
        "/api/v1/catalog/import",
        "post",
        "200",
        "#/components/schemas/ApiResponseCatalogImportResponse");
    assertOperationContract(
        root,
        "/api/v1/catalog/items",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponsePageResponseCatalogItemDto");
    assertQueryParameter(root, "/api/v1/catalog/items", "get", "q");
    assertQueryParameter(root, "/api/v1/catalog/items", "get", "itemClass");
    assertOperationContract(
        root,
        "/api/v1/catalog/items",
        "post",
        "#/components/schemas/CatalogItemRequest",
        "200",
        "#/components/schemas/ApiResponseCatalogItemDto");
    assertOperationContract(
        root,
        "/api/v1/catalog/items/bulk-variants",
        "post",
        "#/components/schemas/BulkVariantRequest",
        "200",
        "#/components/schemas/ApiResponseBulkVariantResponse");
    assertQueryParameter(root, "/api/v1/catalog/items/bulk-variants", "post", "dryRun");
    assertOperationContract(
        root,
        "/api/v1/catalog/items/{itemId}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseCatalogItemDto");
    assertOperationContract(
        root,
        "/api/v1/catalog/items/{itemId}",
        "put",
        "#/components/schemas/CatalogItemRequest",
        "200",
        "#/components/schemas/ApiResponseCatalogItemDto");
    assertOperationContract(
        root,
        "/api/v1/catalog/items/{itemId}",
        "delete",
        null,
        "200",
        "#/components/schemas/ApiResponseCatalogItemDto");
    assertOperationMissing(root, "/api/v1/catalog/products", "get");
    assertOperationMissing(root, "/api/v1/catalog/products", "post");
    assertOperationMissing(root, "/api/v1/catalog/products/single", "post");
    assertOperationMissing(root, "/api/v1/catalog/products/bulk-variants", "post");

    assertOperationMissing(root, "/api/v1/accounting/catalog/import", "post");
    assertOperationMissing(root, "/api/v1/accounting/catalog/products", "get");
    assertOperationMissing(root, "/api/v1/accounting/catalog/products", "post");
    assertOperationMissing(root, "/api/v1/accounting/catalog/products/{id}", "put");
    assertOperationMissing(root, "/api/v1/accounting/catalog/products/bulk-variants", "post");
    assertOperationMissing(root, "/api/v1/production/brands", "get");
    assertOperationMissing(root, "/api/v1/production/brands/{brandId}/products", "get");
  }

  @Test
  void report_contract_paths_use_canonical_namespace_only() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/reports/aged-debtors",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseListAgedDebtorDto");
    assertOperationContract(
        root,
        "/api/v1/reports/balance-sheet/hierarchy",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseBalanceSheetHierarchy");
    assertOperationContract(
        root,
        "/api/v1/reports/income-statement/hierarchy",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseIncomeStatementHierarchy");
    assertOperationContract(
        root,
        "/api/v1/reports/aging/receivables",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseAgedReceivablesReport");

    assertOperationMissing(root, "/api/v1/accounting/reports/aged-debtors", "get");
    assertOperationMissing(root, "/api/v1/accounting/reports/balance-sheet/hierarchy", "get");
    assertOperationMissing(root, "/api/v1/accounting/reports/income-statement/hierarchy", "get");
    assertOperationMissing(root, "/api/v1/accounting/reports/aging/receivables", "get");
    assertOperationMissing(root, "/api/v1/accounting/reports/aging/dealer/{dealerId}", "get");
    assertOperationMissing(
        root, "/api/v1/accounting/reports/aging/dealer/{dealerId}/detailed", "get");
    assertOperationMissing(root, "/api/v1/accounting/reports/dso/dealer/{dealerId}", "get");
    assertOperationMissing(root, "/api/v1/reports/aging/dealer/{dealerId}", "get");
    assertOperationMissing(root, "/api/v1/reports/aging/dealer/{dealerId}/detailed", "get");
    assertOperationMissing(root, "/api/v1/reports/dso/dealer/{dealerId}", "get");
  }

  @Test
  void orchestrator_contract_exposes_only_canonical_runtime_routes() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    JsonNode approveOperation =
        root.path("paths").path("/api/v1/orchestrator/orders/{orderId}/approve").path("post");
    assertThat(approveOperation.isMissingNode()).isFalse();
    assertThat(
            approveOperation
                .path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema")
                .path("$ref")
                .asText())
        .isEqualTo("#/components/schemas/ApproveOrderRequest");
    assertThat(approveOperation.path("responses").has("200")).isTrue();
    JsonNode approveResponseSchema =
        approveOperation.path("responses").path("200").path("content").path("*/*").path("schema");
    assertThat(approveResponseSchema.path("type").asText()).isEqualTo("object");
    assertThat(approveResponseSchema.path("additionalProperties").path("type").asText())
        .isEqualTo("object");

    JsonNode fulfillmentOperation =
        root.path("paths").path("/api/v1/orchestrator/orders/{orderId}/fulfillment").path("post");
    assertThat(fulfillmentOperation.isMissingNode()).isFalse();
    assertThat(
            fulfillmentOperation
                .path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema")
                .path("$ref")
                .asText())
        .isEqualTo("#/components/schemas/OrderFulfillmentRequest");
    assertThat(fulfillmentOperation.path("responses").has("200")).isTrue();
    JsonNode fulfillmentResponseSchema =
        fulfillmentOperation
            .path("responses")
            .path("200")
            .path("content")
            .path("*/*")
            .path("schema");
    assertThat(fulfillmentResponseSchema.path("type").asText()).isEqualTo("object");
    assertThat(fulfillmentResponseSchema.path("additionalProperties").path("type").asText())
        .isEqualTo("object");
    assertThat(
            root.path("paths")
                .path("/api/v1/orchestrator/traces/{traceId}")
                .path("get")
                .isMissingNode())
        .isFalse();
    assertThat(
            root.path("paths")
                .path("/api/v1/orchestrator/traces/{traceId}")
                .path("get")
                .path("responses")
                .has("200"))
        .isTrue();
    assertThat(
            root.path("paths")
                .path("/api/v1/orchestrator/health/integrations")
                .path("get")
                .isMissingNode())
        .isFalse();
    assertThat(
            root.path("paths")
                .path("/api/v1/orchestrator/health/integrations")
                .path("get")
                .path("responses")
                .has("200"))
        .isTrue();
    assertThat(
            root.path("paths")
                .path("/api/v1/orchestrator/health/events")
                .path("get")
                .isMissingNode())
        .isFalse();
    assertThat(
            root.path("paths")
                .path("/api/v1/orchestrator/health/events")
                .path("get")
                .path("responses")
                .has("200"))
        .isTrue();

    assertOperationMissing(root, "/api/v1/orchestrator/dispatch", "post");
    assertOperationMissing(root, "/api/v1/orchestrator/dispatch/{orderId}", "post");
    assertOperationMissing(root, "/api/v1/orchestrator/factory/dispatch/{batchId}", "post");
    assertOperationMissing(root, "/api/v1/orchestrator/payroll/run", "post");
  }

  @Test
  void portal_finance_contract_paths_expose_only_canonical_namespace() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/portal/finance/ledger",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseMapStringObject");
    assertQueryParameter(root, "/api/v1/portal/finance/ledger", "get", "dealerId");
    assertOperationContract(
        root,
        "/api/v1/portal/finance/invoices",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseMapStringObject");
    assertQueryParameter(root, "/api/v1/portal/finance/invoices", "get", "dealerId");
    assertOperationContract(
        root,
        "/api/v1/portal/finance/aging",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseMapStringObject");
    assertQueryParameter(root, "/api/v1/portal/finance/aging", "get", "dealerId");

    assertOperationMissing(root, "/api/v1/dealers/{dealerId}/ledger", "get");
    assertOperationMissing(root, "/api/v1/dealers/{dealerId}/invoices", "get");
    assertOperationMissing(root, "/api/v1/dealers/{dealerId}/aging", "get");
    assertOperationMissing(root, "/api/v1/dealers/{dealerId}/credit-utilization", "get");
    assertOperationMissing(root, "/api/v1/dealers/{dealerId}", "delete");
    assertOperationMissing(root, "/api/v1/invoices/dealers/{dealerId}", "get");
    assertOperationMissing(root, "/api/v1/accounting/aging/dealers/{dealerId}", "get");
    assertOperationMissing(root, "/api/v1/accounting/aging/dealers/{dealerId}/pdf", "get");
    assertOperationMissing(root, "/api/v1/accounting/statements/dealers/{dealerId}", "get");
    assertOperationMissing(root, "/api/v1/accounting/statements/dealers/{dealerId}/pdf", "get");
  }

  @Test
  void support_ticket_contract_paths_expose_only_split_hosts() throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/admin/support/tickets",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSupportTicketListResponse");
    assertOperationContract(
        root,
        "/api/v1/admin/support/tickets",
        "post",
        "#/components/schemas/SupportTicketCreateRequest",
        "200",
        "#/components/schemas/ApiResponseSupportTicketResponse");
    assertOperationContract(
        root,
        "/api/v1/admin/support/tickets/{ticketId}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSupportTicketResponse");

    assertOperationContract(
        root,
        "/api/v1/portal/support/tickets",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSupportTicketListResponse");
    assertOperationContract(
        root,
        "/api/v1/portal/support/tickets",
        "post",
        "#/components/schemas/SupportTicketCreateRequest",
        "200",
        "#/components/schemas/ApiResponseSupportTicketResponse");
    assertOperationContract(
        root,
        "/api/v1/portal/support/tickets/{ticketId}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSupportTicketResponse");

    assertOperationContract(
        root,
        "/api/v1/dealer-portal/support/tickets",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSupportTicketListResponse");
    assertOperationContract(
        root,
        "/api/v1/dealer-portal/support/tickets",
        "post",
        "#/components/schemas/SupportTicketCreateRequest",
        "200",
        "#/components/schemas/ApiResponseSupportTicketResponse");
    assertOperationContract(
        root,
        "/api/v1/dealer-portal/support/tickets/{ticketId}",
        "get",
        null,
        "200",
        "#/components/schemas/ApiResponseSupportTicketResponse");

    assertOperationMissing(root, "/api/v1/support/tickets", "get");
    assertOperationMissing(root, "/api/v1/support/tickets", "post");
    assertOperationMissing(root, "/api/v1/support/tickets/{ticketId}", "get");
  }

  @Test
  void
      inventory_contract_requires_explicit_opening_stock_batch_key_and_removes_retired_bulk_pack_request()
          throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertMultipartBinaryRequest(root, "/api/v1/inventory/opening-stock", "post", "file");

    JsonNode openingStockParameters =
        root.path("paths").path("/api/v1/inventory/opening-stock").path("post").path("parameters");
    JsonNode openingStockBatchKey = null;
    for (JsonNode parameter : openingStockParameters) {
      if ("openingStockBatchKey".equals(parameter.path("name").asText())) {
        openingStockBatchKey = parameter;
        break;
      }
    }

    assertThat(openingStockBatchKey)
        .withFailMessage(
            "Expected query parameter 'openingStockBatchKey' on POST"
                + " /api/v1/inventory/opening-stock")
        .isNotNull();
    assertThat(openingStockBatchKey.path("in").asText()).isEqualTo("query");
    assertThat(openingStockBatchKey.path("required").asBoolean()).isTrue();

    assertThat(root.path("components").path("schemas").has("BulkPackRequest"))
        .withFailMessage(
            "BulkPackRequest schema must be absent after removing retired bulk mutation surface")
        .isFalse();
  }

  @Test
  void production_log_contract_stays_ready_to_pack_and_removes_dead_request_toggles()
      throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationMissing(root, "/api/v1/factory/production-batches", "get");
    assertOperationMissing(root, "/api/v1/factory/production-batches", "post");
    assertOperationContract(
        root,
        "/api/v1/factory/production/logs",
        "post",
        "#/components/schemas/ProductionLogRequest",
        "200",
        "#/components/schemas/ApiResponseProductionLogDetailDto");
    assertThat(root.path("components").path("schemas").has("ProductionBatchRequest")).isFalse();
    assertThat(root.path("components").path("schemas").has("ProductionBatchDto")).isFalse();

    JsonNode productionLogRequest =
        root.path("components").path("schemas").path("ProductionLogRequest");
    JsonNode requestProperties = productionLogRequest.path("properties");
    assertThat(requestProperties.has("brandId")).isTrue();
    assertThat(requestProperties.has("productId")).isTrue();
    assertThat(requestProperties.has("mixedQuantity")).isTrue();
    assertThat(requestProperties.has("materials")).isTrue();
    assertThat(requestProperties.has("addToFinishedGoods"))
        .withFailMessage("ProductionLogRequest must not expose retired addToFinishedGoods toggle")
        .isFalse();

    JsonNode detailDto = root.path("components").path("schemas").path("ProductionLogDetailDto");
    JsonNode detailProperties = detailDto.path("properties");
    assertThat(detailProperties.has("id")).isTrue();
    assertThat(detailProperties.has("publicId")).isTrue();
    assertThat(detailProperties.has("productionCode")).isTrue();
    assertThat(detailProperties.has("productFamilyName")).isTrue();
    assertThat(detailProperties.has("outputBatchCode")).isTrue();
    assertThat(detailProperties.has("outputQuantity")).isTrue();
    assertThat(detailProperties.has("totalPackedQuantity")).isTrue();
    assertThat(detailProperties.has("status")).isTrue();
    assertThat(detailProperties.has("allowedSellableSizes")).isTrue();

    JsonNode unpackedBatchDto = root.path("components").path("schemas").path("UnpackedBatchDto");
    JsonNode unpackedProperties = unpackedBatchDto.path("properties");
    assertThat(unpackedProperties.has("productFamilyName")).isTrue();
    assertThat(unpackedProperties.has("allowedSellableSizes")).isTrue();

    JsonNode packingLineRequest =
        root.path("components").path("schemas").path("PackingLineRequest");
    List<String> packingLineRequired = new ArrayList<>();
    packingLineRequest.path("required").forEach(node -> packingLineRequired.add(node.asText()));
    assertThat(packingLineRequired).contains("childFinishedGoodId");
  }

  @Test
  void superadmin_contract_documents_standard_envelope_errors_and_trace_metadata()
      throws IOException {
    JsonNode root = fetchCurrentSpecNode();
    JsonNode dashboardEnvelope =
        root.path("components").path("schemas").path("ApiResponseCompanySuperAdminDashboardDto");
    assertThat(dashboardEnvelope.path("properties").has("metadata")).isTrue();
    JsonNode dashboardProperties =
        root.path("components")
            .path("schemas")
            .path("CompanySuperAdminDashboardDto")
            .path("properties");
    assertThat(dashboardProperties.has("recurringRevenueByCurrency")).isTrue();
    assertThat(dashboardProperties.has("recurringRevenueAggregationPolicy")).isTrue();
    assertThat(dashboardProperties.has("recurringRevenueCurrencyCount")).isTrue();
    JsonNode metadataSchema = root.path("components").path("schemas").path("Metadata");
    assertThat(metadataSchema.path("properties").has("traceId")).isTrue();
    assertThat(metadataSchema.path("properties").has("correlationId")).isTrue();

    root.path("paths")
        .fields()
        .forEachRemaining(
            pathEntry -> {
              if (!pathEntry.getKey().startsWith("/api/v1/superadmin")) {
                return;
              }
              pathEntry
                  .getValue()
                  .fields()
                  .forEachRemaining(
                      methodEntry -> {
                        if (!isHttpMethod(methodEntry.getKey())) {
                          return;
                        }
                        JsonNode responses = methodEntry.getValue().path("responses");
                        responses
                            .fields()
                            .forEachRemaining(
                                responseEntry -> {
                                  String responseCode = responseEntry.getKey();
                                  if (!responseCode.startsWith("2")) {
                                    return;
                                  }
                                  JsonNode content = responseEntry.getValue().path("content");
                                  assertThat(content.isMissingNode() || content.isEmpty())
                                      .as(
                                          "Super Admin success response %s for %s %s must document"
                                              + " an ApiResponse schema",
                                          responseCode, methodEntry.getKey(), pathEntry.getKey())
                                      .isFalse();
                                  JsonNode schema = content.path("*/*").path("schema");
                                  if (schema.isMissingNode()) {
                                    schema = content.path("application/json").path("schema");
                                  }
                                  assertThat(schema.path("$ref").asText())
                                      .as(
                                          "Super Admin success response %s for %s %s must use"
                                              + " ApiResponse schema",
                                          responseCode, methodEntry.getKey(), pathEntry.getKey())
                                      .startsWith("#/components/schemas/ApiResponse");
                                });
                        assertThat(responses.has("400"))
                            .as(
                                "400 response documented for %s %s",
                                methodEntry.getKey(), pathEntry.getKey())
                            .isTrue();
                        assertThat(responses.has("403"))
                            .as(
                                "403 response documented for %s %s",
                                methodEntry.getKey(), pathEntry.getKey())
                            .isTrue();
                        assertThat(responses.has("415"))
                            .as(
                                "415 response documented for %s %s",
                                methodEntry.getKey(), pathEntry.getKey())
                            .isTrue();
                        assertThat(
                                responses
                                    .path("400")
                                    .path("description")
                                    .asText("")
                                    .contains("metadata.traceId"))
                            .isTrue();
                      });
            });
  }

  @Test
  void packing_contract_keeps_only_canonical_write_surface_and_header_only_idempotency()
      throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/factory/packing-records",
        "post",
        "#/components/schemas/PackingRequest",
        "200",
        "#/components/schemas/ApiResponseProductionLogDetailDto");
    assertOperationMissing(
        root, "/api/v1/factory/packing-records/{productionLogId}/complete", "post");
    assertOperationMissing(root, "/api/v1/factory/pack", "post");

    JsonNode parameters =
        root.path("paths").path("/api/v1/factory/packing-records").path("post").path("parameters");
    List<String> parameterNames = new ArrayList<>();
    parameters.forEach(parameter -> parameterNames.add(parameter.path("name").asText()));
    assertThat(parameterNames).containsExactly("Idempotency-Key");
    assertThat(parameters.get(0).path("required").asBoolean()).isTrue();

    JsonNode packingRequest = root.path("components").path("schemas").path("PackingRequest");
    assertThat(packingRequest.path("properties").has("closeResidualWastage"))
        .withFailMessage(
            "PackingRequest must expose closeResidualWastage on the canonical packing route")
        .isTrue();
    assertThat(packingRequest.path("properties").has("idempotencyKey"))
        .withFailMessage("PackingRequest must not expose idempotencyKey in the request body")
        .isFalse();
  }

  @Test
  void accounting_manual_journal_and_receipt_settlement_contracts_are_hard_cut()
      throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertOperationContract(
        root,
        "/api/v1/accounting/journal-entries",
        "post",
        "#/components/schemas/JournalEntryRequest",
        "200",
        "#/components/schemas/ApiResponseJournalEntryDto");
    assertOperationMissing(root, "/api/v1/accounting/journals/manual", "post");
    assertOperationMissing(root, "/api/v1/accounting/journals/{entryId}/reverse", "post");
    assertOperationMissing(
        root, "/api/v1/accounting/journal-entries/{entryId}/cascade-reverse", "post");
    assertOperationMissing(root, "/api/v1/accounting/periods/{periodId}/close", "post");

    assertHeaderParameters(root, "/api/v1/accounting/receipts/dealer", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/accounting/receipts/dealer/hybrid", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/accounting/settlements/dealers", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/accounting/dealers/{dealerId}/auto-settle", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/accounting/settlements/suppliers", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/accounting/suppliers/{supplierId}/auto-settle", "post", "Idempotency-Key");

    assertOperationContract(
        root,
        "/api/v1/accounting/settlements/dealers",
        "post",
        "#/components/schemas/PartnerSettlementRequest",
        "200",
        "#/components/schemas/ApiResponsePartnerSettlementResponse");
    assertOperationContract(
        root,
        "/api/v1/accounting/settlements/suppliers",
        "post",
        "#/components/schemas/PartnerSettlementRequest",
        "200",
        "#/components/schemas/ApiResponsePartnerSettlementResponse");
    assertOperationContract(
        root,
        "/api/v1/accounting/periods",
        "post",
        "#/components/schemas/AccountingPeriodRequest",
        "200",
        "#/components/schemas/ApiResponseAccountingPeriodDto");
    assertOperationContract(
        root,
        "/api/v1/accounting/periods/{periodId}",
        "put",
        "#/components/schemas/AccountingPeriodRequest",
        "200",
        "#/components/schemas/ApiResponseAccountingPeriodDto");

    assertSchemaPresence(root, "PartnerSettlementRequest", true);
    assertSchemaPresence(root, "AccountingPeriodRequest", true);
    assertSchemaPresence(root, "DealerSettlementRequest", false);
    assertSchemaPresence(root, "SupplierSettlementRequest", false);
    assertSchemaPresence(root, "AccountingPeriodUpsertRequest", false);
    assertSchemaPresence(root, "AccountingPeriodUpdateRequest", false);
    assertSchemaPresence(root, "AccountingPeriodCloseRequest", false);
    assertSchemaPresence(root, "AccountingPeriodLockRequest", false);
  }

  @Test
  void legacy_idempotency_headers_are_hidden_on_hard_cut_sales_and_inventory_writes()
      throws IOException {
    JsonNode root = fetchCurrentSpecNode();

    assertHeaderParameters(root, "/api/v1/sales/orders", "post", "Idempotency-Key");
    assertHeaderParameters(root, "/api/v1/inventory/adjustments", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/inventory/raw-materials/adjustments", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/purchasing/raw-material-purchases", "post", "Idempotency-Key");
    assertHeaderParameters(
        root, "/api/v1/purchasing/raw-material-purchases/returns", "post", "Idempotency-Key");
  }

  @Test
  void openapi_snapshot_matches_repository_contract() throws IOException {
    Path openApiSnapshotPath = resolveRepoRoot().resolve("openapi.json");
    String currentSpec = canonicalizeJson(fetchCurrentSpecNode().toString());
    if (refreshRequested()) {
      assertThat(verifyRequested())
          .withFailMessage(
              "Refresh requires verify mode. Set -D%s=true (or %s=true) together with "
                  + "-D%s=true (or %s=true).",
              SNAPSHOT_VERIFY_PROPERTY,
              SNAPSHOT_VERIFY_ENV,
              SNAPSHOT_REFRESH_PROPERTY,
              SNAPSHOT_REFRESH_ENV)
          .isTrue();
      Files.writeString(openApiSnapshotPath, currentSpec, StandardCharsets.UTF_8);
      return;
    }

    assertThat(Files.exists(openApiSnapshotPath))
        .withFailMessage(
            "Missing OpenAPI snapshot at %s. Remediation: rerun intentionally with -D%s=true "
                + "or %s=true (with -D%s=true or %s=true) to generate it.",
            openApiSnapshotPath,
            SNAPSHOT_REFRESH_PROPERTY,
            SNAPSHOT_REFRESH_ENV,
            SNAPSHOT_VERIFY_PROPERTY,
            SNAPSHOT_VERIFY_ENV)
        .isTrue();

    String snapshotSpec =
        canonicalizeJson(Files.readString(openApiSnapshotPath, StandardCharsets.UTF_8));
    String currentSpecHash = sha256Hex(currentSpec);
    String snapshotSpecHash = sha256Hex(snapshotSpec);
    List<String> currentOps = extractOperationSignatures(currentSpec);
    List<String> snapshotOps = extractOperationSignatures(snapshotSpec);
    List<String> missingSnapshotOps = new ArrayList<>(snapshotOps);
    missingSnapshotOps.removeAll(currentOps);
    String missingSnapshotOpsPreview =
        missingSnapshotOps.stream().limit(12).collect(Collectors.joining(", "));

    assertThat(snapshotOps)
        .withFailMessage(
            "OpenAPI snapshot at %s has no operations. Refresh snapshot intentionally with "
                + "-D%s=true (or %s=true) and -D%s=true (or %s=true).",
            openApiSnapshotPath,
            SNAPSHOT_VERIFY_PROPERTY,
            SNAPSHOT_VERIFY_ENV,
            SNAPSHOT_REFRESH_PROPERTY,
            SNAPSHOT_REFRESH_ENV)
        .isNotEmpty();

    assertThat(currentOps)
        .withFailMessage(
            "OpenAPI breaking operation drift detected at %s. currentOps=%d snapshotOps=%d"
                + " (delta=%d) currentHash=%s snapshotHash=%s. Snapshot operations must remain"
                + " present in the runtime contract. missingSnapshotOpsCount=%d"
                + " missingSnapshotOpsPreview=[%s]. For full parity (including additive drift),"
                + " rerun with -D%s=true (or %s=true) and refresh using -D%s=true (or %s=true).",
            openApiSnapshotPath,
            currentOps.size(),
            snapshotOps.size(),
            currentOps.size() - snapshotOps.size(),
            currentSpecHash,
            snapshotSpecHash,
            missingSnapshotOps.size(),
            missingSnapshotOpsPreview,
            SNAPSHOT_VERIFY_PROPERTY,
            SNAPSHOT_VERIFY_ENV,
            SNAPSHOT_REFRESH_PROPERTY,
            SNAPSHOT_REFRESH_ENV)
        .containsAll(snapshotOps);

    if (!verifyRequested()) {
      return;
    }

    assertThat(currentSpec)
        .withFailMessage(
            "OpenAPI snapshot drift detected at %s. Verify mode is non-mutating unless refresh is"
                + " enabled. Parity signal (sha256) current=%s snapshot=%s. Remediation: rerun"
                + " intentionally with -D%s=true (or %s=true) and -D%s=true (or %s=true), then"
                + " commit updated openapi.json.",
            openApiSnapshotPath,
            currentSpecHash,
            snapshotSpecHash,
            SNAPSHOT_VERIFY_PROPERTY,
            SNAPSHOT_VERIFY_ENV,
            SNAPSHOT_REFRESH_PROPERTY,
            SNAPSHOT_REFRESH_ENV)
        .isEqualTo(snapshotSpec);
  }

  private static boolean verifyRequested() {
    return Boolean.parseBoolean(
        System.getProperty(
            SNAPSHOT_VERIFY_PROPERTY, System.getenv().getOrDefault(SNAPSHOT_VERIFY_ENV, "false")));
  }

  private static boolean refreshRequested() {
    return Boolean.parseBoolean(
        System.getProperty(
            SNAPSHOT_REFRESH_PROPERTY,
            System.getenv().getOrDefault(SNAPSHOT_REFRESH_ENV, "false")));
  }

  private static Path resolveRepoRoot() {
    Path moduleRoot = Path.of("").toAbsolutePath().normalize();
    if (moduleRoot.getFileName() != null
        && "erp-domain".equals(moduleRoot.getFileName().toString())) {
      Path parent = moduleRoot.getParent();
      if (parent != null) {
        return parent;
      }
    }
    return moduleRoot;
  }

  private static String canonicalizeJson(String spec) throws IOException {
    JsonNode parsedSpec = CANONICAL_JSON.readTree(spec);
    return CANONICAL_JSON.writeValueAsString(canonicalizeNode(parsedSpec));
  }

  private void assertHeaderParameters(
      JsonNode root, String path, String method, String... expectedHeaderNames) {
    JsonNode parameters = root.path("paths").path(path).path(method).path("parameters");
    List<String> parameterNames = new ArrayList<>();
    parameters.forEach(
        parameter -> {
          if ("header".equals(parameter.path("in").asText())) {
            parameterNames.add(parameter.path("name").asText());
          }
        });
    assertThat(parameterNames).containsExactly(expectedHeaderNames);
  }

  private void assertSchemaPresence(JsonNode root, String schemaName, boolean expectedPresence) {
    assertThat(root.path("components").path("schemas").has(schemaName))
        .withFailMessage(
            "Expected schema %s presence=%s in generated OpenAPI spec",
            schemaName, expectedPresence)
        .isEqualTo(expectedPresence);
  }

  private void assertPlanDefaultLimitsRequestSchema(JsonNode root) {
    JsonNode schemas = root.path("components").path("schemas");
    JsonNode createRequest = schemas.path("SuperAdminPlanTemplateCreateRequest");
    JsonNode updateRequest = schemas.path("SuperAdminPlanTemplateUpdateRequest");
    String limitsRef = "#/components/schemas/SuperAdminPlanTemplateDefaultLimitsRequest";
    assertThat(createRequest.path("properties").path("defaultLimits").path("$ref").asText())
        .isEqualTo(limitsRef);
    assertThat(updateRequest.path("properties").path("defaultLimits").path("$ref").asText())
        .isEqualTo(limitsRef);

    JsonNode requestLimits = schemas.path("SuperAdminPlanTemplateDefaultLimitsRequest");
    JsonNode responseLimits = schemas.path("DefaultLimits");
    assertThat(requestLimits.path("properties").has("zeroMeansUnlimited")).isFalse();
    assertThat(responseLimits.path("properties").has("zeroMeansUnlimited")).isTrue();
    assertThat(requestLimits.path("required").isArray()).isTrue();
    List<String> requiredFields = new ArrayList<>();
    requestLimits.path("required").forEach(node -> requiredFields.add(node.asText()));
    List<String> limitFields =
        List.of(
            "maxActiveUsers",
            "maxApiRequests",
            "maxStorageBytes",
            "maxPdfExports",
            "maxEmails",
            "maxJobs",
            "burstRequestsPerMinute",
            "maxConcurrentRequests");
    assertThat(requiredFields).containsAll(limitFields);
    limitFields.forEach(
        field -> {
          JsonNode property = requestLimits.path("properties").path(field);
          assertThat(property.path("minimum").asInt()).as(field + " minimum").isZero();
        });
  }

  private static List<String> extractOperationSignatures(String spec) throws IOException {
    JsonNode root = CANONICAL_JSON.readTree(spec);
    JsonNode paths = root.get("paths");
    List<String> operations = new ArrayList<>();
    if (paths == null || !paths.isObject()) {
      return operations;
    }
    paths
        .fields()
        .forEachRemaining(
            pathEntry -> {
              JsonNode methods = pathEntry.getValue();
              if (methods == null || !methods.isObject()) {
                return;
              }
              methods
                  .fieldNames()
                  .forEachRemaining(
                      method -> {
                        if (isHttpMethod(method)) {
                          operations.add(method.toUpperCase() + " " + pathEntry.getKey());
                        }
                      });
            });
    Collections.sort(operations);
    return operations;
  }

  private static boolean isHttpMethod(String method) {
    return "get".equalsIgnoreCase(method)
        || "put".equalsIgnoreCase(method)
        || "post".equalsIgnoreCase(method)
        || "delete".equalsIgnoreCase(method)
        || "patch".equalsIgnoreCase(method)
        || "options".equalsIgnoreCase(method)
        || "head".equalsIgnoreCase(method)
        || "trace".equalsIgnoreCase(method);
  }

  private static JsonNode canonicalizeNode(JsonNode node) {
    if (node == null || node.isNull() || node.isValueNode()) {
      return node;
    }
    if (node.isArray()) {
      ArrayNode canonicalArray = CANONICAL_JSON.createArrayNode();
      for (JsonNode item : node) {
        canonicalArray.add(canonicalizeNode(item));
      }
      return canonicalArray;
    }
    if (node.isObject()) {
      ObjectNode canonicalObject = CANONICAL_JSON.createObjectNode();
      List<String> fieldNames = new ArrayList<>();
      node.fieldNames().forEachRemaining(fieldNames::add);
      Collections.sort(fieldNames);
      for (String fieldName : fieldNames) {
        canonicalObject.set(fieldName, canonicalizeNode(node.get(fieldName)));
      }
      return canonicalObject;
    }
    return node;
  }

  private String collectOperationAndSchemaText(JsonNode root, String path, String method)
      throws IOException {
    JsonNode operation = root.path("paths").path(path).path(method.toLowerCase());
    assertThat(operation.isMissingNode())
        .withFailMessage("Missing %s %s from generated OpenAPI spec", method.toUpperCase(), path)
        .isFalse();
    ObjectNode scoped = CANONICAL_JSON.createObjectNode();
    scoped.set("operation", operation);
    ObjectNode schemas = CANONICAL_JSON.createObjectNode();
    collectReferencedSchemas(root, operation, new HashSet<>(), schemas);
    scoped.set("schemas", schemas);
    return CANONICAL_JSON.writeValueAsString(canonicalizeNode(scoped));
  }

  private static void collectReferencedSchemas(
      JsonNode root, JsonNode node, Set<String> visitedSchemaNames, ObjectNode sink) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return;
    }
    if (node.isObject()) {
      String ref = node.path("$ref").asText(null);
      if (ref != null && ref.startsWith("#/components/schemas/")) {
        String schemaName = ref.substring("#/components/schemas/".length());
        if (visitedSchemaNames.add(schemaName)) {
          JsonNode schema = root.path("components").path("schemas").path(schemaName);
          sink.set(schemaName, schema);
          collectReferencedSchemas(root, schema, visitedSchemaNames, sink);
        }
      }
      node.fields()
          .forEachRemaining(
              entry -> collectReferencedSchemas(root, entry.getValue(), visitedSchemaNames, sink));
      return;
    }
    if (node.isArray()) {
      node.forEach(item -> collectReferencedSchemas(root, item, visitedSchemaNames, sink));
    }
  }

  private static void assertNoProhibitedSetupTerms(String text, String scope) {
    String normalized = text.toLowerCase(java.util.Locale.ROOT);
    assertThat(normalized)
        .withFailMessage(
            "%s must not expose branch or warehouse setup terms. Scanner scope is V1 Add Client,"
                + " owner setup, tenant profile summaries, OpenAPI operation schemas, and"
                + " Super Admin frontend examples; unrelated ERP bank-branch fields and git"
                + " branch provenance are intentionally excluded.",
            scope)
        .doesNotContain("branch", "warehouse");
  }

  private static String sha256Hex(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        builder.append(Character.forDigit((b >>> 4) & 0x0f, 16));
        builder.append(Character.forDigit(b & 0x0f, 16));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
    }
  }

  private JsonNode fetchCurrentSpecNode() throws IOException {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(30_000);
    requestFactory.setReadTimeout(120_000);
    rest.getRestTemplate().setRequestFactory(requestFactory);

    ResponseEntity<String> json = rest.getForEntity("/v3/api-docs", String.class);
    assertThat(json.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json.getBody()).as("OpenAPI payload").isNotBlank();
    return CANONICAL_JSON.readTree(json.getBody());
  }

  private String readRepoFile(String relativePath) throws IOException {
    return Files.readString(resolveRepoRoot().resolve(relativePath), StandardCharsets.UTF_8);
  }

  private void assertOperationContract(
      JsonNode root,
      String path,
      String method,
      String expectedRequestRef,
      String expectedResponseCode,
      String expectedResponseRef) {
    JsonNode operation = root.path("paths").path(path).path(method);
    assertThat(operation.isMissingNode())
        .withFailMessage("Missing %s %s from generated OpenAPI spec", method.toUpperCase(), path)
        .isFalse();

    if (expectedRequestRef == null) {
      assertThat(operation.path("requestBody").isMissingNode())
          .withFailMessage("Did not expect a request body for %s %s", method.toUpperCase(), path)
          .isTrue();
    } else {
      assertThat(
              operation
                  .path("requestBody")
                  .path("content")
                  .path("application/json")
                  .path("schema")
                  .path("$ref")
                  .asText())
          .withFailMessage("Unexpected request contract for %s %s", method.toUpperCase(), path)
          .isEqualTo(expectedRequestRef);
    }

    JsonNode responses = operation.path("responses");
    List<String> documentedResponseCodes = new ArrayList<>();
    responses.fieldNames().forEachRemaining(documentedResponseCodes::add);
    assertThat(responses.has(expectedResponseCode))
        .withFailMessage(
            "Expected %s response for %s %s but found %s",
            expectedResponseCode, method.toUpperCase(), path, documentedResponseCodes)
        .isTrue();

    JsonNode response = responses.path(expectedResponseCode);
    if (expectedResponseRef == null) {
      assertThat(response.path("content").isMissingNode() || response.path("content").isEmpty())
          .withFailMessage(
              "Did not expect a response body for %s %s %s",
              expectedResponseCode, method.toUpperCase(), path)
          .isTrue();
      return;
    }

    JsonNode content = response.path("content");
    JsonNode schema = content.path("*/*").path("schema");
    if (schema.isMissingNode()) {
      schema = content.path("application/json").path("schema");
    }
    assertThat(schema.path("$ref").asText())
        .withFailMessage(
            "Unexpected response contract for %s %s %s",
            expectedResponseCode, method.toUpperCase(), path)
        .isEqualTo(expectedResponseRef);
  }

  private void assertOperationMissing(JsonNode root, String path, String method) {
    JsonNode operation = root.path("paths").path(path).path(method);
    assertThat(operation.isMissingNode())
        .withFailMessage(
            "Did not expect %s %s in generated OpenAPI spec", method.toUpperCase(), path)
        .isTrue();
  }

  private void assertMultipartBinaryRequest(
      JsonNode root, String path, String method, String partName) {
    JsonNode operation = root.path("paths").path(path).path(method);
    assertThat(operation.isMissingNode())
        .withFailMessage("Missing %s %s from generated OpenAPI spec", method.toUpperCase(), path)
        .isFalse();

    JsonNode schema =
        operation.path("requestBody").path("content").path("multipart/form-data").path("schema");
    assertThat(schema.path("type").asText())
        .withFailMessage(
            "Expected multipart/form-data request schema for %s %s", method.toUpperCase(), path)
        .isEqualTo("object");
    assertThat(schema.path("properties").path(partName).path("type").asText())
        .withFailMessage(
            "Expected multipart part %s on %s %s", partName, method.toUpperCase(), path)
        .isEqualTo("string");
    assertThat(schema.path("properties").path(partName).path("format").asText())
        .withFailMessage(
            "Expected multipart part %s to be binary on %s %s",
            partName, method.toUpperCase(), path)
        .isEqualTo("binary");
  }

  private void assertOperationResponse(
      JsonNode root,
      String path,
      String method,
      String expectedResponseCode,
      String expectedResponseRef) {
    JsonNode operation = root.path("paths").path(path).path(method);
    assertThat(operation.isMissingNode())
        .withFailMessage("Missing %s %s from generated OpenAPI spec", method.toUpperCase(), path)
        .isFalse();

    JsonNode responses = operation.path("responses");
    List<String> documentedResponseCodes = new ArrayList<>();
    responses.fieldNames().forEachRemaining(documentedResponseCodes::add);
    assertThat(responses.has(expectedResponseCode))
        .withFailMessage(
            "Expected %s response for %s %s but found %s",
            expectedResponseCode, method.toUpperCase(), path, documentedResponseCodes)
        .isTrue();

    JsonNode content = responses.path(expectedResponseCode).path("content");
    JsonNode schema = content.path("*/*").path("schema");
    if (schema.isMissingNode()) {
      schema = content.path("application/json").path("schema");
    }
    assertThat(schema.path("$ref").asText())
        .withFailMessage(
            "Unexpected response contract for %s %s %s",
            expectedResponseCode, method.toUpperCase(), path)
        .isEqualTo(expectedResponseRef);
  }

  private void assertBinaryOperationResponse(
      JsonNode root, String path, String method, String expectedResponseCode) {
    JsonNode operation = root.path("paths").path(path).path(method);
    assertThat(operation.isMissingNode())
        .withFailMessage("Missing %s %s from generated OpenAPI spec", method.toUpperCase(), path)
        .isFalse();

    JsonNode responses = operation.path("responses");
    assertThat(responses.has(expectedResponseCode))
        .withFailMessage(
            "Expected %s response for %s %s", expectedResponseCode, method.toUpperCase(), path)
        .isTrue();

    JsonNode response = responses.path(expectedResponseCode);
    JsonNode content = response.path("content");
    JsonNode schema = content.path("application/pdf").path("schema");
    if (schema.isMissingNode() || schema.isEmpty()) {
      schema = content.path("*/*").path("schema");
    }
    assertThat(schema.path("type").asText())
        .withFailMessage(
            "Expected binary response schema type for %s %s %s",
            expectedResponseCode, method.toUpperCase(), path)
        .isEqualTo("string");
    assertThat(schema.path("format").asText())
        .withFailMessage(
            "Expected binary response schema format for %s %s %s",
            expectedResponseCode, method.toUpperCase(), path)
        .isIn("binary", "byte", "");
  }

  private void assertQueryParameter(
      JsonNode root, String path, String method, String parameterName) {
    JsonNode parameters = root.path("paths").path(path).path(method).path("parameters");
    assertThat(parameters.isArray())
        .withFailMessage("Expected query/header parameters on %s %s", method.toUpperCase(), path)
        .isTrue();

    boolean found = false;
    for (JsonNode parameter : parameters) {
      if (parameterName.equals(parameter.path("name").asText())) {
        found = true;
        break;
      }
    }

    assertThat(found)
        .withFailMessage(
            "Expected parameter '%s' on %s %s", parameterName, method.toUpperCase(), path)
        .isTrue();
  }
}
