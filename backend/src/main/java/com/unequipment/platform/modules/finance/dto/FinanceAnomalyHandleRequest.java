package com.unequipment.platform.modules.finance.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FinanceAnomalyHandleRequest {

    @NotBlank(message = "异常类型不能为空")
    private String anomalyType;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private Long settlementId;

    @NotBlank(message = "处理状态不能为空")
    @Pattern(regexp = "^(PENDING|PROCESSING|RESOLVED)$", message = "处理状态不合法")
    private String handleStatus;

    private String handleComment;
}

