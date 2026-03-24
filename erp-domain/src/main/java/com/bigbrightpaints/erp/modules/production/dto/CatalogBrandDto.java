package com.bigbrightpaints.erp.modules.production.dto;

import java.util.UUID;

public record CatalogBrandDto(
    Long id,
    UUID publicId,
    String name,
    String code,
    String logoUrl,
    String description,
    boolean active) {}
