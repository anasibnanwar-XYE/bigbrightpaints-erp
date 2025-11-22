# Requestify Premium Testing Guide - ERP Backend

**Date**: November 18, 2025
**Base URL**: `http://localhost:8081` (default) or `http://localhost:${SERVER_PORT}`
**API Version**: v1

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [Authentication Flow](#2-authentication-flow)
3. [Common Headers](#3-common-headers)
4. [Module-Specific Endpoints](#4-module-specific-endpoints)
5. [Testing Workflows](#5-testing-workflows)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Getting Started

### Prerequisites

1. ✅ ERP Backend running on `http://localhost:8081`
2. ✅ Requestify Premium installed
3. ✅ Database populated with seed data

### Environment Setup in Requestify

Create a new environment in Requestify:

**Environment Name**: `ERP Local`

**Variables**:
```json
{
  "baseUrl": "http://localhost:8081",
  "apiVersion": "v1",
  "accessToken": "",
  "refreshToken": "",
  "companyId": "1"
}
```

---

## 2. Authentication Flow

### Step 1: Login to Get Tokens

**Endpoint**: `POST {{baseUrl}}/api/{{apiVersion}}/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Body** (JSON):
```json
{
  "username": "admin@bigbrightpaints.com",
  "password": "admin123",
  "companyId": 1
}
```

**Expected Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "username": "admin@bigbrightpaints.com",
    "email": "admin@bigbrightpaints.com",
    "roles": ["ADMIN", "USER"]
  }
}
```

**Requestify Actions**:
1. After successful login, copy `accessToken` value
2. Set environment variable: `accessToken` = (paste token here)
3. Copy `refreshToken` value
4. Set environment variable: `refreshToken` = (paste token here)

---

### Step 2: Refresh Token (When Access Token Expires)

**Endpoint**: `POST {{baseUrl}}/api/{{apiVersion}}/auth/refresh-token`

**Headers**:
```
Content-Type: application/json
```

**Body** (JSON):
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Expected Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

---

### Step 3: Get Current User Info

**Endpoint**: `GET {{baseUrl}}/api/{{apiVersion}}/auth/me`

**Headers**:
```
Authorization: Bearer {{accessToken}}
```

**Expected Response** (200 OK):
```json
{
  "id": 1,
  "username": "admin@bigbrightpaints.com",
  "email": "admin@bigbrightpaints.com",
  "firstName": "Admin",
  "lastName": "User",
  "roles": ["ADMIN", "USER"],
  "company": {
    "id": 1,
    "name": "Big Bright Paints Ltd."
  }
}
```

---

## 3. Common Headers

### For All Authenticated Requests

```
Authorization: Bearer {{accessToken}}
Content-Type: application/json
Accept: application/json
```

### For Multi-Company Operations

```
Authorization: Bearer {{accessToken}}
X-Company-Id: {{companyId}}
Content-Type: application/json
```

---

## 4. Module-Specific Endpoints

### 4.1 Accounting Module

**Base Path**: `/api/v1/accounting`

#### Get Chart of Accounts

```http
GET {{baseUrl}}/api/v1/accounting/accounts
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
[
  {
    "id": 1,
    "code": "1000",
    "name": "Cash",
    "type": "ASSET",
    "balance": 50000.00,
    "parentAccountId": null
  },
  {
    "id": 2,
    "code": "1100",
    "name": "Accounts Receivable",
    "type": "ASSET",
    "balance": 25000.00,
    "parentAccountId": null
  }
]
```

---

#### Create Journal Entry

```http
POST {{baseUrl}}/api/v1/accounting/journal-entries
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

**Body**:
```json
{
  "entryDate": "2025-11-18",
  "referenceNumber": "JE-001",
  "memo": "Test journal entry",
  "lines": [
    {
      "accountId": 1,
      "description": "Cash payment received",
      "debit": 1000.00,
      "credit": 0.00
    },
    {
      "accountId": 2,
      "description": "Revenue earned",
      "debit": 0.00,
      "credit": 1000.00
    }
  ]
}
```

**Response** (201 Created):
```json
{
  "id": 123,
  "entryDate": "2025-11-18",
  "referenceNumber": "JE-001",
  "memo": "Test journal entry",
  "status": "POSTED",
  "lines": [
    {
      "id": 456,
      "accountCode": "1000",
      "accountName": "Cash",
      "debit": 1000.00,
      "credit": 0.00
    },
    {
      "id": 457,
      "accountCode": "1100",
      "accountName": "Accounts Receivable",
      "debit": 0.00,
      "credit": 1000.00
    }
  ],
  "totalDebit": 1000.00,
  "totalCredit": 1000.00
}
```

---

#### Get Journal Entries (with pagination)

```http
GET {{baseUrl}}/api/v1/accounting/journal-entries?page=0&size=20
Authorization: Bearer {{accessToken}}
```

---

### 4.2 Sales Module

**Base Path**: `/api/v1/sales`

#### Get All Sales Orders

```http
GET {{baseUrl}}/api/v1/sales/orders
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
[
  {
    "id": 1,
    "orderNumber": "SO-2025-001",
    "dealer": {
      "id": 1,
      "name": "ABC Dealers"
    },
    "orderDate": "2025-11-15",
    "status": "PENDING",
    "totalAmount": 50000.00,
    "items": [
      {
        "productCode": "PAINT-001",
        "productName": "Premium Paint Red 1L",
        "quantity": 100,
        "unitPrice": 500.00,
        "totalPrice": 50000.00
      }
    ]
  }
]
```

---

#### Create Sales Order

```http
POST {{baseUrl}}/api/v1/sales/orders
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

**Body**:
```json
{
  "dealerId": 1,
  "orderDate": "2025-11-18",
  "deliveryDate": "2025-11-25",
  "items": [
    {
      "productId": 1,
      "quantity": 50,
      "unitPrice": 500.00
    },
    {
      "productId": 2,
      "quantity": 30,
      "unitPrice": 750.00
    }
  ],
  "notes": "Urgent order"
}
```

**Response** (201 Created):
```json
{
  "id": 123,
  "orderNumber": "SO-2025-123",
  "dealer": {
    "id": 1,
    "name": "ABC Dealers"
  },
  "status": "PENDING",
  "totalAmount": 47500.00,
  "items": [...]
}
```

---

#### Confirm Sales Order

```http
POST {{baseUrl}}/api/v1/sales/orders/123/confirm
Authorization: Bearer {{accessToken}}
```

**Response** (200 OK):
```json
{
  "id": 123,
  "orderNumber": "SO-2025-123",
  "status": "CONFIRMED",
  "message": "Order confirmed successfully"
}
```

---

### 4.3 Dealers Module

**Base Path**: `/api/v1/dealers`

#### Get All Dealers

```http
GET {{baseUrl}}/api/v1/dealers
Authorization: Bearer {{accessToken}}
```

---

#### Create New Dealer

```http
POST {{baseUrl}}/api/v1/dealers
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

**Body**:
```json
{
  "name": "New Dealer LLC",
  "contactPerson": "John Doe",
  "email": "john@newdealer.com",
  "phone": "+92-300-1234567",
  "address": "123 Main Street, Karachi",
  "creditLimit": 100000.00,
  "paymentTerms": "NET_30"
}
```

---

#### Search Dealers

```http
GET {{baseUrl}}/api/v1/dealers/search?query=ABC
Authorization: Bearer {{accessToken}}
```

---

### 4.4 Production Module

**Base Path**: `/api/v1/factory`

#### Get Production Logs

```http
GET {{baseUrl}}/api/v1/factory/production/logs
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
[
  {
    "id": 1,
    "productionCode": "PROD-B-001",
    "productName": "Premium Paint Red",
    "mixedQuantity": 50.00,
    "totalPackedQuantity": 50.00,
    "wastageQuantity": 0.00,
    "materialCostTotal": 1600.00,
    "laborCostTotal": 0.00,
    "overheadCostTotal": 0.00,
    "unitCost": 32.00,
    "status": "PACKED",
    "producedAt": "2025-11-17T10:30:00Z"
  }
]
```

---

### 4.5 Inventory Module

**Base Path**: `/api/v1`

#### Get Raw Materials Stock

```http
GET {{baseUrl}}/api/v1/raw-materials/stock
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
[
  {
    "id": 1,
    "sku": "RM-PB-001",
    "name": "Paint Base",
    "currentStock": 500.00,
    "reservedStock": 50.00,
    "availableStock": 450.00,
    "unit": "LITERS",
    "averageCost": 150.00
  }
]
```

---

#### Get Low Stock Items

```http
GET {{baseUrl}}/api/v1/raw-materials/stock/low-stock
Authorization: Bearer {{accessToken}}
```

---

#### Create Inventory Adjustment

```http
POST {{baseUrl}}/api/v1/inventory/adjustments
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

**Body**:
```json
{
  "itemType": "RAW_MATERIAL",
  "itemId": 1,
  "adjustmentType": "INCREASE",
  "quantity": 10.00,
  "reason": "Physical count correction",
  "notes": "Found additional stock in warehouse"
}
```

---

### 4.6 Purchasing Module

**Base Path**: `/api/v1/purchasing`

#### Get Suppliers

```http
GET {{baseUrl}}/api/v1/purchasing/suppliers
Authorization: Bearer {{accessToken}}
```

---

#### Create Purchase Order

```http
POST {{baseUrl}}/api/v1/purchasing/orders
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

**Body**:
```json
{
  "supplierId": 1,
  "orderDate": "2025-11-18",
  "expectedDeliveryDate": "2025-11-25",
  "items": [
    {
      "rawMaterialId": 1,
      "quantity": 100.00,
      "unitPrice": 150.00
    }
  ],
  "notes": "Urgent requirement"
}
```

---

### 4.7 Reports Module

**Base Path**: `/api/v1/reports`

#### Get Balance Sheet

```http
GET {{baseUrl}}/api/v1/reports/balance-sheet?asOfDate=2025-11-18
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
{
  "asOfDate": "2025-11-18",
  "assets": {
    "currentAssets": {
      "cash": 50000.00,
      "accountsReceivable": 25000.00,
      "inventory": 100000.00,
      "total": 175000.00
    },
    "totalAssets": 175000.00
  },
  "liabilities": {
    "currentLiabilities": {
      "accountsPayable": 30000.00,
      "total": 30000.00
    },
    "totalLiabilities": 30000.00
  },
  "equity": {
    "retainedEarnings": 145000.00,
    "totalEquity": 145000.00
  }
}
```

---

#### Get Profit & Loss Statement

```http
GET {{baseUrl}}/api/v1/reports/profit-loss?startDate=2025-11-01&endDate=2025-11-30
Authorization: Bearer {{accessToken}}
```

---

### 4.8 Orchestrator Module

**Base Path**: `/api/v1/orchestrator`

#### Auto-Approve Sales Order

```http
POST {{baseUrl}}/api/v1/orchestrator/orders/123/approve
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
{
  "orderId": "123",
  "orderStatus": "READY_TO_SHIP",
  "awaitingProduction": false,
  "message": "Order auto-approved and ready for shipment"
}
```

---

#### Finalize Order Fulfillment

```http
POST {{baseUrl}}/api/v1/orchestrator/orders/123/fulfillment
Authorization: Bearer {{accessToken}}
```

---

#### Get Dashboard Metrics

```http
GET {{baseUrl}}/api/v1/orchestrator/dashboard/admin
Authorization: Bearer {{accessToken}}
```

**Response**:
```json
{
  "totalRevenue": 500000.00,
  "totalOrders": 150,
  "pendingOrders": 25,
  "lowStockItems": 5,
  "outstandingReceivables": 75000.00
}
```

---

## 5. Testing Workflows

### Workflow 1: Complete Sales Order Process

**Purpose**: Test the full order-to-cash cycle

**Steps**:

1. **Login**
   ```http
   POST /api/v1/auth/login
   ```

2. **Create Dealer** (if needed)
   ```http
   POST /api/v1/dealers
   ```

3. **Check Inventory**
   ```http
   GET /api/v1/raw-materials/stock
   ```

4. **Create Sales Order**
   ```http
   POST /api/v1/sales/orders
   ```

5. **Auto-Approve Order**
   ```http
   POST /api/v1/orchestrator/orders/{orderId}/approve
   ```

6. **Finalize Shipment**
   ```http
   POST /api/v1/orchestrator/orders/{orderId}/fulfillment
   ```

7. **Verify Journal Entries**
   ```http
   GET /api/v1/accounting/journal-entries
   ```

8. **Check Dealer Balance**
   ```http
   GET /api/v1/dealers/{dealerId}
   ```

---

### Workflow 2: Production to Inventory

**Purpose**: Test production logging and inventory updates

**Steps**:

1. **Check Raw Material Stock**
   ```http
   GET /api/v1/raw-materials/stock
   ```

2. **Create Production Log**
   ```http
   POST /api/v1/factory/production/logs
   ```

3. **Record Packing**
   ```http
   POST /api/v1/factory/packing
   ```

4. **Verify Finished Goods Inventory**
   ```http
   GET /api/v1/inventory/finished-goods
   ```

5. **Check Material Consumption Journal**
   ```http
   GET /api/v1/accounting/journal-entries?reference=PROD-B-001-RM
   ```

---

### Workflow 3: Month-End Closing

**Purpose**: Test accounting period close process

**Steps**:

1. **Get Month-End Checklist**
   ```http
   GET /api/v1/accounting/month-end/checklist?year=2025&month=11
   ```

2. **Perform Physical Count**
   ```http
   POST /api/v1/accounting/inventory/physical-count
   ```

3. **Reconcile Bank Accounts**
   ```http
   POST /api/v1/accounting/bank-reconciliation
   ```

4. **Close Accounting Period**
   ```http
   POST /api/v1/accounting/periods/{periodId}/close
   ```

5. **Generate Reports**
   ```http
   GET /api/v1/reports/balance-sheet?asOfDate=2025-11-30
   GET /api/v1/reports/profit-loss?startDate=2025-11-01&endDate=2025-11-30
   ```

---

## 6. Troubleshooting

### Common Issues

#### Issue 1: 401 Unauthorized

**Symptom**:
```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required",
  "status": 401
}
```

**Solutions**:
1. ✅ Check that `Authorization` header is present
2. ✅ Verify token format: `Bearer {{accessToken}}`
3. ✅ Token may have expired - use refresh token endpoint
4. ✅ Re-login to get new tokens

---

#### Issue 2: 403 Forbidden

**Symptom**:
```json
{
  "error": "Forbidden",
  "message": "Access denied",
  "status": 403
}
```

**Solutions**:
1. ✅ User lacks required role/permission
2. ✅ Login with admin account for testing
3. ✅ Check RBAC configuration

---

#### Issue 3: 404 Not Found

**Symptom**:
```json
{
  "error": "Not Found",
  "message": "Resource not found",
  "status": 404
}
```

**Solutions**:
1. ✅ Check endpoint URL spelling
2. ✅ Verify resource ID exists
3. ✅ Check `X-Company-Id` header for multi-tenant endpoints

---

#### Issue 4: 422 Unprocessable Entity

**Symptom**:
```json
{
  "error": "Validation Failed",
  "message": "Invalid request body",
  "status": 422,
  "errors": [
    {
      "field": "dealerId",
      "message": "Dealer ID is required"
    }
  ]
}
```

**Solutions**:
1. ✅ Check request body matches expected schema
2. ✅ Verify all required fields are present
3. ✅ Check data types (numbers vs strings)
4. ✅ Validate date formats (ISO 8601: `YYYY-MM-DD`)

---

#### Issue 5: 500 Internal Server Error

**Symptom**:
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "status": 500
}
```

**Solutions**:
1. ✅ Check backend logs for stack trace
2. ✅ Verify database is running
3. ✅ Check RabbitMQ connection
4. ✅ Verify environment variables are set

---

## 7. Requestify Premium Features to Use

### Feature 1: Environment Variables

Set up multiple environments:
- **Local Dev**: `http://localhost:8081`
- **Staging**: `http://staging.bigbrightpaints.com:8081`
- **Production**: `https://api.bigbrightpaints.com`

---

### Feature 2: Collection Organization

Create collections by module:
```
📁 ERP Backend
  📁 01 - Authentication
    ├── Login
    ├── Refresh Token
    ├── Logout
    └── Get Current User
  📁 02 - Accounting
    ├── Get Accounts
    ├── Create Journal Entry
    └── Get Journal Entries
  📁 03 - Sales
    ├── Get Orders
    ├── Create Order
    ├── Confirm Order
    └── Cancel Order
  📁 04 - Production
    ├── Get Production Logs
    └── Create Production Log
  📁 05 - Reports
    ├── Balance Sheet
    └── Profit & Loss
```

---

### Feature 3: Pre-Request Scripts

Use Requestify's pre-request scripts to auto-refresh tokens:

```javascript
// Check if token is expired
const tokenExpiry = pm.environment.get("tokenExpiry");
const now = Date.now();

if (now >= tokenExpiry) {
  // Refresh token
  const refreshToken = pm.environment.get("refreshToken");

  pm.sendRequest({
    url: pm.environment.get("baseUrl") + "/api/v1/auth/refresh-token",
    method: "POST",
    header: { "Content-Type": "application/json" },
    body: {
      mode: "raw",
      raw: JSON.stringify({ refreshToken: refreshToken })
    }
  }, function(err, response) {
    if (!err) {
      const json = response.json();
      pm.environment.set("accessToken", json.accessToken);
      pm.environment.set("tokenExpiry", now + (json.expiresIn * 1000));
    }
  });
}
```

---

### Feature 4: Test Scripts

Add assertions to validate responses:

```javascript
// Test: Login successful
pm.test("Status code is 200", function() {
  pm.response.to.have.status(200);
});

pm.test("Response contains access token", function() {
  const json = pm.response.json();
  pm.expect(json.accessToken).to.be.a("string");
  pm.environment.set("accessToken", json.accessToken);
});

pm.test("Token expiry is set", function() {
  const json = pm.response.json();
  pm.expect(json.expiresIn).to.be.a("number");
  const expiry = Date.now() + (json.expiresIn * 1000);
  pm.environment.set("tokenExpiry", expiry);
});
```

---

### Feature 5: Bulk Testing

Use Collection Runner to execute entire workflows:
1. Select collection (e.g., "Complete Sales Order Process")
2. Choose environment (e.g., "ERP Local")
3. Set iterations (e.g., 10 orders)
4. Run collection
5. Review results

---

## 8. Quick Reference Card

### Base URL
```
http://localhost:8081
```

### Authentication
```
POST /api/v1/auth/login
POST /api/v1/auth/refresh-token
GET  /api/v1/auth/me
```

### Key Modules
```
/api/v1/accounting     → Chart of Accounts, Journal Entries
/api/v1/sales          → Sales Orders, Dealers
/api/v1/factory        → Production Logs, Packing
/api/v1/inventory      → Raw Materials, Finished Goods
/api/v1/purchasing     → Purchase Orders, Suppliers
/api/v1/reports        → Financial Reports
/api/v1/orchestrator   → Workflow Automation
```

### Common Headers
```
Authorization: Bearer {token}
X-Company-Id: {companyId}
Content-Type: application/json
Accept: application/json
```

---

*Happy Testing with Requestify Premium!* 🚀

---

**Need Help?**
- Check backend logs: `erp-domain/target/logs/`
- Review Spring Boot actuator: `http://localhost:9090/actuator/health`
- Consult API documentation: `http://localhost:8081/swagger-ui.html` (if enabled)
