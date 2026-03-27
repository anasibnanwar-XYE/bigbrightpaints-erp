# Living Documentation Protocol

This document defines how the Codebase Dictionary stays accurate without becoming stale or overhead-heavy.

---

## Core Principle

**Documentation is only valuable if it reflects reality.**
**Documentation that isn't updated is worse than no documentation.**

---

## Update Triggers

When ANY of these events occur, the relevant documentation MUST be updated:

### Must Update (Non-Negotiable)

| Trigger | What to Update | Who Updates |
|---------|----------------|--------------|
| New class/service created | Add entry to MASTER_INDEX.md, module docs | Creating agent |
| Method signature changed | Update SERVICES.md/CONTROLLERS.md | Modifying agent |
| New REST endpoint added | Add to ENTRY_POINT_MAP.md, module CONTROLLERS.md | Creating agent |
| New migration created | Add to MIGRATION_LOG.md | Creating agent |
| Class deprecated/removed | Mark status in CANONICALITY_MAP.md, update MASTER_INDEX.md | Modifying agent |
| New dependency added | Update SERVICE_DEPENDENCY_GRAPH.md | Modifying agent |
| New test created | Add to TEST_INVENTORY.json | Creating agent |

### Update Format

```markdown
<!-- Last updated: 2026-03-27 by @agent-name -->
<!-- Trigger: NEW_CLASS | METHOD_CHANGE | ENDPOINT_ADDED | MIGRATION_ADDED -->
<!-- Commit: abc12345 -->
```

---

## Migration Tracking Protocol

**CRITICAL:** Every agent that uses a Flyway migration MUST log it.

### Rule: One Source of Truth for Migrations

All migrations are tracked in a single file: `MIGRATION_LOG.md`

### Required Logging

When an agent creates or uses a migration, they MUST add an entry:

```markdown
### V2_167__Add_New_Field_To_Sales_Order.sql
- **Created by:** @agent-name
- **Date:** 2026-03-27
- **Purpose:** Add `discount_percentage` field to `sales_order` table
- **Used by:** SalesOrderCrudService, SalesFulfillmentService
- **Rollback:** DROP COLUMN discount_percentage
- **Conflicts:** None (new column)
- **Status:** Active
```

### Conflict Detection

If two agents create migrations with similar numbers:

```markdown
### CONFLICT DETECTED: V2_167
- **Agent A:** V2_167__Add_New_Field_To_Sales_Order.sql
- **Agent B:** V2_167__Update_Inventory_Reservation.sql
- **Resolution:** Agent B renumbers to V2_168
- **Resolved by:** @agent-b
- **Date:** 2026-03-27
```

---

## No Fallbacks Policy

### The Problem with Fallbacks

AI agents tend to add "fallback" logic:
- "If X fails, try Y"
- "Default to Z if not found"
- "Return empty list instead of error"

**This is PROHIBITED** because:
1. Fallbacks hide bugs
2. Fallbacks create silent data corruption
3. Fallbacks make debugging impossible
4. Fallbacks violate invariants silently

### What to Do Instead

| Situation | Wrong (Fallback) | Right (Explicit Failure) |
|-----------|-------------------|------------------------|
| Service unavailable | Return cached value | Throw `ServiceUnavailableException` |
| Entity not found | Return null | Throw `EntityNotFoundException` |
| Validation fails | Skip validation | Throw `ValidationException` |
| External API fails | Use mock data | Throw `ExternalApiException` |
| Configuration missing | Use default | Throw `ConfigurationException` |

### Exceptions are Good

```java
// WRONG - Silent failure
public Account getAccount(Long id) {
    Account account = repository.findById(id);
    if (account == null) {
        log.warn("Account not found, returning default");
        return new Account(); // NEVER DO THIS
    }
    return account;
}

// RIGHT - Explicit failure
public Account getAccount(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            "Account not found with id: " + id, 
            ErrorCode.ACCOUNT_NOT_FOUND
        ));
}
```

### When Fallbacks ARE Allowed

