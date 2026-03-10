package com.unequipment.platform.modules.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserMessage {

    private Long id;
    private Long userId;
    private String msgType;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
