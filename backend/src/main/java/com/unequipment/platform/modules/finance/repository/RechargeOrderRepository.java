package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * RechargeOrderRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface RechargeOrderRepository {

    List<RechargeOrder> findByUserId(Long userId);

    List<RechargeOrder> findPageByUserId(@Param("userId") Long userId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    List<RechargeOrder> findPageByScope(@Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("userId") Long userId,
                                        @Param("auditUserId") Long auditUserId,
                                        @Param("minAmount") java.math.BigDecimal minAmount,
                                        @Param("maxAmount") java.math.BigDecimal maxAmount,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("roleCode") String roleCode,
                                        @Param("scopeDepartmentId") Long scopeDepartmentId,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    long countPageByScope(@Param("keyword") String keyword,
                          @Param("status") String status,
                          @Param("userId") Long userId,
                          @Param("auditUserId") Long auditUserId,
                          @Param("minAmount") java.math.BigDecimal minAmount,
                          @Param("maxAmount") java.math.BigDecimal maxAmount,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("roleCode") String roleCode,
                          @Param("scopeDepartmentId") Long scopeDepartmentId);

    RechargeOrder findById(Long id);

    int insert(RechargeOrder rechargeOrder);

    int update(RechargeOrder rechargeOrder);

    int updateIfPending(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("remark") String remark,
                        @Param("auditUserId") Long auditUserId,
                        @Param("auditTime") LocalDateTime auditTime,
                        @Param("updateTime") LocalDateTime updateTime);

    int updateFirstApproveIfPending(@Param("id") Long id,
                                    @Param("status") String status,
                                    @Param("reviewStatus") String reviewStatus,
                                    @Param("firstAuditUserId") Long firstAuditUserId,
                                    @Param("firstAuditTime") LocalDateTime firstAuditTime,
                                    @Param("auditUserId") Long auditUserId,
                                    @Param("auditTime") LocalDateTime auditTime,
                                    @Param("updateTime") LocalDateTime updateTime);

    int updateSecondApproveIfReviewPending(@Param("id") Long id,
                                           @Param("status") String status,
                                           @Param("reviewStatus") String reviewStatus,
                                           @Param("secondAuditUserId") Long secondAuditUserId,
                                           @Param("secondAuditTime") LocalDateTime secondAuditTime,
                                           @Param("auditUserId") Long auditUserId,
                                           @Param("auditTime") LocalDateTime auditTime,
                                           @Param("updateTime") LocalDateTime updateTime);

    int rejectIfPendingOrReviewPending(@Param("id") Long id,
                                       @Param("status") String status,
                                       @Param("reviewStatus") String reviewStatus,
                                       @Param("remark") String remark,
                                       @Param("auditUserId") Long auditUserId,
                                       @Param("auditTime") LocalDateTime auditTime,
                                       @Param("updateTime") LocalDateTime updateTime);

    long countByScopeAndCreateTime(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("roleCode") String roleCode,
                                   @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal sumAmountByStatusAndScope(@Param("status") String status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("roleCode") String roleCode,
                                         @Param("scopeDepartmentId") Long scopeDepartmentId);

    long countByStatusAndScope(@Param("status") String status,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("roleCode") String roleCode,
                               @Param("scopeDepartmentId") Long scopeDepartmentId);
}
