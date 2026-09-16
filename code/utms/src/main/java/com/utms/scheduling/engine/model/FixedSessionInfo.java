package com.utms.scheduling.engine.model;

import com.utms.scheduling.engine.enums.FixedReason;
import lombok.Builder;
import lombok.Getter;

/**
 * A session that is immovable during (partial) re-generation (A4-14, KD-59).
 * Pre-placed into the solver's occupancy maps so free variables schedule around it
 * (HC-LOCK-4). Parallels {@link CommonSlotInfo}, which is a special case of a fixed
 * placement. Carries the full placement (day/slot/room) plus the entities it occupies
 * (faculty/batch), and a {@link FixedReason} for audit and infeasibility attribution.
 */
@Getter
@Builder
public class FixedSessionInfo {
    private final Long sessionId;
    private final Long facultyId;
    private final Long batchId;
    private final Long roomId;
    private final String dayOfWeek;
    private final Long slotDefinitionId;
    private final FixedReason reason;
}
