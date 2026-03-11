package com.unequipment.platform.modules.instrument.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class InstrumentSaveRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name length must be <= 200")
    private String name;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @Size(max = 50, message = "code length must be <= 50")
    private String code;
    @Size(max = 50, message = "assetNo length must be <= 50")
    private String assetNo;
    @Size(max = 100, message = "manufacturer length must be <= 100")
    private String manufacturer;
    @Size(max = 100, message = "supplier length must be <= 100")
    private String supplier;
    @Size(max = 50, message = "originCountry length must be <= 50")
    private String originCountry;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;
    @Size(max = 50, message = "equipmentSource length must be <= 50")
    private String equipmentSource;
    @Size(max = 50, message = "serviceContactName length must be <= 50")
    private String serviceContactName;
    @Pattern(regexp = "^[0-9+\\-() ]{0,20}$", message = "serviceContactPhone format is invalid")
    private String serviceContactPhone;
    @Size(max = 2000, message = "description length must be <= 2000")
    private String description;
    @Size(max = 5000, message = "usageDesc length must be <= 5000")
    private String usageDesc;
    @Size(max = 5000, message = "sampleDesc length must be <= 5000")
    private String sampleDesc;
    @Size(max = 5000, message = "noticeText length must be <= 5000")
    private String noticeText;
    @Size(max = 8000, message = "technicalSpecs length must be <= 8000")
    private String technicalSpecs;
    @Size(max = 8000, message = "mainFunctions length must be <= 8000")
    private String mainFunctions;
    @Size(max = 8000, message = "serviceContent length must be <= 8000")
    private String serviceContent;
    @Size(max = 8000, message = "userNotice length must be <= 8000")
    private String userNotice;
    @Size(max = 5000, message = "chargeStandard length must be <= 5000")
    private String chargeStandard;
    @Size(max = 200, message = "location length must be <= 200")
    private String location;
    @Size(max = 255, message = "coverImage length must be <= 255")
    private String coverImage;
    @DecimalMin(value = "0.00", message = "priceInternal must be >= 0")
    private BigDecimal priceInternal;
    @DecimalMin(value = "0.00", message = "priceExternal must be >= 0")
    private BigDecimal priceExternal;
    // compatible old fields, will be removed after frontend fully migrated
    @DecimalMin(value = "0.00", message = "machinePricePerHour must be >= 0")
    private BigDecimal machinePricePerHour;
    @DecimalMin(value = "0.00", message = "samplePricePerItem must be >= 0")
    private BigDecimal samplePricePerItem;
    @Size(max = 20, message = "status length must be <= 20")
    private String status;
    @Pattern(regexp = "^(MACHINE|SAMPLE|BOTH)?$", message = "openMode must be MACHINE/SAMPLE/BOTH")
    private String openMode;
    @Min(value = 0, message = "openStatus must be 0 or 1")
    @Max(value = 1, message = "openStatus must be 0 or 1")
    private Integer openStatus;
    @Min(value = 0, message = "supportExternal must be 0 or 1")
    @Max(value = 1, message = "supportExternal must be 0 or 1")
    private Integer supportExternal;
    @Min(value = 0, message = "needAudit must be 0 or 1")
    @Max(value = 1, message = "needAudit must be 0 or 1")
    private Integer needAudit;
    @Min(value = 0, message = "requireTraining must be 0 or 1")
    @Max(value = 1, message = "requireTraining must be 0 or 1")
    private Integer requireTraining;
    @Pattern(regexp = "^(HOUR|SAMPLE|PROJECT)?$", message = "bookingUnit must be HOUR/SAMPLE/PROJECT")
    private String bookingUnit;
    @Min(value = 1, message = "minReserveMinutes must be >= 1")
    private Integer minReserveMinutes;
    @Min(value = 1, message = "maxReserveMinutes must be >= 1")
    private Integer maxReserveMinutes;
    @Min(value = 1, message = "stepMinutes must be >= 1")
    private Integer stepMinutes;
}
