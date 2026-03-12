package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.UsageRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsageRecordRepository {

    UsageRecord findByOrderId(Long orderId);

    int insert(UsageRecord usageRecord);

    int update(UsageRecord usageRecord);

    long sumActualMinutesByInstrumentId(Long instrumentId);
}
