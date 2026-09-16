package com.utms.approval.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A4-19 FR-2 — approve at the current level. */
@Getter
@Setter
public class ApproveRequest {

    @Size(max = 2000, message = "comments must not exceed 2000 characters")
    private String comments;
}
