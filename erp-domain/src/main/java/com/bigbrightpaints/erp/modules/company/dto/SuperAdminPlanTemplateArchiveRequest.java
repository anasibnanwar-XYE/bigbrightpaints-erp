package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.Size;

public record SuperAdminPlanTemplateArchiveRequest(@Size(max = 300) String reason) {}
