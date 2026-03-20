package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.vo.FinanceDetailVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinanceDetailRepository {

    List<FinanceDetailVO> findPage(@Param("keyword") String keyword,
                                   @Param("bizType") String bizType,
                                   @Param("inoutType") String inoutType,
                                   @Param("instrumentId") Long instrumentId,
                                   @Param("departmentId") Long departmentId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("roleCode") String roleCode,
                                   @Param("scopeDepartmentId") Long scopeDepartmentId,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    long countPage(@Param("keyword") String keyword,
                   @Param("bizType") String bizType,
                   @Param("inoutType") String inoutType,
                   @Param("instrumentId") Long instrumentId,
                   @Param("departmentId") Long departmentId,
                   @Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime,
                   @Param("roleCode") String roleCode,
                   @Param("scopeDepartmentId") Long scopeDepartmentId);
}
