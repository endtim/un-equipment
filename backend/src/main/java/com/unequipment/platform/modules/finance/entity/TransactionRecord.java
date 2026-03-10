package com.unequipment.platform.modules.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TransactionRecord {

    private Long id;
    private String txnNo;
    private Long userId;
    private Long orderId;
    private Long rechargeId;
    private String txnType;
    private String inoutType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createTime;
}
