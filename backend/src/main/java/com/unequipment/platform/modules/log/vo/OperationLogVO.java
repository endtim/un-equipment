package com.unequipment.platform.modules.log.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OperationLogVO {

    private Long id;
    private Long userId;
    private String operatorName;
    private String moduleName;
    private String moduleLabel;
    private String actionName;
    private String actionLabel;
    private String requestMethod;
    private String requestUri;
    private String requestIp;
    private Integer resultCode;
    private String resultMsg;
    private String resultLabel;
    private Long bizId;
    private LocalDateTime createTime;
}
