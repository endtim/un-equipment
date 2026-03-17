package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.vo.SettlementAdminVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * SettlementAdminRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface SettlementAdminRepository {

    List<SettlementAdminVO> findPage(@Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("departmentId") Long departmentId,
                                     @Param("orderId") Long orderId,
                                     @Param("createStart") LocalDateTime createStart,
                                     @Param("createEnd") LocalDateTime createEnd,
                                     @Param("settledStart") LocalDateTime settledStart,
                                     @Param("settledEnd") LocalDateTime settledEnd,
                                     @Param("roleCode") String roleCode,
                                     @Param("scopeDepartmentId") Long scopeDepartmentId,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    long countPage(@Param("keyword") String keyword,
                   @Param("status") String status,
                   @Param("departmentId") Long departmentId,
                   @Param("orderId") Long orderId,
                   @Param("createStart") LocalDateTime createStart,
                   @Param("createEnd") LocalDateTime createEnd,
                   @Param("settledStart") LocalDateTime settledStart,
                   @Param("settledEnd") LocalDateTime settledEnd,
                   @Param("roleCode") String roleCode,
                   @Param("scopeDepartmentId") Long scopeDepartmentId);

    SettlementAdminVO findDetail(@Param("id") Long id,
                                 @Param("roleCode") String roleCode,
                                 @Param("scopeDepartmentId") Long scopeDepartmentId);
}
