package com.utms.scheduling.engine.entity;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A session that could not be placed during generation (FR-2.5).
 * Persisted on timeout or infeasibility so the coordinator can see what remains.
 * Stores denormalized context (course/faculty/batch names) for immediate readability
 * without requiring joins at query time.
 */
@Entity
@Table(name = "unplaced_sessions")
@Getter
@Setter
public class UnplacedSession extends BaseEntity {

    @Column(name = "draft_id", nullable = false)
    private Long draftId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "faculty_name", nullable = false, length = 200)
    private String facultyName;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "batch_name", nullable = false, length = 100)
    private String batchName;

    @Column(name = "session_type", nullable = false, length = 20)
    private String sessionType;

    @Column(name = "required_duration_minutes", nullable = false)
    private Integer requiredDurationMinutes;

    @Column(name = "reason", length = 500)
    private String reason;
}
