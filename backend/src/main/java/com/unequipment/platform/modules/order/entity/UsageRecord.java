package com.unequipment.platform.modules.order.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UsageRecord {

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
