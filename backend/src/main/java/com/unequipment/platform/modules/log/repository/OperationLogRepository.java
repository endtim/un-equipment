package com.unequipment.platform.modules.log.repository;

import com.unequipment.platform.modules.log.entity.OperationLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationLogRepository {

    List<OperationLog> findAll();

    List<OperationLog> findPage(@Param("moduleName") String moduleName, @Param("keyword") String keyword,
                                @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage(@Param("moduleName") String moduleName, @Param("keyword") String keyword);

    int insert(OperationLog log);
}
