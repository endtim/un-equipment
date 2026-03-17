package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * InstrumentCategoryRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface InstrumentCategoryRepository {

    List<InstrumentCategory> findAll();

    List<InstrumentCategory> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    long countPage();

    InstrumentCategory findById(Long id);

    int insert(InstrumentCategory category);

    int update(InstrumentCategory category);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
