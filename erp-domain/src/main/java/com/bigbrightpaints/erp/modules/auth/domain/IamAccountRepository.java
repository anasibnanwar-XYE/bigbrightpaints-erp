package com.bigbrightpaints.erp.modules.auth.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IamAccountRepository extends JpaRepository<IamAccount, Long> {

  Optional<IamAccount> findByPublicId(UUID publicId);

  Optional<IamAccount> findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
      String email, String authScopeCode);
}
