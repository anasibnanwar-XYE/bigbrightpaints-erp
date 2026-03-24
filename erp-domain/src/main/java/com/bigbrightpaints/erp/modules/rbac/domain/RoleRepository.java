package com.bigbrightpaints.erp.modules.rbac.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(String name);

  List<Role> findByNameIn(Collection<String> names);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Role r where r.name = :name")
  Optional<Role> lockByName(@Param("name") String name);
}
