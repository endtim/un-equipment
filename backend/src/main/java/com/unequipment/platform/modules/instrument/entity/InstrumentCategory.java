package com.unequipment.platform.modules.instrument.entity;

import com.unequipment.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InstrumentCategory extends BaseEntity {

    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private Integer sortNo;
    private String status;
}
