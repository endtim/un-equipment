package com.unequipment.platform.modules.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FinanceBudget {

    private Long id;
    private String budgetNo;
    private Integer budgetYear;
    private Long departmentId;
    private Long instrumentId;
    private BigDecimal budgetAmount;
    private BigDecimal warningRatio;
    private String status;
    private String remark;
    private Long operatorUserId;
    private String departmentName;
    private String instrumentName;
    private String operatorUserName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
