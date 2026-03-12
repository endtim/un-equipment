package com.unequipment.platform.modules.finance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RechargeOrder {

    private Long id;
    private String rechargeNo;
    private Long userId;
    private BigDecimal amount;
    private String payMethod;
    private String voucherUrl;
    private String status;
    private String remark;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String userName;
    private String auditUserName;
}
