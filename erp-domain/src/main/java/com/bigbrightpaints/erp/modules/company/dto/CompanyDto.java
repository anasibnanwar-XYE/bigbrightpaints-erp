package com.bigbrightpaints.erp.modules.company.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanyDto(Long id,
                         UUID publicId,
                         String name,
                         String code,
                         String timezone,
                         String stateCode,
                         BigDecimal defaultGstRate) {

    public CompanyDto(Long id,
                      UUID publicId,
                      String name,
                      String code,
                      String timezone,
                      BigDecimal defaultGstRate) {
        this(id, publicId, name, code, timezone, null, defaultGstRate);
    }
}
