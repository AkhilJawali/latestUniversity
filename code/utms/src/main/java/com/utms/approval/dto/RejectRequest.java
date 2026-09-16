package com.utms.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A4-19 FR-3 — reject at the current level. Reason is mandatory (HC-AW-2). */
@Getter
@Setter
public class RejectRequest {

    @NotBlank(message = "rejectionReason is required")
    @Size(max = 500, message = "rejectionReason must not exceed 500 characters")
    private String rejectionReason;

    @Size(max = 2000, message = "comments must not exceed 2000 characters")
    private String comments;
}
