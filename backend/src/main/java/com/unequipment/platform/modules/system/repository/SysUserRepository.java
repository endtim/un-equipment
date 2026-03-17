package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * SysUserRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface SysUserRepository {

    SysUser findById(@Param("id") Long id);

    SysUser findByUsername(@Param("username") String username);

    List<SysUser> findAll();

    List<SysUser> findPage(@Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    List<SysUser> findPageByDepartment(@Param("departmentId") Long departmentId,
                                       @Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    long countPage(@Param("keyword") String keyword);

    long countPageByDepartment(@Param("departmentId") Long departmentId,
                               @Param("keyword") String keyword);

    int insert(SysUser user);

    int update(SysUser user);

    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    int updatePassword(@Param("id") Long id,
                       @Param("password") String password,
                       @Param("updateTime") LocalDateTime updateTime);

    int softDelete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    long countByDepartmentId(@Param("departmentId") Long departmentId);

    int countByUsernameExcludeId(@Param("username") String username, @Param("excludeId") Long excludeId);
}
