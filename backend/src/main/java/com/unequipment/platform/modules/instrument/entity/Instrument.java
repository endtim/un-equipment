package com.unequipment.platform.modules.instrument.entity;

import com.unequipment.platform.common.model.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Instrument extends BaseEntity {

    private String instrumentNo;
    private String instrumentName;
    private String model;
    private String brand;
    private String assetNo;
    private String manufacturer;
    private String supplier;
    private String originCountry;
    private LocalDate purchaseDate;
    private LocalDate productionDate;
    private String equipmentSource;
    private String serviceContactName;
    private String serviceContactPhone;
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
    private String technicalSpecs;
    private String mainFunctions;
    private String serviceContent;
    private String userNotice;
    private String chargeStandard;
    private Integer isHot;
    private Integer sortNo;

    private String categoryName;
    private String ownerUserName;
    private String departmentName;
}
