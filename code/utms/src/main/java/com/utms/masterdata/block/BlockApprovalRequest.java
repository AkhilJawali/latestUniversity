package com.utms.masterdata.block;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BlockApprovalRequest {

    @Size(max = 1000, message = "Comments must not exceed 1000 characters")
    private String comments;
}
