package com.unequipment.platform.modules.system.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysUserRole {

    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createTime;
}
