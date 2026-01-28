package com.bigbrightpaints.erp.core.health;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Exposes configuration validation as an Actuator health indicator.
 * <p>
 * The underlying checks can be DB-intensive, so results are cached for a short period.
 */
@Component
@ConditionalOnProperty(prefix = "erp.environment.validation", name = "health-indicator.enabled", havingValue = "true")
public class ConfigurationHealthIndicator implements HealthIndicator {

    private final ConfigurationHealthService configurationHealthService;
    private final Duration cacheTtl;

    private volatile Instant cachedAt;
    private volatile ConfigurationHealthService.ConfigurationHealthReport cachedReport;

    public ConfigurationHealthIndicator(ConfigurationHealthService configurationHealthService,
                                        @Value("${erp.environment.validation.health-cache-seconds:60}") long cacheSeconds) {
        this.configurationHealthService = configurationHealthService;
        this.cacheTtl = Duration.ofSeconds(Math.max(5, cacheSeconds));
    }

    @Override
    public Health health() {
        try {
            ConfigurationHealthService.ConfigurationHealthReport report = getReport();
            Health.Builder builder = report.healthy() ? Health.up() : Health.down();
            builder.withDetail("issuesCount", report.issues().size());
            if (!report.healthy()) {
                builder.withDetail("issuesSample", report.issues().stream().limit(25).toList());
            }
            if (cachedAt != null) {
                builder.withDetail("cachedAt", cachedAt.toString());
                builder.withDetail("cacheTtlSeconds", cacheTtl.toSeconds());
            }
            return builder.build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }

    private ConfigurationHealthService.ConfigurationHealthReport getReport() {
        Instant now = CompanyTime.now();
        Instant last = cachedAt;
        ConfigurationHealthService.ConfigurationHealthReport report = cachedReport;
        if (last != null && report != null && Duration.between(last, now).compareTo(cacheTtl) < 0) {
            return report;
        }
        ConfigurationHealthService.ConfigurationHealthReport fresh = configurationHealthService.evaluate();
        cachedReport = fresh;
        cachedAt = now;
        return fresh;
    }
}
