package com.unequipment.platform.modules.finance.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FinanceDetailVO {

    private String bizType;
    private String bizTypeLabel;
    private String bizNo;
    private String orderNo;
    private Long instrumentId;
    private String instrumentName;
    private Long departmentId;
    private String departmentName;
    private Long userId;
    private String userName;
    private BigDecimal amount;
    private String inoutType;
    private String inoutTypeLabel;
    private String remark;
    private LocalDateTime occurTime;
}
