package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysUser;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserRepository {

    @Select("select u.*, d.dept_name as department_name, r.role_code as primary_role_code "
        + "from sys_user u "
        + "left join sys_department d on d.id = u.department_id "
        + "left join sys_user_role ur on ur.user_id = u.id "
        + "left join sys_role r on r.id = ur.role_id "
        + "where u.id = #{id} and u.deleted = 0 limit 1")
    SysUser findById(Long id);

    @Select("select u.*, d.dept_name as department_name, r.role_code as primary_role_code "
        + "from sys_user u "
        + "left join sys_department d on d.id = u.department_id "
        + "left join sys_user_role ur on ur.user_id = u.id "
        + "left join sys_role r on r.id = ur.role_id "
        + "where u.username = #{username} and u.deleted = 0 limit 1")
    SysUser findByUsername(String username);

    @Select("select u.*, d.dept_name as department_name, r.role_code as primary_role_code "
        + "from sys_user u "
        + "left join sys_department d on d.id = u.department_id "
        + "left join sys_user_role ur on ur.user_id = u.id "
        + "left join sys_role r on r.id = ur.role_id "
        + "where u.deleted = 0 order by u.id asc")
    List<SysUser> findAll();

    @Select("<script>"
        + "select u.*, d.dept_name as department_name, r.role_code as primary_role_code "
        + "from sys_user u "
        + "left join sys_department d on d.id = u.department_id "
        + "left join sys_user_role ur on ur.user_id = u.id "
        + "left join sys_role r on r.id = ur.role_id "
        + "where u.deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and (u.username like concat('%', #{keyword}, '%') or u.real_name like concat('%', #{keyword}, '%')) "
        + "</if>"
        + "order by u.id asc limit #{offset}, #{pageSize}"
        + "</script>")
    List<SysUser> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("<script>"
        + "select count(1) "
        + "from sys_user u "
        + "where u.deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and (u.username like concat('%', #{keyword}, '%') or u.real_name like concat('%', #{keyword}, '%')) "
        + "</if>"
        + "</script>")
    long countPage(@Param("keyword") String keyword);

    @Insert("insert into sys_user(username, password, real_name, user_type, user_no, gender, phone, email, avatar_url, auth_type, department_id, unit_name, title_name, status, last_login_time, remark, create_time, update_time, deleted) "
        + "values(#{username}, #{password}, #{realName}, #{userType}, #{userNo}, #{gender}, #{phone}, #{email}, #{avatarUrl}, #{authType}, #{departmentId}, #{unitName}, #{titleName}, #{status}, #{lastLoginTime}, #{remark}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("update sys_user set real_name=#{realName}, user_type=#{userType}, user_no=#{userNo}, gender=#{gender}, phone=#{phone}, email=#{email}, avatar_url=#{avatarUrl}, auth_type=#{authType}, department_id=#{departmentId}, unit_name=#{unitName}, title_name=#{titleName}, status=#{status}, remark=#{remark}, update_time=#{updateTime} where id=#{id}")
    int update(SysUser user);

    @Update("update sys_user set last_login_time = #{lastLoginTime}, update_time = #{lastLoginTime} where id = #{id}")
    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    @Update("update sys_user set password = #{password}, update_time = #{updateTime} where id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("updateTime") LocalDateTime updateTime);

    @Update("update sys_user set deleted = 1, update_time = #{updateTime} where id = #{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
