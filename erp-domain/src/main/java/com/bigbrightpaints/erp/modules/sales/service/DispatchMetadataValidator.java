package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmRequest;
import org.springframework.util.StringUtils;

public final class DispatchMetadataValidator {

    private DispatchMetadataValidator() {
    }

    public static void validate(DispatchConfirmRequest request) {
        boolean hasTransportActor = StringUtils.hasText(request.transporterName())
                || StringUtils.hasText(request.driverName());
        if (!hasTransportActor) {
            throw ValidationUtils.invalidInput("Dispatch confirmation requires transporterName or driverName");
        }
        if (!StringUtils.hasText(request.vehicleNumber())) {
            throw ValidationUtils.invalidInput("Dispatch confirmation requires vehicleNumber");
        }
        if (!StringUtils.hasText(request.challanReference())) {
            throw ValidationUtils.invalidInput("Dispatch confirmation requires challanReference");
        }
    }
}
