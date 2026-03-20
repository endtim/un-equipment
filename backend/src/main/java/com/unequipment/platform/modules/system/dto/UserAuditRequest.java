package com.unequipment.platform.modules.system.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserAuditRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String remark;
}

