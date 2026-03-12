package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysUserRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserRoleRepository {

    List<SysUserRole> findByUserId(Long userId);

    int deleteByUserId(Long userId);

    int deleteByRoleId(@Param("roleId") Long roleId);

    int insert(SysUserRole userRole);

    long countByRoleId(@Param("roleId") Long roleId);
}
