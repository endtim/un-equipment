package com.unequipment.platform.modules.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unequipment.platform.common.model.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    private String username;
    @JsonIgnore
    private String password;
    private String realName;
    private String userType;
    private String userNo;
    private String gender;
    private String phone;
    private String email;
    private String avatarUrl;
    private String authType;
    private Long departmentId;
    private String unitName;
    private String titleName;
    private String status;
    private LocalDateTime lastLoginTime;
    private String remark;

    private String departmentName;
    private String primaryRoleCode;
}
