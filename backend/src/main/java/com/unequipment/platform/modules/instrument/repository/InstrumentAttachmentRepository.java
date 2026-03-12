package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InstrumentAttachmentRepository {

    List<InstrumentAttachment> findAll();

    List<InstrumentAttachment> findByInstrumentId(Long instrumentId);

    InstrumentAttachment findById(Long id);

    int insert(InstrumentAttachment attachment);

    int update(InstrumentAttachment attachment);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int softDeleteByInstrumentId(@Param("instrumentId") Long instrumentId, @Param("updateTime") java.time.LocalDateTime updateTime);
}
