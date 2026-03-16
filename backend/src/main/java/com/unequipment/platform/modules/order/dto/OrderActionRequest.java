package com.unequipment.platform.modules.order.dto;

import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderActionRequest {

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String comment;
}
