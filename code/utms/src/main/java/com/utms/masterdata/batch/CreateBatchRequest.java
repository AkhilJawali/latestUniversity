package com.utms.masterdata.batch;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateBatchRequest {

    @NotBlank(message = "Year identifier is required")
    @Size(max = 20, message = "Year identifier must not exceed 20 characters")
    private String yearIdentifier;

    @NotNull(message = "Strength is required")
    @Min(value = 1, message = "Strength must be at least 1")
    private Integer strength;

    @NotNull(message = "Program ID is required")
    private Long programId;

    @Size(max = 200, message = "Elective basket must not exceed 200 characters")
    private String electiveBasket;
}
