package com.unequipment.platform.modules.content.entity;

import com.unequipment.platform.common.model.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HelpDoc extends BaseEntity {

    private String title;
    private String docType;
    private String summary;
    private String content;
    private String fileUrl;
    private Integer sortNo;
    private String publishStatus;
    private LocalDateTime publishTime;
}
