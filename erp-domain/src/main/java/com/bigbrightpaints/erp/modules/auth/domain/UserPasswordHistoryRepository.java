package com.bigbrightpaints.erp.modules.auth.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {

  List<UserPasswordHistory> findTop5ByUserOrderByChangedAtDesc(UserAccount user);

  List<UserPasswordHistory> findByUserOrderByChangedAtDesc(UserAccount user);
}
