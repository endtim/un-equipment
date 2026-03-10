package com.unequipment.platform.modules.order.dto;

import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderActionRequest {

    @Size(max = 255, message = "comment length must be <= 255")
    private String comment;
}
