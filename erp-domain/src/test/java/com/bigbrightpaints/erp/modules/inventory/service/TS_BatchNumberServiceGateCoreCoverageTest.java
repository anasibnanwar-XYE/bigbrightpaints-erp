package com.bigbrightpaints.erp.modules.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.service.NumberSequenceService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;

@Tag("reconciliation")
class TS_BatchNumberServiceGateCoreCoverageTest {

  @Test
  void rawMaterialCodesUseCompanyTimezoneSkuFallbackAndPreviewSequence() {
    NumberSequenceService sequenceService = mock(NumberSequenceService.class);
    BatchNumberService service = new BatchNumberService(sequenceService);
    Company company = company("BBP", "UTC");
    RawMaterial material = new RawMaterial();
    material.setCompany(company);
    material.setSku("rm- 01");
    when(sequenceService.nextValue(eq(company), eq(rawMaterialKey("01")))).thenReturn(4L);
    when(sequenceService.previewNextValue(eq(company), eq(rawMaterialKey("01")))).thenReturn(5L);

    assertThat(service.nextRawMaterialBatchCode(material)).isEqualTo(rawMaterialKey("01") + "-004");
    long preview = service.previewRawMaterialBatchSequence(material);
    assertThat(service.previewRawMaterialBatchCodeAt(material, preview + 2))
        .isEqualTo(rawMaterialKey("01") + "-007");

    RawMaterial missingSku = new RawMaterial();
    missingSku.setCompany(company);
    missingSku.setSku(" ");
    when(sequenceService.nextValue(eq(company), eq(rawMaterialKey("ITEM")))).thenReturn(1L);
    assertThat(service.nextRawMaterialBatchCode(missingSku))
        .isEqualTo(rawMaterialKey("ITEM") + "-001");

    verify(sequenceService).previewNextValue(eq(company), eq(rawMaterialKey("01")));
  }

  @Test
  void finishedGoodCodesUsePackedDateCurrentMonthFallbackAndPackagingSlipSequence() {
    NumberSequenceService sequenceService = mock(NumberSequenceService.class);
    BatchNumberService service = new BatchNumberService(sequenceService);
    Company company = company("ACME", "UTC");
    FinishedGood finishedGood = new FinishedGood();
    finishedGood.setCompany(company);
    finishedGood.setProductCode("fg- 01");
    String packedKey = "ACME-FG-01-202501";
    when(sequenceService.nextValue(eq(company), eq(packedKey))).thenReturn(7L);
    when(sequenceService.previewNextValue(eq(company), eq(packedKey))).thenReturn(8L);

    assertThat(service.nextFinishedGoodBatchCode(finishedGood, LocalDate.of(2025, 1, 15)))
        .isEqualTo(packedKey + "-007");
    long preview =
        service.previewFinishedGoodBatchSequence(finishedGood, LocalDate.of(2025, 1, 15));
    assertThat(
            service.previewFinishedGoodBatchCodeAt(
                finishedGood, LocalDate.of(2025, 1, 15), preview + 2))
        .isEqualTo(packedKey + "-010");

    String currentKey = "ACME-FG-01-" + YearMonth.now(ZoneId.of("UTC")).toString().replace("-", "");
    when(sequenceService.nextValue(eq(company), eq(currentKey))).thenReturn(3L);
    assertThat(service.nextFinishedGoodBatchCode(finishedGood, null))
        .isEqualTo(currentKey + "-003");

    when(sequenceService.nextValue(eq(company), eq("ACME-PS"))).thenReturn(12L);
    assertThat(service.nextPackagingSlipNumber(company)).isEqualTo("ACME-PS-012");
  }

  private static Company company(String code, String timezone) {
    Company company = new Company();
    company.setCode(code);
    company.setTimezone(timezone);
    return company;
  }

  private static String rawMaterialKey(String sku) {
    return "RM-" + sku + "-" + YearMonth.now(ZoneId.of("UTC")).toString().replace("-", "");
  }
}
