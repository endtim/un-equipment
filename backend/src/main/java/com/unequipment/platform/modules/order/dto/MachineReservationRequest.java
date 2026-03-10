package com.unequipment.platform.modules.order.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class MachineReservationRequest {

    @NotNull(message = "instrumentId is required")
    private Long instrumentId;

    @NotNull(message = "reservedStart is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reservedStart;

    @NotNull(message = "reservedEnd is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reservedEnd;

    private String remark;
}
