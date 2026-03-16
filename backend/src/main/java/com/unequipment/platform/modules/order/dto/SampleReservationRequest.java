package com.unequipment.platform.modules.order.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SampleReservationRequest {

    @NotNull(message = "仪器编号不能为空")
    private Long instrumentId;

    @NotBlank(message = "样品名称不能为空")
    private String sampleName;

    @NotNull(message = "样品数量不能为空")
    @Min(value = 1, message = "样品数量必须大于0")
    private Integer sampleCount;

    private String projectName;

    private String purpose;

    private String remark;
}
