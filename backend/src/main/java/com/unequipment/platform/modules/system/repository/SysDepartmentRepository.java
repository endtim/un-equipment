package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysDepartment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * SysDepartmentRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface SysDepartmentRepository {

    List<SysDepartment> findAll();

    List<SysDepartment> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage();

    SysDepartment findById(Long id);

    SysDepartment findFirst();

    SysDepartment findByDeptCode(@Param("deptCode") String deptCode);

    int insert(SysDepartment department);

    int update(SysDepartment department);

    int softDelete(@Param("id") Long id, @Param("operatorId") Long operatorId, @Param("updateTime") java.time.LocalDateTime updateTime);

    int countByDeptCodeExcludeId(@Param("deptCode") String deptCode, @Param("excludeId") Long excludeId);
}
