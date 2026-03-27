# BigBright ERP Codebase Dictionary

This directory contains comprehensive documentation for the BigBright ERP codebase, designed to serve as a decision-support system for both humans and AI agents.

## Quick Start

1. **For Developers**: Start with `core-infrastructure/` to understand the foundational components
2. **For AI Agents**: Read the specific module docs when working on features
3. **For Architecture Review**: Check `quality-reports/` for duplicate detection and recommendations

## Directory Structure

```
docs/codebase-dictionary/
├── README.md                          # This file
├── core-infrastructure/
│   ├── UTILITIES.md                  # All utility classes
│   ├── CONFIGS.md                    # All configuration classes
│   ├── SECURITY.md                   # Security filters and auth
│   ├── EXCEPTION_HANDLING.md         # Exception chain
│   ├── AUDIT_FRAMEWORK.md            # Audit services
│   ├── IDEMPOTENCY_FRAMEWORK.md      # Idempotency utilities
│   └── ORCHESTRATOR.md               # Orchestrator layer
├── modules/
│   ├── accounting/
│   ├── sales/
│   └── ... (15 modules total)
├── dependency-maps/
│   ├── SERVICE_DEPENDENCY_GRAPH.md
│   ├── MODULE_DEPENDENCIES.md
│   └── DATABASE_RELATIONSHIPS.md
└── quality-reports/
    ├── DUPLICATES.md
    ├── DEAD_CODE.md
    ├── CONFLICTS.md
    └── RECOMMENDATIONS.md
```

## Documentation Schema

Every component is documented with:

| Field | Description |
|-------|-------------|
| Name | Class name |
| Type | Service/Controller/Repository/Helper/DTO/Entity/Config |
| Module | Which module owns this (core/sales/accounting/etc.) |
| Package | Full Java package path |
| File | Path relative to repository root |
| Responsibility | Single-sentence description of purpose |
| Use when | When this is the right choice |
| Do not use when | When to use something else |
| Public methods | Exact method signatures |
| Callers | Classes that call this component |
| Dependencies | Injected beans and their purposes |
| Side effects | DB writes, events published, external calls |
| Status | Canonical/Scoped/Legacy/Duplicate-risk/Deprecated |

## Canonicality Status

- **Canonical**: Primary implementation, use this
- **Scoped**: Valid only in specific workflow/area
- **Legacy**: Still used, avoid new usage
- **Duplicate-risk**: Overlaps with other code
- **Deprecated**: Phase out

## The 8 Questions

Every entry answers:
1. What is it?
2. Why does it exist?
3. When should it be used?
4. When should it NOT be used?
5. Who calls it?
6. What does it depend on?
7. What invariants does it protect?
8. Is it Canonical/Scoped/Legacy/Duplicate-risk/Deprecated?

## Module Count

| Package | Files | Purpose |
|---------|-------|---------|
| core/util | 10 | General utilities |
| core/config | 18 | Spring configuration |
| core/security | 15 | Authentication and authorization |
| core/exception | 8 | Exception handling |
| core/audit | 8 | Audit logging |
| core/audittrail | 12 | Enterprise audit trail |
| core/idempotency | 3 | Idempotency support |
| orchestrator | 35 | Command dispatch, events |
| accounting | 145 | Financial accounting |
| sales | 74 | Sales orders, dealers |
| inventory | 71 | Stock management |
| factory | 69 | Manufacturing |
| purchasing | 48 | Procurement |
| auth | 47 | Authentication |
| hr | 47 | HR and payroll |
| admin | 39 | Admin functions |
| company | 37 | Multi-tenant |
| production | 33 | Product catalog |
| reports | 28 | Financial reports |
| invoice | 14 | Invoice generation |
| portal | 10 | Dealer portal |
| rbac | 11 | Role-based access |

**Total: ~858 Java files**

## Contributing

When adding new components:
1. Follow the standard documentation schema
2. Extract exact method signatures from source code
3. Identify callers through usage search
4. Determine canonicality status
5. Document any invariants protected
