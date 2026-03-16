package com.unequipment.platform.modules.instrument.entity;

import com.unequipment.platform.common.model.BaseEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InstrumentOpenRule extends BaseEntity {

    private Long instrumentId;
    private Integer weekDay;
    private String weekDays;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxReserveMinutes;
    private Integer stepMinutes;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private String status;
}
