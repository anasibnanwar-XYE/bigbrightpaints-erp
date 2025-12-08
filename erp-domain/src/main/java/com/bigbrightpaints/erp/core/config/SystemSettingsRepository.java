package com.bigbrightpaints.erp.core.config;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSetting, String> {
}
