package com.unequipment.platform.modules.system.entity;

import com.unequipment.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

    private String roleName;
    private String roleCode;
    private String status;
    private String remark;
}
