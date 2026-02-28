package com.bigbrightpaints.erp.modules.company.dto;

public record CoATemplateDto(
        String code,
        String name,
        String description,
        Integer accountCount
) {
}
