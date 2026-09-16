package com.utms.masterdata.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CreateAssetRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Identifier is required")
    @Size(max = 50, message = "Identifier must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Identifier must be alphanumeric (hyphens and underscores allowed)")
    private String identifier;

    @NotBlank(message = "Asset type is required")
    @Size(max = 50, message = "Asset type must not exceed 50 characters")
    private String assetType;

    @NotNull(message = "Owning department ID is required")
    private Long owningDepartmentId;

    @NotNull(message = "Campus ID is required")
    private Long campusId;

    @Valid
    private List<CreateAvailabilityWindowRequest> availabilityWindows;
}
