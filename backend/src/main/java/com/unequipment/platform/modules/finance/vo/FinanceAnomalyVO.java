package com.unequipment.platform.modules.finance.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FinanceAnomalyVO {

    private String anomalyType;
    private String anomalyLabel;
    private Long orderId;
    private String orderNo;
    private Long settlementId;
    private String settlementNo;
    private Long userId;
    private String userName;
    private String detail;
    private LocalDateTime createTime;
    private String handleStatus;
    private String handleComment;
    private Long handlerUserId;
    private String handlerUserName;
    private LocalDateTime handleTime;
}
