package com.utms.approval.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A4-19 FR-1 — submit a draft into the approval workflow. */
@Getter
@Setter
public class SubmitDraftRequest {

    @NotNull(message = "draftId is required")
    private Long draftId;

    @Size(max = 2000, message = "comments must not exceed 2000 characters")
    private String comments;
}
