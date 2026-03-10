package com.unequipment.platform.modules.instrument.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class InstrumentSaveRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name length must be <= 200")
    private String name;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @Size(max = 50, message = "code length must be <= 50")
    private String code;
    @Size(max = 2000, message = "description length must be <= 2000")
    private String description;
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
}
