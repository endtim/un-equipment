package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.Instrument;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InstrumentRepository {

    List<Instrument> findAll();

    List<Instrument> findPageByCondition(@Param("keyword") String keyword,
                                         @Param("status") String status,
                                         @Param("categoryId") Long categoryId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    List<Instrument> findPageByScope(@Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("categoryId") Long categoryId,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize,
                                     @Param("roleCode") String roleCode,
                                     @Param("operatorId") Long operatorId,
                                     @Param("departmentId") Long departmentId);

    long countPageByCondition(@Param("keyword") String keyword,
                              @Param("status") String status,
                              @Param("categoryId") Long categoryId);

    long countPageByScope(@Param("keyword") String keyword,
                          @Param("status") String status,
                          @Param("categoryId") Long categoryId,
                          @Param("roleCode") String roleCode,
                          @Param("operatorId") Long operatorId,
                          @Param("departmentId") Long departmentId);

    long countAll();

    long countByCategoryId(@Param("categoryId") Long categoryId);

    Instrument findById(@Param("id") Long id);

    List<Instrument> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    int insert(Instrument instrument);

    int update(Instrument instrument);

    int softDelete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
