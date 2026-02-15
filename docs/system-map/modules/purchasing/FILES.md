# PURCHASING Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/controller/PurchasingWorkflowController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/controller/RawMaterialPurchaseController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/controller/SupplierController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/GoodsReceipt.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/GoodsReceiptLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/GoodsReceiptRepository.java | JPA repository for GoodsReceipt persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/PurchaseOrder.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/PurchaseOrderLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/PurchaseOrderRepository.java | JPA repository for PurchaseOrder persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/RawMaterialPurchase.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/RawMaterialPurchaseLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/RawMaterialPurchaseRepository.java | JPA repository for RawMaterialPurchase persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/Supplier.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/SupplierRepository.java | JPA repository for Supplier persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/GoodsReceiptLineRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/GoodsReceiptLineResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/GoodsReceiptRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/GoodsReceiptResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/PurchaseOrderLineRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/PurchaseOrderLineResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/PurchaseOrderRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/PurchaseOrderResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/PurchaseReturnRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/RawMaterialPurchaseLineRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/RawMaterialPurchaseLineResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/RawMaterialPurchaseRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/RawMaterialPurchaseResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/SupplierRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/SupplierResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchasingService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/SupplierService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V27__raw_material_purchasing.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V5__purchasing_hr.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/controller/PurchasingWorkflowControllerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/dto/RawMaterialPurchaseRequestJsonAliasTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchasingServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/service/SupplierServiceTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/controller/PurchasingWorkflowController.java | P2P workflow API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/controller/RawMaterialPurchaseController.java | Raw material invoice workflow API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/controller/SupplierController.java | Supplier administration API

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/PurchasingService.java | Purchase order/GRN/invoice path
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/service/SupplierService.java | Supplier state and access controls
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/PurchaseOrder.java | PO contract and status transitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/RawMaterialPurchase.java | Purchase lifecycle and outstanding control
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/domain/GoodsReceipt.java | Receipt quantity linking to PO/invoice
