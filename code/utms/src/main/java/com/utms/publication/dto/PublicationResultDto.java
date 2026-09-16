package com.utms.publication.dto;

import lombok.Builder;
import lombok.Getter;

/** A4-21 — result of a publish operation. */
@Getter
@Builder
public class PublicationResultDto {
    private final Long draftId;
    private final String status;
    private final Long supersededDraftId;
    private final int affectedFacultyCount;
    private final int affectedBatchCount;
}
