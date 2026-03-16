package com.unequipment.platform.modules.order.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditRequest {

    @NotBlank(message = "操作不能为空")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "操作值不合法")
    private String action;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String comment;
}
