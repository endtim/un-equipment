package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentClosePeriod;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InstrumentClosePeriodRepository {

    @Select("select * from biz_instrument_close_period where instrument_id = #{instrumentId} order by close_start desc")
    List<InstrumentClosePeriod> findByInstrumentId(Long instrumentId);

    @Insert("insert into biz_instrument_close_period(instrument_id, close_start, close_end, close_type, reason, create_time) "
        + "values(#{instrumentId}, #{closeStart}, #{closeEnd}, #{closeType}, #{reason}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InstrumentClosePeriod closePeriod);
}
