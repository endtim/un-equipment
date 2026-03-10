package com.unequipment.platform.modules.content.repository;

import com.unequipment.platform.modules.content.entity.UserMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMessageRepository {

    @Select("select * from sys_message where user_id = #{userId} order by create_time desc")
    List<UserMessage> findByUserId(Long userId);

    @Select("select * from sys_message where id = #{id} and user_id = #{userId} limit 1")
    UserMessage findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("insert into sys_message(user_id, msg_type, title, content, biz_type, biz_id, read_status, read_time, create_time) "
        + "values(#{userId}, #{msgType}, #{title}, #{content}, #{bizType}, #{bizId}, #{readStatus}, #{readTime}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserMessage userMessage);

    @Update("update sys_message set read_status = 1, read_time = #{readTime} where id = #{id} and user_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    @Update("update sys_message set read_status = 1, read_time = #{readTime} where user_id = #{userId} and read_status = 0")
    int markAllRead(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
}
