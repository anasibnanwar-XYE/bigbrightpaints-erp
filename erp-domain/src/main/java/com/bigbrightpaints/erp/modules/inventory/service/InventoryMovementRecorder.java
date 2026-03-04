package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.event.InventoryMovementEvent;
import com.bigbrightpaints.erp.modules.inventory.event.InventoryValuationChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class InventoryMovementRecorder {

    private final InventoryMovementRepository inventoryMovementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CompanyClock companyClock;

    public InventoryMovementRecorder(InventoryMovementRepository inventoryMovementRepository,
                                     ApplicationEventPublisher eventPublisher,
                                     CompanyClock companyClock) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.eventPublisher = eventPublisher;
        this.companyClock = companyClock;
    }

    public InventoryMovement recordFinishedGoodMovement(FinishedGood finishedGood,
                                                        FinishedGoodBatch batch,
                                                        String referenceType,
                                                        String referenceId,
                                                        String movementType,
                                                        BigDecimal quantity,
                                                        BigDecimal unitCost,
                                                        Long packingSlipId) {
        InventoryMovement movement = new InventoryMovement();
        movement.setFinishedGood(finishedGood);
        movement.setFinishedGoodBatch(batch);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setPackingSlipId(packingSlipId);
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        InventoryMovement saved = inventoryMovementRepository.save(movement);
        publishMovementEventIfSupported(finishedGood, saved, referenceType, referenceId, movementType, quantity, unitCost);
        return saved;
    }

    private void publishMovementEventIfSupported(FinishedGood finishedGood,
                                                 InventoryMovement movement,
                                                 String referenceType,
                                                 String referenceId,
                                                 String movementType,
                                                 BigDecimal quantity,
                                                 BigDecimal unitCost) {
        InventoryMovementEvent.MovementType eventType = switch (movementType) {
            case "RECEIPT" -> InventoryMovementEvent.MovementType.RECEIPT;
            case "DISPATCH" -> InventoryMovementEvent.MovementType.ISSUE;
            default -> null;
        };
        if (eventType == null || finishedGood == null || movement == null) {
            return;
        }
        BigDecimal safeQty = quantity != null ? quantity : BigDecimal.ZERO;
        BigDecimal safeCost = unitCost != null ? unitCost : BigDecimal.ZERO;

        String referenceNumber;
        if (StringUtils.hasText(referenceType) && StringUtils.hasText(referenceId)) {
            referenceNumber = referenceType + "-" + referenceId;
        } else if (StringUtils.hasText(referenceId)) {
            referenceNumber = referenceId;
        } else {
            referenceNumber = referenceType;
        }

        Long relatedId = parseLongOrNull(referenceId);
        InventoryMovementEvent event = InventoryMovementEvent.builder()
                .companyId(finishedGood.getCompany() != null ? finishedGood.getCompany().getId() : null)
                .movementType(eventType)
                .inventoryType(InventoryValuationChangedEvent.InventoryType.FINISHED_GOOD)
                .itemId(finishedGood.getId())
                .itemCode(finishedGood.getProductCode())
                .itemName(finishedGood.getName())
                .quantity(safeQty)
                .unitCost(safeCost)
                .totalCost(safeQty.multiply(safeCost))
                .movementId(movement.getId())
                .referenceNumber(referenceNumber)
                .movementDate(companyClock.today(finishedGood.getCompany()))
                .relatedEntityId(relatedId)
                .relatedEntityType(referenceType)
                .build();
        eventPublisher.publishEvent(event);
    }

    private Long parseLongOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
