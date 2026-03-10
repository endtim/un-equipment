package com.unequipment.platform.modules.instrument.entity;

import com.unequipment.platform.common.model.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Instrument extends BaseEntity {

    private String instrumentNo;
    private String instrumentName;
    private String model;
    private String brand;
    private Long categoryId;
    private Long departmentId;
    private Long ownerUserId;
    private String location;
    private String status;
    private String openMode;
    private Integer openStatus;
    private Integer supportExternal;
    private Integer needAudit;
    private Integer requireTraining;
    private String bookingUnit;
    private BigDecimal priceInternal;
    private BigDecimal priceExternal;
    private Integer minReserveMinutes;
    private Integer maxReserveMinutes;
    private Integer stepMinutes;
    private String coverUrl;
    private String intro;
    private String usageDesc;
    private String sampleDesc;
    private String noticeText;
    private Integer isHot;
    private Integer sortNo;

    private String categoryName;
    private String ownerUserName;
    private String departmentName;
}
