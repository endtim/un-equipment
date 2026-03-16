package com.unequipment.platform.modules.order.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class MachineReservationRequest {

    @NotNull(message = "仪器编号不能为空")
    private Long instrumentId;

    @NotNull(message = "预约开始时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reservedStart;

    @NotNull(message = "预约结束时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reservedEnd;

    private String projectName;

    private String purpose;

    private String remark;
}
