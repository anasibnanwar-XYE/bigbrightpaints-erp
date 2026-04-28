package com.bigbrightpaints.erp.modules.company.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantActivationTokenRepository
    extends JpaRepository<TenantActivationToken, Long> {

  Optional<TenantActivationToken> findByTokenDigest(String tokenDigest);
}
