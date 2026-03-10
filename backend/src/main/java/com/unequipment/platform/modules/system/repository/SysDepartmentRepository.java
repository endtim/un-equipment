package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.SysDepartment;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysDepartmentRepository {

    @Select("select * from sys_department where deleted = 0 order by sort_no asc, id asc")
    List<SysDepartment> findAll();

    @Select("select * from sys_department where id = #{id} and deleted = 0")
    SysDepartment findById(Long id);

    @Select("select * from sys_department where deleted = 0 order by id asc limit 1")
    SysDepartment findFirst();

    @Insert("insert into sys_department(parent_id, dept_name, dept_code, leader_user_id, phone, email, sort_no, status, remark, create_by, create_time, update_by, update_time, deleted) "
        + "values(#{parentId}, #{deptName}, #{deptCode}, #{leaderUserId}, #{phone}, #{email}, #{sortNo}, #{status}, #{remark}, #{createBy}, #{createTime}, #{updateBy}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysDepartment department);

    @Update("update sys_department set parent_id=#{parentId}, dept_name=#{deptName}, dept_code=#{deptCode}, leader_user_id=#{leaderUserId}, phone=#{phone}, email=#{email}, sort_no=#{sortNo}, status=#{status}, remark=#{remark}, update_by=#{updateBy}, update_time=#{updateTime} where id=#{id}")
    int update(SysDepartment department);

    @Update("update sys_department set deleted = 1, update_by=#{operatorId}, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("operatorId") Long operatorId, @Param("updateTime") java.time.LocalDateTime updateTime);
}
