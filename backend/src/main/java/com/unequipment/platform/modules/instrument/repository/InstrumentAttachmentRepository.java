package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * InstrumentAttachmentRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface InstrumentAttachmentRepository {

    List<InstrumentAttachment> findAll();

    List<InstrumentAttachment> findAllByScope(@Param("roleCode") String roleCode,
                                              @Param("operatorId") Long operatorId,
                                              @Param("departmentId") Long departmentId);

    List<InstrumentAttachment> findByInstrumentId(Long instrumentId);

    List<InstrumentAttachment> findPageByScope(@Param("roleCode") String roleCode,
                                               @Param("operatorId") Long operatorId,
                                               @Param("departmentId") Long departmentId,
                                               @Param("offset") int offset,
                                               @Param("pageSize") int pageSize);

    long countByScope(@Param("roleCode") String roleCode,
                      @Param("operatorId") Long operatorId,
                      @Param("departmentId") Long departmentId);

    InstrumentAttachment findById(Long id);

    int insert(InstrumentAttachment attachment);

    int update(InstrumentAttachment attachment);

    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);

    int softDeleteByInstrumentId(@Param("instrumentId") Long instrumentId, @Param("updateTime") java.time.LocalDateTime updateTime);
}
