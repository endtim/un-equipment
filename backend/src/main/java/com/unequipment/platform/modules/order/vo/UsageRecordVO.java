package com.unequipment.platform.modules.order.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UsageRecordVO {

    private Long id;
    private Long orderId;
    private Long instrumentId;
    private Long operatorUserId;
    private LocalDateTime checkinTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer actualMinutes;
    private Integer abnormalFlag;
    private String abnormalDesc;
    private Long ownerConfirmUserId;
    private LocalDateTime ownerConfirmTime;
}
