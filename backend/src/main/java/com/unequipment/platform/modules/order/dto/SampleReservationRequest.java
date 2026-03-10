package com.unequipment.platform.modules.order.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SampleReservationRequest {

    @NotNull(message = "instrumentId is required")
    private Long instrumentId;

    @NotBlank(message = "sampleName is required")
    private String sampleName;

    @NotNull(message = "sampleCount is required")
    @Min(value = 1, message = "sampleCount must be greater than 0")
    private Integer sampleCount;

    private String remark;
}
