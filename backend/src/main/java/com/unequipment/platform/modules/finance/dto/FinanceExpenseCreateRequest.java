package com.unequipment.platform.modules.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class FinanceExpenseCreateRequest {

    @NotNull(message = "仪器不能为空")
    private Long instrumentId;

    @NotBlank(message = "支出类型不能为空")
    private String expenseType;

    @NotNull(message = "支出金额不能为空")
    @DecimalMin(value = "0.01", message = "支出金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "支出标题不能为空")
    @Size(max = 200, message = "支出标题长度不能超过200个字符")
    private String title;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    @NotNull(message = "支出时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expenseTime;
}
