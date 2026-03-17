package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.finance.vo.FinanceAnomalyVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * ReservationOrderRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface ReservationOrderRepository {

    ReservationOrder findById(@Param("id") Long id);

    ReservationOrder findByIdForUpdate(@Param("id") Long id);

    List<ReservationOrder> findAll();

    List<ReservationOrder> findPageForAdmin(@Param("orderType") String orderType,
                                            @Param("status") String status,
                                            @Param("keyword") String keyword,
                                            @Param("submitStart") LocalDateTime submitStart,
                                            @Param("submitEnd") LocalDateTime submitEnd,
                                            @Param("filterDepartmentId") Long filterDepartmentId,
                                            @Param("auditorKeyword") String auditorKeyword,
                                            @Param("minAmount") BigDecimal minAmount,
                                            @Param("maxAmount") BigDecimal maxAmount,
                                            @Param("roleCode") String roleCode,
                                            @Param("operatorId") Long operatorId,
                                            @Param("scopeDepartmentId") Long scopeDepartmentId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    long countForAdmin(@Param("orderType") String orderType,
                       @Param("status") String status,
                       @Param("keyword") String keyword,
                       @Param("submitStart") LocalDateTime submitStart,
                       @Param("submitEnd") LocalDateTime submitEnd,
                       @Param("filterDepartmentId") Long filterDepartmentId,
                       @Param("auditorKeyword") String auditorKeyword,
                       @Param("minAmount") BigDecimal minAmount,
                       @Param("maxAmount") BigDecimal maxAmount,
                       @Param("roleCode") String roleCode,
                       @Param("operatorId") Long operatorId,
                       @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<ReservationOrder> findByUserId(@Param("userId") Long userId);

    List<ReservationOrder> findByUserIdAndOrderType(@Param("userId") Long userId,
                                                    @Param("orderType") String orderType);

    List<ReservationOrder> findPageByUser(@Param("userId") Long userId,
                                          @Param("orderType") String orderType,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    long countByUser(@Param("userId") Long userId,
                     @Param("orderType") String orderType);

    List<ReservationOrder> findPendingAuditExpired(@Param("cutoffTime") LocalDateTime cutoffTime,
                                                   @Param("limit") int limit);

    long countByInstrumentId(@Param("instrumentId") Long instrumentId);

    long countDistinctUsersByInstrumentId(@Param("instrumentId") Long instrumentId);

    int countMachineConflict(@Param("instrumentId") Long instrumentId,
                             @Param("reserveEnd") LocalDateTime reserveEnd,
                             @Param("reserveStart") LocalDateTime reserveStart);

    int countMachineConflictExcludeOrder(@Param("instrumentId") Long instrumentId,
                                         @Param("reserveEnd") LocalDateTime reserveEnd,
                                         @Param("reserveStart") LocalDateTime reserveStart,
                                         @Param("excludeOrderId") Long excludeOrderId);

    List<ReservationOrder> findMachineReservedSlots(@Param("instrumentId") Long instrumentId,
                                                    @Param("dayStart") LocalDateTime dayStart,
                                                    @Param("dayEnd") LocalDateTime dayEnd);

    int insert(ReservationOrder order);

    int update(ReservationOrder order);

    int updateByIdAndStatus(@Param("order") ReservationOrder order,
                            @Param("expectedStatus") String expectedStatus);

    int markSettling(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    Integer tryAcquireNamedLock(@Param("lockKey") String lockKey,
                                @Param("timeoutSeconds") int timeoutSeconds);

    Integer releaseNamedLock(@Param("lockKey") String lockKey);

    long countByScopeAndCreateTime(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("roleCode") String roleCode,
                                   @Param("scopeDepartmentId") Long scopeDepartmentId);

    long countCompletedButUnsettledByScope(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("roleCode") String roleCode,
                                           @Param("scopeDepartmentId") Long scopeDepartmentId);

    long countWaitingSettlementByScope(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("roleCode") String roleCode,
                                       @Param("scopeDepartmentId") Long scopeDepartmentId);

    long countConfirmedButUnpaidByScope(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("roleCode") String roleCode,
                                        @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal avgWaitingSettlementHoursByScope(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("roleCode") String roleCode,
                                                @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<FinanceAnomalyVO> findFinanceAnomalyPage(@Param("type") String type,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("roleCode") String roleCode,
                                                  @Param("scopeDepartmentId") Long scopeDepartmentId,
                                                  @Param("offset") int offset,
                                                  @Param("pageSize") int pageSize);

    long countFinanceAnomaly(@Param("type") String type,
                             @Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime,
                             @Param("roleCode") String roleCode,
                             @Param("scopeDepartmentId") Long scopeDepartmentId);
}
