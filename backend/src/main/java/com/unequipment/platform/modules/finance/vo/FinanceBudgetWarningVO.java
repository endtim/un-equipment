package com.unequipment.platform.modules.finance.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinanceBudgetWarningVO {

    private Long budgetId;
    private String budgetNo;
    private Integer budgetYear;
    private Long departmentId;
    private String departmentName;
    private Long instrumentId;
    private String instrumentName;
    private BigDecimal budgetAmount;
    private BigDecimal warningRatio;
    private BigDecimal usedAmount;
    private BigDecimal usedRatio;
    private String warningLevel;
}
