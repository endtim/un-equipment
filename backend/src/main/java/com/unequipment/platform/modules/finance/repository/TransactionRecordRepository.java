package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionRecordRepository {

    List<TransactionRecord> findByUserId(Long userId);

    int insert(TransactionRecord transactionRecord);
}
