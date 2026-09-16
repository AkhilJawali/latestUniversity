package com.utms.masterdata.batch;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateBatchRequest {

    @Min(value = 1, message = "Strength must be at least 1")
    private Integer strength;

    @Size(max = 200, message = "Elective basket must not exceed 200 characters")
    private String electiveBasket;
}
