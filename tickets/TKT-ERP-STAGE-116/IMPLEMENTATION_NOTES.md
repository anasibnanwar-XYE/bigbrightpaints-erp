# Implementation Notes

## API Contracts

1. `GET /api/v1/sales/dashboard`
   - auth: `ROLE_ADMIN|ROLE_SALES|ROLE_FACTORY|ROLE_ACCOUNTING`
   - payload:
     - `activeDealers`
     - `totalOrders`
     - `orderStatusBuckets` (`open`, `in_progress`, `dispatched`, `completed`, `cancelled`, `other`)
     - `pendingCreditRequests`

2. `POST /api/v1/sales/dispatch/confirm`
   - auth tightened to: `ROLE_FACTORY|ROLE_ADMIN` and `dispatch.confirm`
   - sales users can still observe order/dispatch progress but cannot perform final dispatch.

3. `POST /api/v1/sales/credit-requests/{id}/approve`
   - approval now:
     - requires pending status
     - requires linked dealer
     - pessimistically locks dealer row
     - increments dealer credit limit by requested amount
     - emits audit metadata (`oldLimit`, `newLimit`, `increment`)

## Database

- Flyway v2 migration added:
  - `V22__promotions_image_url.sql`
  - DDL: `promotions.image_url varchar(1024)` (nullable, backward compatible)
- Domain mapping aligned:
  - `Promotion.imageUrl`
  - `PromotionRequest.imageUrl`
  - `PromotionDto.imageUrl`

## Regression Coverage

- Unit tests:
  - dashboard bucket aggregation and counts
  - credit approval mutation + fail-closed paths
  - promotion image map in create/update paths
- Integration tests:
  - sales dashboard activity metrics
  - dispatch role boundary enforcement
  - credit approval increases persisted dealer limit
