package com.unequipment.platform.common.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public abstract class BaseEntity {

    private Long id;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer deleted;
}
