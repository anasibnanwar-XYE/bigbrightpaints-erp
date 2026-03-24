package com.bigbrightpaints.erp.modules.company.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.modules.company.domain.CoATemplate;
import com.bigbrightpaints.erp.modules.company.domain.CoATemplateRepository;
import com.bigbrightpaints.erp.modules.company.dto.CoATemplateDto;

@Service
public class CoATemplateService {

  private final CoATemplateRepository coATemplateRepository;

  public CoATemplateService(CoATemplateRepository coATemplateRepository) {
    this.coATemplateRepository = coATemplateRepository;
  }

  @Transactional(readOnly = true)
  public List<CoATemplateDto> listActiveTemplates() {
    return coATemplateRepository.findByActiveTrueOrderByNameAsc().stream()
        .map(
            template ->
                new CoATemplateDto(
                    template.getCode(),
                    template.getName(),
                    template.getDescription(),
                    template.getAccountCount()))
        .toList();
  }

  @Transactional(readOnly = true)
  public CoATemplate requireActiveTemplate(String code) {
    if (code == null || code.isBlank()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "coaTemplateCode is required");
    }
    return coATemplateRepository
        .findByCodeIgnoreCaseAndActiveTrue(code.trim())
        .orElseThrow(
            () ->
                com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Unsupported CoA template: " + code));
  }
}
