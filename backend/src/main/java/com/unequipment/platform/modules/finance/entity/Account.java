package com.unequipment.platform.modules.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Account {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozenAmount;
    private BigDecimal totalRecharge;
    private BigDecimal totalConsume;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
