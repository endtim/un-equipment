package com.unequipment.platform.modules.order.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditRecord {

    private Long id;
    private Long orderId;
    private Integer nodeNo;
    private Long auditorId;
    private String auditorRole;
    private String auditResult;
    private String auditOpinion;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
}
