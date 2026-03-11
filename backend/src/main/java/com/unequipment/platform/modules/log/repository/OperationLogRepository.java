package com.unequipment.platform.modules.log.repository;

import com.unequipment.platform.modules.log.entity.OperationLog;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationLogRepository {

    @Select("select l.*, u.real_name as operator_name "
        + "from sys_operation_log l "
        + "left join sys_user u on u.id = l.user_id "
        + "order by l.create_time desc, l.id desc")
    List<OperationLog> findAll();

    @Select("<script>"
        + "select l.*, u.real_name as operator_name "
        + "from sys_operation_log l "
        + "left join sys_user u on u.id = l.user_id "
        + "where 1 = 1 "
        + "<if test='moduleName != null and moduleName != \"\"'>"
        + "and l.module_name = #{moduleName} "
        + "</if>"
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and (l.action_name like concat('%', #{keyword}, '%') or l.request_uri like concat('%', #{keyword}, '%')) "
        + "</if>"
        + "order by l.create_time desc, l.id desc limit #{offset}, #{pageSize}"
        + "</script>")
    List<OperationLog> findPage(@Param("moduleName") String moduleName, @Param("keyword") String keyword,
                                @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("<script>"
        + "select count(1) from sys_operation_log l where 1 = 1 "
        + "<if test='moduleName != null and moduleName != \"\"'>"
        + "and l.module_name = #{moduleName} "
        + "</if>"
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and (l.action_name like concat('%', #{keyword}, '%') or l.request_uri like concat('%', #{keyword}, '%')) "
        + "</if>"
        + "</script>")
    long countPage(@Param("moduleName") String moduleName, @Param("keyword") String keyword);

    @Insert("insert into sys_operation_log(user_id, module_name, action_name, request_method, request_uri, request_ip, result_code, result_msg, biz_id, create_time) "
        + "values(#{userId}, #{moduleName}, #{actionName}, #{requestMethod}, #{requestUri}, #{requestIp}, #{resultCode}, #{resultMsg}, #{bizId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);
}
