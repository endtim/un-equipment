package com.unequipment.platform.modules.instrument.repository;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrainingQualificationRepository {

    long countValidByUserAndInstrument(@Param("userId") Long userId,
                                       @Param("instrumentId") Long instrumentId,
                                       @Param("now") LocalDateTime now);
}

