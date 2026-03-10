package com.unequipment.platform.modules.order.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResultFile {

    private Long id;
    private Long orderId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long uploadUserId;
    private LocalDateTime createTime;
    private Integer deleted;
}
