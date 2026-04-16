# Seed UAT Runtime R2 Evidence Bundle

Captured on: 2026-04-17
Branch: `seed-uat-runtime`
Base: `6bdfef50af3340489134068f61220095792b7e60`

## Commands

1. `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuthServiceAuditAttributionTest,TenantAdminProvisioningServiceTest,CompanyServiceTest,TenantOnboardingServiceTest test`
2. `bash scripts/gate_fast.sh`
3. `bash ci/check-enterprise-policy.sh`
4. `bash ci/check-codex-review-guidelines.sh`
5. `bash ci/lint-knowledgebase.sh`

## Result anchors

- `focused-auth-company-uat-tests.txt` includes the expected warning-only logout blacklist stack trace and ends with `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`.
- `gate-fast.txt` ends with `[gate-fast] OK`.
- `check-enterprise-policy.txt` ends with `[enterprise-policy] OK`.
- `check-codex-review-guidelines.txt` ends with `[codex-review-guidelines] OK`.
- `lint-knowledgebase.txt` ends with `[knowledgebase-lint] OK`.

## Notes

- `gate_fast` still reports changed-file coverage compatibility warnings for `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/UatSeedDataInitializer.java`; this bundle therefore proves the packet through composed auth/company tests plus a green baseline, not dedicated line-level coverage of the new seed initializer.
- The evidence bundle exists to unblock scrutiny governance for the already-green baseline stabilization milestone on the active branch.

## SHA-256

- `991b3f8623516c1f7b3490514bf4926f120d492eb2ae22db997b3b202c379db2` `focused-auth-company-uat-tests.txt`
- `713f3f0dccaa7c5ac8f063be6fb41c4bbdb8ec3e78f2c275e48907363b864418` `gate-fast.txt`
- `b77449a75ea328df9d7dfe4bd82d670cb7c3d095bd4f0cf82b628a9d06d7cf82` `check-enterprise-policy.txt`
- `9bd2556502e3cdd0a1163bfbac4af5886b0d362eb0d1433b34c84b0c4de330b9` `check-codex-review-guidelines.txt`
- `b82dc6dd20061f528ef2ed3f112be357554a2b8cc058b683b7edbb3eac9f337d` `lint-knowledgebase.txt`
