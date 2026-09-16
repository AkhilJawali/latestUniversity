package com.utms.scheduling.engine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request body for partial re-generation (A4-14, FR-3). The scope selects which
 * subset of the source draft to regenerate (batch / section / course id-sets).
 */
@Getter
@Setter
public class RegenerateRequest {

    @NotNull
    @Valid
    private ScopeDto scope;

    /** Optional deterministic seed; null lets the engine choose (PD-69). */
    private Long seed;

    @Getter
    @Setter
    public static class ScopeDto {
        private List<Long> batchIds = List.of();
        private List<Long> sectionIds = List.of();
        private List<Long> courseIds = List.of();
    }
}
