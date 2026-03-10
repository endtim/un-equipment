package com.unequipment.platform.modules.system.entity;

import com.unequipment.platform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDepartment extends BaseEntity {

    private Long parentId;
    private String deptName;
    private String deptCode;
    private Long leaderUserId;
    private String phone;
    private String email;
    private Integer sortNo;
    private String status;
    private String remark;
}
