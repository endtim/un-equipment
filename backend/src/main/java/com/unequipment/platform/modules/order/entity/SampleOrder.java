package com.unequipment.platform.modules.order.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SampleOrder {

    private Long id;
    private Long orderId;
    private String sampleName;
    private Integer sampleCount;
    private String sampleType;
    private String sampleSpec;
    private String testRequirement;
    private Integer dangerFlag;
    private String dangerDesc;
    private String receiveStatus;
    private LocalDateTime receivedTime;
    private Long receiverUserId;
    private String testingStatus;
    private String resultSummary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
