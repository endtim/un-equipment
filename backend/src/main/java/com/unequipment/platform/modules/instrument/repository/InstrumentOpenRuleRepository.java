package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * InstrumentOpenRuleRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface InstrumentOpenRuleRepository {

    List<InstrumentOpenRule> findAll();

    List<InstrumentOpenRule> findAllByScope(@Param("roleCode") String roleCode,
                                            @Param("operatorId") Long operatorId,
                                            @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<InstrumentOpenRule> findPageByScope(@Param("instrumentId") Long instrumentId,
                                             @Param("weekDay") Integer weekDay,
                                             @Param("status") String status,
                                             @Param("roleCode") String roleCode,
                                             @Param("operatorId") Long operatorId,
                                             @Param("scopeDepartmentId") Long scopeDepartmentId,
                                             @Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    long countPageByScope(@Param("instrumentId") Long instrumentId,
                          @Param("weekDay") Integer weekDay,
                          @Param("status") String status,
                          @Param("roleCode") String roleCode,
                          @Param("operatorId") Long operatorId,
                          @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<InstrumentOpenRule> findByInstrumentId(Long instrumentId);

    InstrumentOpenRule findById(Long id);

    int insert(InstrumentOpenRule rule);

    int update(InstrumentOpenRule rule);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int softDeleteByInstrumentId(@Param("instrumentId") Long instrumentId, @Param("updateTime") java.time.LocalDateTime updateTime);
}
