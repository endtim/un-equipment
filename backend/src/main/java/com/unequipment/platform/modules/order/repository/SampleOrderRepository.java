package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.SampleOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SampleOrderRepository {

    SampleOrder findByOrderId(Long orderId);

    int insert(SampleOrder sampleOrder);

    int update(SampleOrder sampleOrder);

    long sumSampleCountByInstrumentId(Long instrumentId);
}
