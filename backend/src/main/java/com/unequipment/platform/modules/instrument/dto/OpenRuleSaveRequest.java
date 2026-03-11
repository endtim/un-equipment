package com.unequipment.platform.modules.instrument.dto;

import java.time.LocalDate;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class OpenRuleSaveRequest {

    @NotNull(message = "instrumentId is required")
    private Long instrumentId;

    // legacy field: 1~7 (Mon~Sun)
    @Pattern(regexp = "^[1-7]$", message = "openDays must be between 1 and 7")
    private String openDays;

    // legacy field: HH:mm-HH:mm
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$", message = "openTimeRange must be HH:mm-HH:mm")
    private String openTimeRange;

    // structured fields
    @Min(value = 1, message = "weekDay must be between 1 and 7")
    @Max(value = 7, message = "weekDay must be between 1 and 7")
    private Integer weekDay;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "startTime must be HH:mm")
    private String startTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "endTime must be HH:mm")
    private String endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveEndDate;

    @Min(value = 1, message = "stepMinutes must be >= 1")
    private Integer stepMinutes;

    @Min(value = 1, message = "maxReserveMinutes must be >= 1")
    private Integer maxReserveMinutes;
}
