package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysUserRole;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserRoleRepository {

    @Select("select * from sys_user_role where user_id = #{userId}")
    List<SysUserRole> findByUserId(Long userId);

    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Insert("insert into sys_user_role(user_id, role_id, create_time) values(#{userId}, #{roleId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUserRole userRole);
}
