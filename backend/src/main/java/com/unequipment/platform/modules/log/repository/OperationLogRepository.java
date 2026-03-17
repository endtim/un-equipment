package com.unequipment.platform.modules.log.repository;

import com.unequipment.platform.modules.log.entity.OperationLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * OperationLogRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface OperationLogRepository {

    List<OperationLog> findAll();

    List<OperationLog> findPage(@Param("moduleName") String moduleName, @Param("keyword") String keyword,
                                @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage(@Param("moduleName") String moduleName, @Param("keyword") String keyword);

    int insert(OperationLog log);
}
