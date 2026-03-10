package com.unequipment.platform.modules.instrument.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MaintenanceRecord {

    private Long id;
    private Long instrumentId;
    private String maintType;
    private String title;
    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long operatorUserId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
