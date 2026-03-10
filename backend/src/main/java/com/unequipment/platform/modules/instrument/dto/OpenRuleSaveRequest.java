package com.unequipment.platform.modules.instrument.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class OpenRuleSaveRequest {

    @NotNull(message = "instrumentId is required")
    private Long instrumentId;

    @NotBlank(message = "openDays is required")
    @Pattern(regexp = "^[1-7]$", message = "openDays must be between 1 and 7")
    private String openDays;

    @NotBlank(message = "openTimeRange is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$", message = "openTimeRange must be HH:mm-HH:mm")
    private String openTimeRange;

    @Size(max = 50, message = "targetScope length must be <= 50")
    private String targetScope;
    @Size(max = 255, message = "ruleRemark length must be <= 255")
    private String ruleRemark;
}
