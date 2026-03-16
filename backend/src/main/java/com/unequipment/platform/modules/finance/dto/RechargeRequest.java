package com.unequipment.platform.modules.finance.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RechargeRequest {

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    private String proofUrl;
    private String remark;
}