Only these specific cases:
1. **Optional display data** - UI can show "N/A" if data missing
2. **Cache misses** - Fall back to database (but log the miss)
3. **Feature flags** - Default to safe behavior if flag unreadable

---

## Agent Usage Guide

### Planning Agents (High Context Usage)

Planning agents SHOULD read:
- `AI_CONTEXT.md` - Quick reference
- `WHERE_SHOULD_NEW_CODE_GO.md` - Where to add code
- `DOMAIN_INVARIANTS.md` - What rules to follow
- `CROSS_MODULE_DEPENDENCIES.md` - Impact of changes

Planning agents SHOULD NOT:
- Read every module file
- Re-analyze existing documentation
- Re-document existing code

### Implementation Agents (Minimal Context Usage)

Implementation agents MUST update (after code changes):
1. Add entry to MASTER_INDEX.md if new class
2. Update relevant module file (SERVICES.md, CONTROLLERS.md, etc.)
3. Log migration in MIGRATION_LOG.md if created
4. Add test entry in TEST_INVENTORY.json if new test

**Time limit:** 2 minutes max for documentation updates

### Update Checklist for Implementation Agents

```markdown
## Doc Update Checklist

- [ ] New class? → Added to MASTER_INDEX.md
- [ ] New/changed methods? → Updated module SERVICES.md/CONTROLLERS.md
- [ ] New endpoint? → Updated ENTRY_POINT_MAP.md
- [ ] New migration? → Logged in MIGRATION_LOG.md
- [ ] New test? → Added to TEST_INVENTORY.json
- [ ] Status changed? → Updated CANONICALITY_MAP.md
- [ ] Commit SHA added to entries modified
```

---

## Documentation Confidence Levels

Every entry MUST include a confidence level:

| Level | Badge | When to Use | Update Frequency |
|-------|-------|-------------|-------------------|
| **EXACT** | `![EXACT]` | Extracted from source code within 30 days | Re-verify every 30 days |
| **INFERRED** | `![INFERRED]` | Reasonable inference from code patterns | Re-verify on next touch |
| **PARTIAL** | `![PARTIAL]` | Some aspects documented, others pending | Complete on next touch |
| **STALE-RISK** | `![STALE-RISK]` | Not verified in 30+ days | Re-verify immediately |

### Adding Confidence to Entries

```markdown
### SalesOrderCrudService

| Field | Value |
|-------|-------|
| **Name** | SalesOrderCrudService |
| **Confidence** | ![EXACT] (verified 2026-03-27) |
| **Last Commit** | abc12345 |
...
```

---

## Quick Update Commands

### After Creating a New Service

```bash
# Add to master index
echo "- SalesOrderBulkService | Service | sales | com.bigbrightpaints.erp.modules.sales.service | Bulk order operations | Canonical" >> docs/codebase-dictionary/MASTER_INDEX.md

# Add to module services
# (Edit docs/codebase-dictionary/modules/sales/SERVICES.md)
```

### After Creating a New Migration

```bash
# Log the migration
cat >> docs/codebase-dictionary/MIGRATION_LOG.md << EOF

### V2_XXX__Description.sql
- **Created by:** @your-agent-name
- **Date:** $(date +%Y-%m-%d)
- **Purpose:** [What it does]
- **Used by:** [Which services]
- **Conflicts:** [Any conflicts or "None"]
- **Status:** Active
EOF
```

---

## Staleness Detection

Run this weekly to detect stale docs:

```bash
# Find entries not updated in 30 days
find docs/codebase-dictionary -name "*.md" -mtime +30

# Find entries without confidence level
grep -L "Confidence" docs/codebase-dictionary/**/*.md
```

---

## Summary

| What | Who | When | Time Budget |
|------|-----|------|--------------|
| Create new doc entries | Creating agent | At creation time | 2 min max |
| Update changed entries | Modifying agent | At modification time | 1 min max |
| Log migrations | Creating agent | At migration time | 30 sec max |
| Re-verify stale entries | First touching agent | On first touch | 5 min max |
| Weekly staleness check | Automated/Planning agent | Weekly | 5 min max |

**Goal:** Accurate docs without overhead. Truth, not assumptions.
