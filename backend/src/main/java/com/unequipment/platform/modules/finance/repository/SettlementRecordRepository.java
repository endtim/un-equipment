package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * SettlementRecordRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface SettlementRecordRepository {

    List<SettlementRecord> findAll();

    SettlementRecord findByOrderId(@Param("orderId") Long orderId);

    SettlementRecord findById(@Param("id") Long id);

    int insert(SettlementRecord settlementRecord);

    int updatePendingByOrderId(@Param("orderId") Long orderId,
                               @Param("billType") String billType,
                               @Param("priceDesc") String priceDesc,
                               @Param("estimatedAmount") BigDecimal estimatedAmount,
                               @Param("discountAmount") BigDecimal discountAmount,
                               @Param("finalAmount") BigDecimal finalAmount);

    int confirmByOrderId(@Param("orderId") Long orderId,
                         @Param("finalAmount") BigDecimal finalAmount,
                         @Param("settledTime") LocalDateTime settledTime,
                         @Param("operatorUserId") Long operatorUserId);

    int updateStatusByOrderId(@Param("orderId") Long orderId,
                              @Param("settleStatus") String settleStatus,
                              @Param("settledTime") LocalDateTime settledTime,
                              @Param("operatorUserId") Long operatorUserId,
                              @Param("finalAmount") BigDecimal finalAmount);

    int updateStatusByIdWhenCurrent(@Param("id") Long id,
                                    @Param("currentStatus") String currentStatus,
                                    @Param("targetStatus") String targetStatus,
                                    @Param("operatorUserId") Long operatorUserId,
                                    @Param("settledTime") LocalDateTime settledTime);

    List<SettlementRecord> findRefundingTimeout(@Param("beforeTime") LocalDateTime beforeTime,
                                                @Param("limit") int limit);

    int updateStatusById(@Param("id") Long id,
                         @Param("targetStatus") String targetStatus,
                         @Param("operatorUserId") Long operatorUserId,
                         @Param("settledTime") LocalDateTime settledTime);

    long countSettledByScopeAndSettledTime(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("roleCode") String roleCode,
                                           @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal sumFinalAmountByStatusAndScopeAndSettledTime(@Param("settleStatus") String settleStatus,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime,
                                                             @Param("roleCode") String roleCode,
                                                             @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal avgSettleHoursByScopeAndSettledTime(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("roleCode") String roleCode,
                                                   @Param("scopeDepartmentId") Long scopeDepartmentId);
}
