package com.unequipment.platform.modules.finance.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SettlementAdminVO {

    private Long id;
    private String settlementNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String userName;
    private Long instrumentId;
    private String instrumentName;
    private Long departmentId;
    private String departmentName;
    private String billType;
    private BigDecimal estimatedAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String settleStatus;
    private LocalDateTime createTime;
    private LocalDateTime settledTime;
    private Long operatorUserId;
    private String operatorName;
    private String orderStatus;
    private String payStatus;
    private String settlementStatus;
}
