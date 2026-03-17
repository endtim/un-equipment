package com.unequipment.platform.modules.finance.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FinanceAnomalyHandle {

    private Long id;
    private String anomalyType;
    private Long orderId;
    private Long settlementId;
    private String handleStatus;
    private String handleComment;
    private Long handlerUserId;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    private String handlerUserName;
}

