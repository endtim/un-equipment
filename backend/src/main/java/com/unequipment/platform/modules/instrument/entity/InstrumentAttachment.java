package com.unequipment.platform.modules.instrument.entity;

import com.unequipment.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InstrumentAttachment extends BaseEntity {

    private Long instrumentId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Integer sortNo;
}
