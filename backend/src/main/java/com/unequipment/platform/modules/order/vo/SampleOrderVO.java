package com.unequipment.platform.modules.order.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SampleOrderVO {

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
}
