package com.unequipment.platform.modules.order.entity;

import com.unequipment.platform.common.model.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationOrder extends BaseEntity {

    private String orderNo;
    private String orderType;
    private Long userId;
    private Long instrumentId;
    private Long departmentId;
    private Long ownerUserId;
    private String contactName;
    private String contactPhone;
    private String purpose;
    private String projectName;
    private LocalDateTime reserveStart;
    private LocalDateTime reserveEnd;
    private Integer reserveMinutes;
    private String orderStatus;
    private String auditStatus;
    private String payStatus;
    private String settlementStatus;
    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;
    private String source;
    private LocalDateTime submitTime;
    private LocalDateTime approveTime;
    private LocalDateTime finishTime;
    private String cancelReason;
    private String remark;

    private String userName;
    private String instrumentName;
}
