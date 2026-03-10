package com.unequipment.platform.modules.stat.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DailySnapshot {

    private Long id;
    private LocalDate statDate;
    private Integer instrumentTotal;
    private Integer instrumentOpenTotal;
    private Integer reservableInstrumentTotal;
    private Integer machineOrderTotal;
    private Integer sampleOrderTotal;
    private Integer completedOrderTotal;
    private Integer effectiveWorkMinutes;
    private Integer externalServiceMinutes;
    private BigDecimal incomeAmount;
    private LocalDateTime createTime;
}
