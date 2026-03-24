package com.bigbrightpaints.erp.modules.accounting.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bigbrightpaints.erp.modules.accounting.dto.TallyImportResponse;
import com.bigbrightpaints.erp.modules.accounting.service.TallyImportService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/migration")
public class TallyImportController {

  private final TallyImportService tallyImportService;

  public TallyImportController(TallyImportService tallyImportService) {
    this.tallyImportService = tallyImportService;
  }

  @PostMapping(value = "/tally-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<TallyImportResponse>> importTally(
      @RequestPart("file") MultipartFile file) {
    TallyImportResponse response = tallyImportService.importTallyXml(file);
    return ResponseEntity.ok(ApiResponse.success("Tally import processed", response));
  }
}
