package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstrumentOpenRuleRepository {

    @Select("select * from biz_instrument_open_rule where deleted = 0 order by instrument_id asc, week_day asc, start_time asc")
    List<InstrumentOpenRule> findAll();

    @Select("select * from biz_instrument_open_rule where instrument_id = #{instrumentId} and deleted = 0 order by week_day asc, start_time asc")
    List<InstrumentOpenRule> findByInstrumentId(Long instrumentId);

    @Select("select * from biz_instrument_open_rule where id = #{id} and deleted = 0")
    InstrumentOpenRule findById(Long id);

    @Insert("insert into biz_instrument_open_rule(instrument_id, week_day, start_time, end_time, max_reserve_minutes, step_minutes, effective_start_date, effective_end_date, status, create_time, update_time, deleted) "
        + "values(#{instrumentId}, #{weekDay}, #{startTime}, #{endTime}, #{maxReserveMinutes}, #{stepMinutes}, #{effectiveStartDate}, #{effectiveEndDate}, #{status}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InstrumentOpenRule rule);

    @Update("update biz_instrument_open_rule set instrument_id=#{instrumentId}, week_day=#{weekDay}, start_time=#{startTime}, end_time=#{endTime}, max_reserve_minutes=#{maxReserveMinutes}, step_minutes=#{stepMinutes}, effective_start_date=#{effectiveStartDate}, effective_end_date=#{effectiveEndDate}, status=#{status}, update_time=#{updateTime} where id=#{id}")
    int update(InstrumentOpenRule rule);

    @Update("update biz_instrument_open_rule set deleted = 1, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
