package com.utms.masterdata.block;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BlockRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;
}
