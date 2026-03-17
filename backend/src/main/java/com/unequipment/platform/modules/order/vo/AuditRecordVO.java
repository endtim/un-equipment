package com.unequipment.platform.modules.order.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditRecordVO {

    private Long id;
    private Integer nodeNo;
    private Long auditorId;
    private String auditorRole;
    private String auditResult;
    private String auditOpinion;
    private LocalDateTime auditTime;
}
