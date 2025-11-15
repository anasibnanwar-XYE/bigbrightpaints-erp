package com.bigbrightpaints.erp.modules.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {

    List<UserPasswordHistory> findTop5ByUserOrderByChangedAtDesc(UserAccount user);

    List<UserPasswordHistory> findByUserOrderByChangedAtDesc(UserAccount user);
}

