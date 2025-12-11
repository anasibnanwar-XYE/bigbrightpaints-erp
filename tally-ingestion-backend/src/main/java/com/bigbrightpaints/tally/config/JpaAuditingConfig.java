package com.bigbrightpaints.tally.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enable JPA Auditing for automatic created_at and updated_at timestamps
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
