package com.unequipment.platform.modules.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderSummaryVO {

    private Long id;
    private String orderNo;
    private String orderType;
    private String status;
    private String instrumentName;
    private String userName;
    private LocalDateTime reservedStart;
    private LocalDateTime reservedEnd;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdAt;
}
