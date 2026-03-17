package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentClosePeriod;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * InstrumentClosePeriodRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface InstrumentClosePeriodRepository {

    List<InstrumentClosePeriod> findByInstrumentId(Long instrumentId);

    int insert(InstrumentClosePeriod closePeriod);
}
