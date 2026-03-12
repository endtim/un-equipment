package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InstrumentOpenRuleRepository {

    List<InstrumentOpenRule> findAll();

    List<InstrumentOpenRule> findByInstrumentId(Long instrumentId);

    InstrumentOpenRule findById(Long id);

    int insert(InstrumentOpenRule rule);

    int update(InstrumentOpenRule rule);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int softDeleteByInstrumentId(@Param("instrumentId") Long instrumentId, @Param("updateTime") java.time.LocalDateTime updateTime);
}
