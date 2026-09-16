package com.utms.masterdata.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UpdateAssetRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Asset type is required")
    @Size(max = 50, message = "Asset type must not exceed 50 characters")
    private String assetType;

    @Valid
    private List<CreateAvailabilityWindowRequest> availabilityWindows;
}
