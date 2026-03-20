package com.unequipment.platform.modules.system.repository;

import com.unequipment.platform.modules.system.entity.UserAuditLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAuditLogRepository {

    int insert(UserAuditLog log);

    List<UserAuditLog> findPageByUserId(@Param("userId") Long userId,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    long countByUserId(@Param("userId") Long userId);
}

