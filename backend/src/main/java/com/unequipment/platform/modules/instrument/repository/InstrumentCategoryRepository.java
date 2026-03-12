package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InstrumentCategoryRepository {

    List<InstrumentCategory> findAll();

    InstrumentCategory findById(Long id);

    int insert(InstrumentCategory category);

    int update(InstrumentCategory category);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
