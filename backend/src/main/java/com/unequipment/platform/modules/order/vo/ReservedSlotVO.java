package com.unequipment.platform.modules.order.vo;

import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
public class ReservedSlotVO {

    private Long orderId;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String statusLabel;
    private String text;
    private LocalDateTime reservedStart;
    private LocalDateTime reservedEnd;
}
