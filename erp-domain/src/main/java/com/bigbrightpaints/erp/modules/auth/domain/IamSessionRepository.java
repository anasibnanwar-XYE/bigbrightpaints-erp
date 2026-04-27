package com.bigbrightpaints.erp.modules.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IamSessionRepository extends JpaRepository<IamSession, Long> {}
