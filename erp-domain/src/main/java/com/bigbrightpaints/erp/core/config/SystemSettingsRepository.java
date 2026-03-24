package com.bigbrightpaints.erp.core.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SystemSettingsRepository extends JpaRepository<SystemSetting, String> {

  @Modifying
  @Transactional
  @Query(
      value =
          """
          INSERT INTO system_settings (setting_key, setting_value)
          VALUES (:key, '1')
          ON CONFLICT (setting_key)
          DO UPDATE SET setting_value = (
              CASE
                  WHEN trim(COALESCE(system_settings.setting_value, '')) ~ '^[0-9]+$'
                      THEN (trim(system_settings.setting_value))::bigint + 1
                  ELSE 1
              END
          )::text
          """,
      nativeQuery = true)
  void incrementLongSetting(@Param("key") String key);
}
