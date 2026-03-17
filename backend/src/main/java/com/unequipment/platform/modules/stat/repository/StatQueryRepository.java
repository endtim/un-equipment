package com.unequipment.platform.modules.stat.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * StatQueryRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface StatQueryRepository {

    long countInstruments();

    long countDepartments();

    long countOrders(@Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime,
                     @Param("roleCode") String roleCode,
                     @Param("scopeDepartmentId") Long scopeDepartmentId);

    long countOrdersByStatus(@Param("status") String status,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime,
                             @Param("roleCode") String roleCode,
                             @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal avgOrderFinalAmount(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("roleCode") String roleCode,
                                   @Param("scopeDepartmentId") Long scopeDepartmentId);

    long countSettlementsByStatus(@Param("status") String status,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  @Param("roleCode") String roleCode,
                                  @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal sumSettlementAmountByStatus(@Param("status") String status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("roleCode") String roleCode,
                                           @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<Map<String, Object>> queryTopInstruments(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("roleCode") String roleCode,
                                                  @Param("scopeDepartmentId") Long scopeDepartmentId,
                                                  @Param("limit") int limit);

    List<Map<String, Object>> queryDepartmentDistribution(@Param("startTime") LocalDateTime startTime,
                                                          @Param("endTime") LocalDateTime endTime,
                                                          @Param("roleCode") String roleCode,
                                                          @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<Map<String, Object>> queryDailyTrend(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("roleCode") String roleCode,
                                              @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<Map<String, Object>> queryMonthlyTrend(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("roleCode") String roleCode,
                                                @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<Map<String, Object>> queryPlatformMembers(@Param("roleCode") String roleCode,
                                                   @Param("scopeDepartmentId") Long scopeDepartmentId);
}
