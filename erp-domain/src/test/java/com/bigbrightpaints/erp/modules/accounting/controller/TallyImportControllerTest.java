package com.bigbrightpaints.erp.modules.accounting.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.bigbrightpaints.erp.modules.accounting.dto.TallyImportResponse;
import com.bigbrightpaints.erp.modules.accounting.service.TallyImportService;

@ExtendWith(MockitoExtension.class)
class TallyImportControllerTest {

  @Mock private TallyImportService tallyImportService;

  @Test
  void importTally_delegatesToService() {
    TallyImportController controller = new TallyImportController(tallyImportService);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "tally.xml", "application/xml", "<ENVELOPE/>".getBytes(StandardCharsets.UTF_8));

    TallyImportResponse response =
        new TallyImportResponse(1, 1, 0, 1, 1, List.of(), List.of(), List.of());
    when(tallyImportService.importTallyXml(file)).thenReturn(response);

    controller.importTally(file);

    verify(tallyImportService).importTallyXml(file);
  }
}
