package com.unequipment.platform.modules.content.repository;

import com.unequipment.platform.modules.content.entity.UserMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMessageRepository {

    List<UserMessage> findByUserId(Long userId);

    List<UserMessage> findPageByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    long countByUserId(@Param("userId") Long userId);

    UserMessage findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int insert(UserMessage userMessage);

    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    int markAllRead(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
}
