package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scheduled_sessions")
@Getter
@Setter
public class ScheduledSession extends BaseEntity {

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "slot_definition_id", nullable = false)
    private Long slotDefinitionId;

    @Column(name = "session_type", nullable = false, length = 20)
    private String sessionType;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    /**
     * Recurrence pattern (A4-13, KD-59/KD-60). Defaults to WEEKLY so existing
     * rows and engine output behave exactly as before (backward compatibility).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType = RecurrenceType.WEEKLY;

    /**
     * The alternating-week group this session occupies (A4-13). Non-null only
     * when {@link #recurrenceType} is FORTNIGHTLY; null for WEEKLY (HC-FN-4).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "week_group", length = 10)
    private WeekGroup weekGroup;

    /**
     * Approval flag (A4-14, KD-60). Set by the approval workflow (A4-18) later;
     * read by the scheduling engine now, which treats approved sessions as
     * immovable/fixed during (partial) re-generation (HC-LOCK-2).
     */
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;
}
