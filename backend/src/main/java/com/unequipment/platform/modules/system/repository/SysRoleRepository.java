package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRoleRepository {

    List<SysRole> findAll();

    List<SysRole> findAllNonAdmin();

    SysRole findByRoleCode(String roleCode);

    SysRole findById(Long id);

    int insert(SysRole role);

    int update(SysRole role);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
