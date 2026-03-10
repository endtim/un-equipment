package com.unequipment.platform.modules.instrument.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InstrumentClosePeriod {

    private Long id;
    private Long instrumentId;
    private LocalDateTime closeStart;
    private LocalDateTime closeEnd;
    private String closeType;
    private String reason;
    private LocalDateTime createTime;
}
