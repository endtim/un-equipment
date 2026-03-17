package com.unequipment.platform.modules.order.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderAmountAdjustRequest {

    @NotNull(message = "结算金额不能为空")
    @DecimalMin(value = "0.00", message = "结算金额必须大于等于0")
    private BigDecimal finalAmount;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String comment;
}
