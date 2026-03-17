package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.SampleOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * SampleOrderRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface SampleOrderRepository {

    SampleOrder findByOrderId(Long orderId);

    int insert(SampleOrder sampleOrder);

    int update(SampleOrder sampleOrder);

    long sumSampleCountByInstrumentId(Long instrumentId);
}
