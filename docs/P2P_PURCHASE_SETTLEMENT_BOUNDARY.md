# P2P Purchase vs Settlement Boundary Contract

This note clarifies why raw-material purchasing does not execute supplier
payment/settlement logic, and which endpoint to call for each stage.

## Endpoint routing matrix

| Flow | Endpoint | Purpose |
|---|---|---|
| Purchase invoice creation | `POST /api/v1/purchasing/raw-material-purchases` | Creates purchase invoice from supplier + goods receipt, posts purchase journal (inventory/payable), sets purchase outstanding. |
| Supplier payment | `POST /api/v1/accounting/suppliers/payments` | Records a payment journal; requires allocation rows tied to purchases. |
| Supplier settlement | `POST /api/v1/accounting/settlements/suppliers` | Records settlement allocations with optional discount/write-off/FX treatment and on-account behavior. |

## Invariants enforced

1. Purchase creation is isolated to purchasing module routing.
2. Purchase creation posts via `AccountingFacade.postPurchaseJournal(...)` only.
3. Purchase creation does not call `recordSupplierPayment(...)` or `settleSupplierInvoices(...)`.
4. Settlement/payment mutations of purchase outstanding happen only under
   `/api/v1/accounting/...` endpoints.
5. Supplier settlement request contract does not include purchase invoice fields
   (`invoiceNumber`, `goodsReceiptId`).

## Frontend payload compatibility for purchase creation

`RawMaterialPurchaseRequest` accepts canonical fields and these aliases:

- `invoiceNumber`: aliases `invoiceNo`, `invoice_no`
- `goodsReceiptId`: aliases `goodsReceiptID`, `goods_receipt_id`, `goodsReceipt`, `grnId`

If the frontend sends settlement payloads to purchase endpoint (or vice versa),
the request fails validation for missing required contract fields.

