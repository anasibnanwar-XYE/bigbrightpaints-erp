package com.bigbrightpaints.erp.modules.production.dto;

import java.util.UUID;

public record ProductionBrandDto(
    Long id, UUID publicId, String name, String code, long productCount) {}
