# BigBright ERP - Frontend API Guide

**Backend URL:** `http://localhost:8081`  
**Swagger UI:** `http://localhost:8081/swagger-ui/index.html`  
**OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

---

## 1. Authentication

### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "mdanas7869292@gmail.com",
  "password": "Admin@12345"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "mdanas7869292@gmail.com",
      "displayName": "Md Anas",
      "roles": ["ROLE_ADMIN"]
    }
  }
}
```

### Using the Token
All protected endpoints require:
```http
Authorization: Bearer <accessToken>
```

### Refresh Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Logout
```http
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

---

## 2. MFA (Multi-Factor Authentication)

### Enable MFA
```http
POST /api/v1/mfa/enable
Authorization: Bearer <accessToken>
```
**Response:** Returns QR code URL for authenticator app setup.

### Verify MFA Setup
```http
POST /api/v1/mfa/verify
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "code": "123456"
}
```

### Login with MFA
After initial login returns `mfaRequired: true`:
```http
POST /api/v1/auth/mfa/verify
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456"
}
```

---

## 3. Password Management

### Forgot Password
```http
POST /api/v1/auth/password/forgot
Content-Type: application/json

{
  "email": "user@example.com"
}
```
Sends reset email with token.

### Reset Password
```http
POST /api/v1/auth/password/reset
Content-Type: application/json

{
  "token": "reset-token-from-email",
  "newPassword": "NewSecure@123",
  "confirmPassword": "NewSecure@123"
}
```

**Password Policy:** Minimum 10 characters.

---

## 4. Company Context

Most endpoints are **multi-tenant**. Set company context via header:
```http
X-Company-Id: 1
```

Or the backend uses the user's default company.

---

## 5. Accounting Portal

### Chart of Accounts
```http
GET /api/v1/accounting/accounts
Authorization: Bearer <token>
```

### Create Account
```http
POST /api/v1/accounting/accounts
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "CASH-001",
  "name": "Petty Cash",
  "type": "ASSET",
  "parentId": null
}
```

**Account Types:** `ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`

### Journal Entries
```http
GET /api/v1/accounting/journal-entries
POST /api/v1/accounting/journal-entries
```

**Create Journal Entry:**
```json
{
  "referenceNumber": "JE-2024-001",
  "entryDate": "2024-01-15",
  "memo": "Office supplies purchase",
  "autoApprove": true,
  "lines": [
    {"accountId": 1, "description": "Supplies", "debit": 500.00, "credit": 0},
    {"accountId": 2, "description": "Cash", "debit": 0, "credit": 500.00}
  ]
}
```

### Trial Balance
```http
GET /api/v1/accounting/trial-balance?asOf=2024-01-31
```

### P&L Report
```http
GET /api/v1/accounting/profit-loss?from=2024-01-01&to=2024-01-31
```

### Balance Sheet
```http
GET /api/v1/accounting/balance-sheet?asOf=2024-01-31
```

---

## 6. Sales Portal

### Dealers (Customers)
```http
GET /api/v1/dealers
POST /api/v1/dealers
GET /api/v1/dealers/{id}
PUT /api/v1/dealers/{id}
```

**Create Dealer:**
```json
{
  "name": "ABC Paints Shop",
  "email": "abc@example.com",
  "phone": "+91-9876543210",
  "gstin": "27AABCU9603R1ZM",
  "address": "123 Market Street",
  "creditLimit": 100000.00
}
```

### Sales Orders
```http
GET /api/v1/sales/orders
POST /api/v1/sales/orders
GET /api/v1/sales/orders/{id}
```

**Create Order:**
```json
{
  "dealerId": 1,
  "orderDate": "2024-01-15",
  "items": [
    {
      "productId": 1,
      "quantity": 10,
      "unitPrice": 500.00,
      "taxRate": 18.00
    }
  ],
  "notes": "Urgent delivery"
}
```

### Order Status Flow
`DRAFT` → `CONFIRMED` → `PROCESSING` → `DISPATCHED` → `DELIVERED`

---

## 7. Invoice Portal

### List Invoices
```http
GET /api/v1/invoices
GET /api/v1/invoices/{id}
GET /api/v1/invoices/dealers/{dealerId}
```

### Download Invoice PDF
```http
GET /api/v1/invoices/{id}/pdf
```
Returns `application/pdf` binary.

### Send Invoice Email
```http
POST /api/v1/invoices/{id}/email
Authorization: Bearer <token>
```
Sends HTML email with PDF attachment to dealer's email.

**Response:**
```json
{
  "success": true,
  "data": "Invoice email sent to dealer@example.com"
}
```

---

## 8. Inventory Portal

### Finished Goods
```http
GET /api/v1/inventory/finished-goods
POST /api/v1/inventory/finished-goods
GET /api/v1/inventory/finished-goods/{id}
```

### Stock Levels
```http
GET /api/v1/inventory/stock-levels
```

### Batches
```http
GET /api/v1/inventory/batches
POST /api/v1/inventory/batches
```

---

## 9. Purchasing Portal

### Suppliers
```http
GET /api/v1/suppliers
POST /api/v1/suppliers
GET /api/v1/suppliers/{id}
PUT /api/v1/suppliers/{id}
```

### Purchase Orders
```http
GET /api/v1/purchasing/orders
POST /api/v1/purchasing/orders
```

### Goods Receipt
```http
POST /api/v1/purchasing/receipts
```

