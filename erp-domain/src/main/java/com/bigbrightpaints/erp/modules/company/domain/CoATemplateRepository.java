package com.bigbrightpaints.erp.modules.company.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoATemplateRepository extends JpaRepository<CoATemplate, Long> {

    List<CoATemplate> findByActiveTrueOrderByNameAsc();

    Optional<CoATemplate> findByCodeIgnoreCaseAndActiveTrue(String code);
}
