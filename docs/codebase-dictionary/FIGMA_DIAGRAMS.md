# Figma Diagrams - BigBright ERP Codebase Dictionary

This file contains links to all canonical flow diagrams created in Figma for the BigBright ERP system.

## Canonical Flow Diagrams

### 1. Order-to-Cash (O2C) Flow
**Description:** Complete sales order lifecycle from creation to settlement
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/708cae92-c502-4a2c-855b-39bfc17812af?utm_source=other&utm_content=edit_in_figjam)

**Key Steps:**
- Sales Order Created
- Credit Check
- Reserve Inventory
- Dispatch Preview & Confirmation
- Invoice Generation
- Accounting Posting
- Settlement

---

### 2. Procure-to-Pay (P2P) Flow
**Description:** Complete procurement lifecycle from PO creation to payment
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/6cae59d0-c686-4ec0-96eb-013827910689?utm_source=other&utm_content=edit_in_figjam)

**Key Steps:**
- Create Purchase Order
- Approval Workflow
- Goods Receipt
- Quality Check
- Inventory Update
- Purchase Invoice
- 3-Way Match
- Payment Processing

---

### 3. Manufacturing-to-Stock (M2S) Flow
**Description:** Production execution from raw material issue to finished goods
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/df52cb16-ab14-420a-965c-6155074ea542?utm_source=other&utm_content=edit_in_figjam)

**Key Steps:**
- Production Log Creation
- Raw Material Issue
- Production Execution
- Packing Request & Execution
- Finished Goods Registration
- Cost Allocation
- Inventory Valuation Update

---

### 4. Payroll-to-Accounting Flow
**Description:** Payroll processing with Indian statutory deductions
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/cfc8e52f-0228-4cae-abe5-856212522a83?utm_source=other&utm_content=edit_in_figjam)

**Key Steps:**
- Payroll Run Initiation
- Employee Data Load
- Gross Salary Calculation
- Statutory Deductions (PF, ESI, TDS, PT)
- Net Salary Calculation
- Accounting Posting

---

### 5. Authentication Flow
**Description:** User authentication with MFA support
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/6989008c-d3f3-4012-9f3a-5621994e2b67?utm_source=other&utm_content=edit_in_figjam)

**Key Steps:**
- Login Request
- Credential Validation
- MFA Verification (if enabled)
- JWT Token Generation
- Password Reset Flow

---

### 6. Module Dependency Graph
**Description:** System architecture showing module dependencies
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/3c043860-3f25-42ac-88a0-2f6c3f3a458c?utm_source=other&utm_content=edit_in_figjam)

**Module Groups:**
- Core Infrastructure (Core, Security, Audit, Idempotency)
- Business Modules (Auth, Company, RBAC, Admin)
- Operations Modules (Sales, Inventory, Factory, Purchasing, Production)
- Finance Modules (Accounting, Invoice, Reports, HR/Payroll)
- External (Portal)

---

## How to Use These Diagrams

1. **For Development:** Reference these diagrams when implementing new features to ensure alignment with canonical flows
2. **For Code Review:** Use as a checklist to verify changes don't break existing flow invariants
3. **For Onboarding:** New team members can quickly understand system architecture and data flow
4. **For AI Agents:** These diagrams provide visual context that complements the written documentation

### 7. Complete E2E ERP Lifecycle
**Description:** Full end-to-end flow from company onboarding to accounting period close
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/164fc842-4d50-4f8c-8bf0-0ae78d154ebf?utm_source=other&utm_content=edit_in_figjam)

**Key Phases:**
- Company Onboarding & User Setup
- Master Data (Brands, Products, Variants)
- Sales Orders & Dispatch
- Purchasing & Inventory
- Manufacturing & Costing
- Accounting & Period Close

---

### 8. Backend Architecture Diagram
**Description:** System architecture showing layers and module relationships
[View Diagram](https://www.figma.com/online-whiteboard/create-diagram/f6a17a8a-bd19-4e4c-ad54-cf317f09987d?utm_source=other&utm_content=edit_in_figjam)

**Layers:**
- Controllers (REST API)
- Services (Business Logic)
- Repositories (Data Access)
- Database (PostgreSQL)
- Cross-cutting: Security, Audit, Events

---

## Related Documentation

- [MASTER_INDEX.md](./MASTER_INDEX.md) - Complete class listing
- [CANONICALITY_MAP.md](./CANONICALITY_MAP.md) - Status of each component
- [DOMAIN_INVARIANTS.md](./DOMAIN_INVARIANTS.md) - Business rules protected by each flow
- [WHERE_SHOULD_NEW_CODE_GO.md](./WHERE_SHOULD_NEW_CODE_GO.md) - Extension point guide
