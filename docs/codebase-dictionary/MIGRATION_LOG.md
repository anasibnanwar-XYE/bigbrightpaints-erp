# Migration Log

This file tracks ALL Flyway v2 migrations. **Every agent must log their migrations here.**

---

## Active Migrations

### V2_001__Initial_Schema_Setup.sql
- **Created by:** System
- **Date:** 2024-01-15
- **Purpose:** Initial schema setup for multi-tenant ERP
- **Used by:** All modules (base tables)
- **Status:** Active

### V2_002__Company_And_User_Tables.sql
- **Created by:** System
- **Date:** 2024-01-15
- **Purpose:** Company, User, Role, Permission tables
- **Used by:** auth, company, rbac modules
- **Status:** Active

### V2_003__Accounting_Foundation.sql
- **Created by:** System
- **Date:** 2024-01-20
- **Purpose:** Account, JournalEntry, JournalLine, AccountingPeriod tables
- **Used by:** accounting module
- **Status:** Active

### V2_004__Sales_Module_Tables.sql
- **Created by:** System
- **Date:** 2024-01-25
- **Purpose:** SalesOrder, Dealer, Credit tables
- **Used by:** sales module
- **Status:** Active

### V2_005__Inventory_Module_Tables.sql
- **Created by:** System
- **Date:** 2024-02-01
- **Purpose:** FinishedGood, RawMaterial, InventoryMovement, Batch tables
- **Used by:** inventory module
- **Status:** Active

### V2_006__Factory_Module_Tables.sql
- **Created by:** System
- **Date:** 2024-02-05
- **Purpose:** ProductionLog, PackingRecord, ProductionBatch tables
- **Used by:** factory module
- **Status:** Active

---

## Conflict Detection Zone

*No conflicts recorded yet*

---

## Migration Naming Convention

```
V2_{number}__{Description}.sql

Examples:
- V2_010__Add_Discount_Field_To_Sales_Order.sql
- V2_011__Create_Purchase_Return_Table.sql
- V2_012__Add_Gst_Rate_To_Product.sql
```

---

## How to Log Your Migration

When you create a migration, ADD AN ENTRY at the TOP of the "Active Migrations" section:

```markdown
### V2_XXX__Your_Migration_Description.sql
- **Created by:** @your-agent-name
- **Date:** YYYY-MM-DD
- **Purpose:** What this migration does
- **Used by:** Which services/modules use this
- **Conflicts:** None | V2_YYY (if conflict detected)
- **Status:** Active | Rolled Back | Superseded
```

---

## Pre-Migration Checklist

Before creating a new migration:

1. **Check this file** - Is there already a similar migration?
2. **Check the number** - Get the next available V2_XXX number
3. **Check for conflicts** - Will this affect existing tables used by other modules?
4. **Plan rollback** - Document how to rollback if needed

---

## Rollback Registry

When migrations are rolled back, move them here:

### Rolled Back Migrations

*None yet*

---

## Superseded Migrations

When a migration is replaced by a newer one, document here:

### Superseded Migrations

*None yet*
