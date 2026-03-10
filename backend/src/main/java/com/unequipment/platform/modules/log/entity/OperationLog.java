package com.unequipment.platform.modules.log.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OperationLog {

    private Long id;
    private Long userId;
    private String moduleName;
    private String actionName;
    private String requestMethod;
    private String requestUri;
    private String requestIp;
    private Integer resultCode;
    private String resultMsg;
    private Long bizId;
    private LocalDateTime createTime;
}
