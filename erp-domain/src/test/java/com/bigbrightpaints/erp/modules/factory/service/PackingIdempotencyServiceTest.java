package com.bigbrightpaints.erp.modules.factory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigbrightpaints.erp.modules.factory.domain.PackingRequestRecordRepository;
import com.bigbrightpaints.erp.modules.factory.dto.PackingLineRequest;
import com.bigbrightpaints.erp.modules.factory.dto.PackingRequest;

@Tag("critical")
@ExtendWith(MockitoExtension.class)
class PackingIdempotencyServiceTest {

  @Mock private PackingRequestRecordRepository packingRequestRecordRepository;
  @Mock private ProductionLogService productionLogService;

  @Test
  void packingRequestHash_changesWhenResidualCloseFlagChanges() {
    PackingIdempotencyService service =
        new PackingIdempotencyService(packingRequestRecordRepository, productionLogService);

    PackingRequest standardRequest =
        new PackingRequest(
            1L,
            LocalDate.of(2024, 1, 1),
            "packer",
            "pack-1",
            List.of(new PackingLineRequest(11L, 2, "1L", null, 12, 1, 12)),
            Boolean.FALSE);
    PackingRequest residualCloseRequest =
        new PackingRequest(
            1L,
            LocalDate.of(2024, 1, 1),
            "packer",
            "pack-1",
            List.of(new PackingLineRequest(11L, 2, "1L", null, 12, 1, 12)),
            Boolean.TRUE);

    String standardHash = service.packingRequestHash(standardRequest, LocalDate.of(2024, 1, 1));
    String residualCloseHash =
        service.packingRequestHash(residualCloseRequest, LocalDate.of(2024, 1, 1));

    assertThat(standardHash).isNotBlank();
    assertThat(residualCloseHash).isNotBlank();
    assertThat(residualCloseHash).isNotEqualTo(standardHash);
  }
}
