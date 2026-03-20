package com.unequipment.platform.modules.system.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    private String remark;
}

