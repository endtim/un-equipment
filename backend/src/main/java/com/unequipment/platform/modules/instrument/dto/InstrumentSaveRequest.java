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

    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称长度不能超过200个字符")
    private String name;

    @NotNull(message = "分类编号不能为空")
    private Long categoryId;

    @Size(max = 50, message = "编码长度不能超过50个字符")
    private String code;
    @Size(max = 100, message = "型号长度不能超过100个字符")
    private String model;
    @Size(max = 100, message = "品牌长度不能超过100个字符")
    private String brand;
    @Size(max = 50, message = "资产编号长度不能超过50个字符")
    private String assetNo;
    @Size(max = 100, message = "生产厂家长度不能超过100个字符")
    private String manufacturer;
    @Size(max = 100, message = "供应商长度不能超过100个字符")
    private String supplier;
    @Size(max = 50, message = "产地长度不能超过50个字符")
    private String originCountry;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;
    @Size(max = 50, message = "设备来源长度不能超过50个字符")
    private String equipmentSource;
    @Size(max = 50, message = "服务联系人长度不能超过50个字符")
    private String serviceContactName;
    @Pattern(regexp = "^[0-9+\\-() ]{0,20}$", message = "服务联系电话格式不合法")
    private String serviceContactPhone;
    @Size(max = 2000, message = "简介长度不能超过2000个字符")
    private String description;
    @Size(max = 5000, message = "使用说明长度不能超过5000个字符")
    private String usageDesc;
    @Size(max = 5000, message = "送样说明长度不能超过5000个字符")
    private String sampleDesc;
    @Size(max = 5000, message = "特别说明长度不能超过5000个字符")
    private String noticeText;
    @Size(max = 8000, message = "技术指标长度不能超过8000个字符")
    private String technicalSpecs;
    @Size(max = 8000, message = "主要功能长度不能超过8000个字符")
    private String mainFunctions;
    @Size(max = 8000, message = "服务内容长度不能超过8000个字符")
    private String serviceContent;
    @Size(max = 8000, message = "用户须知长度不能超过8000个字符")
    private String userNotice;
    @Size(max = 5000, message = "收费标准说明长度不能超过5000个字符")
    private String chargeStandard;
    @Size(max = 200, message = "放置地点长度不能超过200个字符")
    private String location;
    @Size(max = 255, message = "封面地址长度不能超过255个字符")
    private String coverImage;
    @DecimalMin(value = "0.00", message = "校内价格必须大于等于0")
    private BigDecimal priceInternal;
    @DecimalMin(value = "0.00", message = "校外价格必须大于等于0")
    private BigDecimal priceExternal;
    // compatible old fields, will be removed after frontend fully migrated
    @DecimalMin(value = "0.00", message = "上机价格必须大于等于0")
    private BigDecimal machinePricePerHour;
    @DecimalMin(value = "0.00", message = "送样价格必须大于等于0")
    private BigDecimal samplePricePerItem;
    @Size(max = 20, message = "状态长度不能超过20个字符")
    private String status;
    @Pattern(regexp = "^(MACHINE|SAMPLE|BOTH)?$", message = "开放模式取值不合法")
    private String openMode;
    @Min(value = 0, message = "开放状态必须为0或1")
    @Max(value = 1, message = "开放状态必须为0或1")
    private Integer openStatus;
    @Min(value = 0, message = "是否支持校外必须为0或1")
    @Max(value = 1, message = "是否支持校外必须为0或1")
    private Integer supportExternal;
    @Min(value = 0, message = "是否需要审核必须为0或1")
    @Max(value = 1, message = "是否需要审核必须为0或1")
    private Integer needAudit;
    @Min(value = 0, message = "是否需要培训必须为0或1")
    @Max(value = 1, message = "是否需要培训必须为0或1")
    private Integer requireTraining;
    @Pattern(regexp = "^(HOUR|SAMPLE|PROJECT)?$", message = "计费单位取值不合法")
    private String bookingUnit;
    @Min(value = 1, message = "最小预约时长必须大于等于1")
    private Integer minReserveMinutes;
    @Min(value = 1, message = "最大预约时长必须大于等于1")
    private Integer maxReserveMinutes;
    @Min(value = 1, message = "时间步长必须大于等于1")
    private Integer stepMinutes;
}
