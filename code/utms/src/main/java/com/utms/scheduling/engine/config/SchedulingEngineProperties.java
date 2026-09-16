package com.utms.scheduling.engine.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the scheduling engine.
 * Bound from application.yml prefix: utms.scheduling.engine
 */
@Component
@ConfigurationProperties(prefix = "utms.scheduling.engine")
@Validated
@Getter
@Setter
public class SchedulingEngineProperties {

    /**
     * Default timeout for generation in seconds.
     * Overridable per-request via GenerationRequest.timeoutDurationSeconds.
     */
    @Min(30)
    @Max(600)
    private int defaultTimeoutSeconds = 120;

    /**
     * Minimum improvement (as percentage of totalSessions) required to persist
     * a best-so-far checkpoint update to the database. Prevents excessive DB writes.
     * E.g., 5 means only write when improvement >= 5% of total sessions.
     */
    @Min(1)
    @Max(50)
    private int progressThresholdPercent = 5;

    /**
     * Grace period in seconds beyond timeout_duration_seconds before the
     * StaleRequestReaper marks a request as orphaned.
     */
    @Min(30)
    @Max(600)
    private int reaperGracePeriodSeconds = 120;
}
