package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * UsageRecordRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface UsageRecordRepository {

    UsageRecord findByOrderId(Long orderId);

    int insert(UsageRecord usageRecord);

    int update(UsageRecord usageRecord);

    long sumActualMinutesByInstrumentId(Long instrumentId);
}
