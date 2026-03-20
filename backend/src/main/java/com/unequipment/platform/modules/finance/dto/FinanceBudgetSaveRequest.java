package com.unequipment.platform.modules.finance.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class FinanceBudgetSaveRequest {

    private Long id;

    @NotNull(message = "预算年度不能为空")
    @Min(value = 2020, message = "预算年度不合法")
    @Max(value = 2100, message = "预算年度不合法")
    private Integer budgetYear;

    private Long departmentId;
    private Long instrumentId;

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal budgetAmount;

    @NotNull(message = "预警阈值不能为空")
    @DecimalMin(value = "1", message = "预警阈值最小为1")
    private BigDecimal warningRatio;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
