package com.bigbrightpaints.erp.modules.rbac.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
  Optional<Permission> findByCode(String code);

  List<Permission> findByCodeIn(Collection<String> codes);
}
