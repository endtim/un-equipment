package com.unequipment.platform.modules.system.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserAuditLog {
    private Long id;
    private Long userId;
    private String actionType;
    private String actionResult;
    private Long operatorUserId;
    private String operatorRole;
    private String remark;
    private LocalDateTime createTime;

    private String username;
    private String realName;
    private String operatorName;
}

