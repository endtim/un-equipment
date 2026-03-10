package com.unequipment.platform.modules.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SettlementRecord {

    private Long id;
    private String settlementNo;
    private Long orderId;
    private Long userId;
    private Long instrumentId;
    private String billType;
    private String priceDesc;
    private BigDecimal estimatedAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String settleStatus;
    private LocalDateTime settledTime;
    private Long operatorUserId;
    private LocalDateTime createTime;
}
