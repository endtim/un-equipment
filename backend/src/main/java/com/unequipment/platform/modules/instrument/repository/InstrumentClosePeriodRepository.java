package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentClosePeriod;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InstrumentClosePeriodRepository {

    List<InstrumentClosePeriod> findByInstrumentId(Long instrumentId);

    int insert(InstrumentClosePeriod closePeriod);
}
