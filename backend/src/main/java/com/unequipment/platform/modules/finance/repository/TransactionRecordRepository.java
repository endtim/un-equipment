package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * TransactionRecordRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface TransactionRecordRepository {

    List<TransactionRecord> findByUserId(Long userId);

    List<TransactionRecord> findPageByUserId(@Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    long countByUserId(Long userId);

    int insert(TransactionRecord transactionRecord);
}
