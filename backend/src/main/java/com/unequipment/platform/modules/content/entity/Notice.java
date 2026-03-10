package com.unequipment.platform.modules.content.entity;

import com.unequipment.platform.common.model.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Notice extends BaseEntity {

    private String title;
    private String category;
    private String summary;
    private String content;
    private String coverUrl;
    private Long instrumentId;
    private Integer topFlag;
    private String publishStatus;
    private LocalDateTime publishTime;
    private Integer viewCount;
}