---

## 10. Production Portal (Factory)

### Production Orders
```http
GET /api/v1/factory/production-orders
POST /api/v1/factory/production-orders
```

### Mixing & Packing
```http
POST /api/v1/factory/mixing
POST /api/v1/factory/packing
```

### Cost Allocation
```http
POST /api/v1/factory/cost-allocation
```

---

## 11. HR Portal

### Employees
```http
GET /api/v1/hr/employees
POST /api/v1/hr/employees
GET /api/v1/hr/employees/{id}
```

### Payroll
```http
POST /api/v1/hr/payroll/process
GET /api/v1/hr/payroll/slips/{employeeId}
```

---

## 12. Common Response Format

All APIs return:
```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "errors": null
}
```

**Error Response:**
```json
{
  "success": false,
  "data": null,
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Email is required"}
  ]
}
```

---

## 13. Pagination

List endpoints support:
```http
GET /api/v1/dealers?page=0&size=20&sort=name,asc
```

**Response includes:**
```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

---

## 14. Error Codes

| HTTP Code | Meaning |
|-----------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/expired token) |
| 403 | Forbidden (no permission) |
| 404 | Not Found |
| 409 | Conflict (duplicate entry) |
| 500 | Server Error |

---

## 15. Quick Start for Frontend Dev

1. **Start backend:** `mvn spring-boot:run "-Dspring.profiles.active=dev"`
2. **Open Swagger:** http://localhost:8081/swagger-ui/index.html
3. **Login** to get token
4. **Use token** in Authorization header for all requests
5. **Set X-Company-Id** header if multi-company

---

## 16. Key Endpoints Summary

| Portal | Endpoint | Method |
|--------|----------|--------|
| Auth | `/api/v1/auth/login` | POST |
| Auth | `/api/v1/auth/refresh` | POST |
| MFA | `/api/v1/mfa/enable` | POST |
| Accounting | `/api/v1/accounting/accounts` | GET, POST |
| Accounting | `/api/v1/accounting/journal-entries` | GET, POST |
| Sales | `/api/v1/dealers` | GET, POST |
| Sales | `/api/v1/sales/orders` | GET, POST |
| Invoices | `/api/v1/invoices` | GET |
| Invoices | `/api/v1/invoices/{id}/pdf` | GET |
| Invoices | `/api/v1/invoices/{id}/email` | POST |
| Inventory | `/api/v1/inventory/finished-goods` | GET, POST |
| Purchasing | `/api/v1/suppliers` | GET, POST |
| Factory | `/api/v1/factory/production-orders` | GET, POST |
| HR | `/api/v1/hr/employees` | GET, POST |

---

## 17. Testing with cURL

```bash
# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"mdanas7869292@gmail.com","password":"Admin@12345"}'

# Get accounts (use token from login response)
curl http://localhost:8081/api/v1/accounting/accounts \
  -H "Authorization: Bearer eyJhbGc..."

# Send invoice email
curl -X POST http://localhost:8081/api/v1/invoices/1/email \
  -H "Authorization: Bearer eyJhbGc..."
```

---

**Full OpenAPI Spec:** See `openapi.yaml` or `openapi.json` in project root.

---

## 18. Deployment Configuration

### Environment Variables for Frontend

Replace hardcoded `localhost:8081` with environment variables:

**For React (Create React App):**
```bash
# .env.development
REACT_APP_API_URL=http://localhost:8081

# .env.production
REACT_APP_API_URL=https://api.bigbrightpaints.com
```

**For Vite:**
```bash
# .env.development
VITE_API_URL=http://localhost:8081

# .env.production
VITE_API_URL=https://api.bigbrightpaints.com
```

**For Next.js:**
```bash
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8081

# .env.production
NEXT_PUBLIC_API_URL=https://api.bigbrightpaints.com
```

### Using the Generated TypeScript Client

```typescript
// Install from clients/typescript-axios
// npm install ../clients/typescript-axios

import { Configuration, AuthControllerApi, SalesControllerApi } from '@bigbright/erp-api-client';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

const config = new Configuration({
  basePath: API_URL,
  accessToken: () => localStorage.getItem('accessToken') || '',
});

export const authApi = new AuthControllerApi(config);
export const salesApi = new SalesControllerApi(config);
```

### Nginx Reverse Proxy (Recommended for Production)

```nginx
server {
    listen 80;
    server_name app.bigbrightpaints.com;

    # Frontend static files
    location / {
        root /var/www/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://backend:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Proxy Swagger UI
    location /swagger-ui/ {
        proxy_pass http://backend:8081/swagger-ui/;
    }
}
```

With this setup, frontend can use relative URLs: `fetch('/api/v1/auth/login')`

### Backend CORS Configuration

For separate domains, update `application-prod.yml`:
```yaml
erp:
  cors:
    allowed-origins: https://app.bigbrightpaints.com,https://admin.bigbrightpaints.com
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  frontend:
    image: bigbright/erp-frontend:latest
    ports:
      - "80:80"
    environment:
      - API_URL=http://backend:8081
    depends_on:
      - backend

  backend:
    image: bigbright/erp-backend:latest
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/erp
      - JWT_SECRET=${JWT_SECRET}
      - ERP_CORS_ALLOWED_ORIGINS=https://app.bigbrightpaints.com
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=erp
      - POSTGRES_USER=erp
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```
