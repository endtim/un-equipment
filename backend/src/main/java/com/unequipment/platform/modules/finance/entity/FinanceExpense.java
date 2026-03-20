package com.unequipment.platform.modules.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FinanceExpense {

    private Long id;
    private String expenseNo;
    private Long instrumentId;
    private Long departmentId;
    private String expenseType;
    private BigDecimal amount;
    private String title;
    private String remark;
    private LocalDateTime expenseTime;
    private Long operatorUserId;
    private String instrumentName;
    private String departmentName;
    private String operatorUserName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
