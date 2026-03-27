# BigBright ERP Feature-Function Mapping

This directory contains comprehensive mapping of ALL ERP functions and features organized by module.

## Purpose

Quick reference to understand the full scope of the ERP system, including:
- Business functions and operational functions
- User management and authentication
- Multi-tenant management
- System configuration and feature flags
- All cross-cutting concerns

## Structure

```
FEATURE-function-mapping/
├── README.md (this file)
├── MODULE_FUNCTION_MAPPING.md (complete function inventory)
├── diagrams/ (placeholder for Figma diagram links)
└── cross-cutting/
    ├── AUTH_FUNCTIONS.md
    ├── SECURITY_FUNCTIONS.md
    ├── AUDIT_FUNCTIONS.md
    └── INTEGRATION_FUNCTIONS.md
```

## Quick Links

### By Module
- [Auth Module](./auth-functions.md)
- [Company Module](./company-functions.md)
- [HR Module](./hr-functions.md)
- [Inventory Module](./inventory-functions.md)
- [Invoice Module](./invoice-functions.md)
- [Portal Module](./portal-functions.md)
- [Reports Module](./reports-functions.md)
- [Factory Module](./factory-functions.md)
- [Purchasing Module](./purchasing-functions.md)
- [Production Module](./production-functions.md)
- [RBAC Module](./rbac-functions.md)
- [Admin Module](./admin-functions.md)
- [Sales Module](./sales-functions.md)
- [Accounting Module](./accounting-functions.md)

### By Function Type
- [Authentication & Security](./cross-cutting/AUTH_FUNCTIONS.md)
- [Audit & Compliance](./cross-cutting/AUDIT_FUNCTIONS.md)
- [Multi-tenant Management](./cross-cutting/TENANT_FUNCTIONS.md)
- [PDF & Document Generation](./cross-cutting/DOCUMENT_FUNCTIONS.md)
- [Export & Report Functions](./cross-cutting/EXPORT_FUNCTIONS.md)
- [Approval Workflows](./cross-cutting/APPROVAL_FUNCTIONS.md)
- [Reconciliation Functions](./cross-cutting/RECONCILIATION_FUNCTIONS.md)
- [Period Management Functions](./cross-cutting/PERIOD_FUNCTIONS.md)
- [Support Ticket Functions](./cross-cutting/SUPPORT_FUNCTIONS.md)
- [Feature Flag Functions](./cross-cutting/FEATURE_FLAG_FUNCTIONS.md)
- [External Integration Functions](./cross-cutting/INTEGRATION_FUNCTIONS.md)

## Statistics

| Category | Count |
|----------|-------|
| **Total Modules** | 15 |
| **Total Services** | ~150+ |
| **Total Controllers** | ~50+ |
| **Total Endpoints** | ~300+ |

## Related Documentation

- [MASTER_INDEX.md](../MASTER_INDEX.md) - Complete class listing
- [CANONICALITY_MAP.md](../CANONICALITY_MAP.md) - Status of each component
- [DOMAIN_INVARIANTS.md](../DOMAIN_INVARIANTS.md) - Business rules
- [FIGMA_DIAGRAMS.md](../FIGMA_DIAGRAMS.md) - Visual flow diagrams
