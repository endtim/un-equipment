package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysRoleRepository {

    @Select("select * from sys_role where deleted = 0 order by id asc")
    List<SysRole> findAll();

    @Select("select * from sys_role where role_code = #{roleCode} and deleted = 0 limit 1")
    SysRole findByRoleCode(String roleCode);

    @Select("select * from sys_role where id = #{id} and deleted = 0")
    SysRole findById(Long id);

    @Insert("insert into sys_role(role_name, role_code, status, remark, create_time, update_time, deleted) "
        + "values(#{roleName}, #{roleCode}, #{status}, #{remark}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysRole role);

    @Update("update sys_role set role_name=#{roleName}, role_code=#{roleCode}, status=#{status}, remark=#{remark}, update_time=#{updateTime} where id=#{id}")
    int update(SysRole role);

    @Update("update sys_role set deleted = 1, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
