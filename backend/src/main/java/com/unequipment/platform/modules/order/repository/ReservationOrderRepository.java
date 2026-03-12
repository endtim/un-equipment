package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.ReservationOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationOrderRepository {

    ReservationOrder findById(@Param("id") Long id);

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

    long countByInstrumentId(@Param("instrumentId") Long instrumentId);

    long countDistinctUsersByInstrumentId(@Param("instrumentId") Long instrumentId);

    int countMachineConflict(@Param("instrumentId") Long instrumentId,
                             @Param("reserveEnd") LocalDateTime reserveEnd,
                             @Param("reserveStart") LocalDateTime reserveStart);

    List<ReservationOrder> findMachineReservedSlots(@Param("instrumentId") Long instrumentId,
                                                    @Param("dayStart") LocalDateTime dayStart,
                                                    @Param("dayEnd") LocalDateTime dayEnd);

    int insert(ReservationOrder order);

    int update(ReservationOrder order);

    int markSettling(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
