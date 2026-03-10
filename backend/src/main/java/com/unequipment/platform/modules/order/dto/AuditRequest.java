package com.unequipment.platform.modules.order.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditRequest {

    @NotBlank(message = "action is required")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "action must be APPROVE or REJECT")
    private String action;

    @Size(max = 255, message = "comment length must be <= 255")
    private String comment;
}
