package com.bigbrightpaints.erp.modules.accounting.dto;

import java.util.List;

public record AuditDigestResponse(String periodLabel, List<String> entries) {}
