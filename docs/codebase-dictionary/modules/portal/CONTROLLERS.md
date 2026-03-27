# Portal Controllers

## Overview

| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| PortalInsightsController | 3 | Portal insights |

---

## PortalInsightsController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/controller/PortalInsightsController.java`

**Package**: `com.bigbrightpaints.erp.modules.portal.controller`

**Base Path**: `/api/v1/portal`

**Dependencies**:
- PortalInsightsService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/dashboard` | `ResponseEntity<ApiResponse<DashboardInsights>> dashboard()` | Dashboard insights |
| GET | `/operations` | `ResponseEntity<ApiResponse<OperationsInsights>> operations()` | Operations insights |
| GET | `/workforce` | `ResponseEntity<ApiResponse<WorkforceInsights>> workforce()` | Workforce insights (HR module required) |
