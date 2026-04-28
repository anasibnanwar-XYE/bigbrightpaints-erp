package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.AssertTrue;

public record OwnerSetupAccountingRequest(
    @AssertTrue(message = "confirmDefaults must be true") Boolean confirmDefaults) {}
