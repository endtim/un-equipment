package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysDepartment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysDepartmentRepository {

    List<SysDepartment> findAll();

    SysDepartment findById(Long id);

    SysDepartment findFirst();

    SysDepartment findByDeptCode(@Param("deptCode") String deptCode);

    int insert(SysDepartment department);

    int update(SysDepartment department);

    int softDelete(@Param("id") Long id, @Param("operatorId") Long operatorId, @Param("updateTime") java.time.LocalDateTime updateTime);
}
