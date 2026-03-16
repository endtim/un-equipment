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

    @NotNull(message = "仪器编号不能为空")
    private Long instrumentId;

    // 兼容字段：支持 "1" 或 "1,2,3,4,5"
    @Pattern(regexp = "^([1-7])(,[1-7])*$", message = "开放星期格式不正确，应为 1-7 或逗号分隔列表")
    private String openDays;

    // 兼容字段：HH:mm-HH:mm
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$", message = "开放时间范围格式应为 HH:mm-HH:mm")
    private String openTimeRange;

    // 结构化字段：单日（兼容旧逻辑）
    @Min(value = 1, message = "星期取值必须在 1 到 7 之间")
    @Max(value = 7, message = "星期取值必须在 1 到 7 之间")
    private Integer weekDay;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式应为 HH:mm")
    private String startTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式应为 HH:mm")
    private String endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveEndDate;

    @Min(value = 1, message = "时间步长必须大于等于 1 分钟")
    private Integer stepMinutes;

    @Min(value = 1, message = "最长预约时长必须大于等于 1 分钟")
    private Integer maxReserveMinutes;

    @Pattern(regexp = "^(ENABLED|DISABLED)?$", message = "规则状态取值不合法")
    private String status;
}