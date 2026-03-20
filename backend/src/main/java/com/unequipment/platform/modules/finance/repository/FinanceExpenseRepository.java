package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.FinanceExpense;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinanceExpenseRepository {

    int insert(FinanceExpense expense);

    List<FinanceExpense> findPageByScope(@Param("keyword") String keyword,
                                         @Param("expenseType") String expenseType,
                                         @Param("instrumentId") Long instrumentId,
                                         @Param("departmentId") Long departmentId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("roleCode") String roleCode,
                                         @Param("scopeDepartmentId") Long scopeDepartmentId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    long countPageByScope(@Param("keyword") String keyword,
                          @Param("expenseType") String expenseType,
                          @Param("instrumentId") Long instrumentId,
                          @Param("departmentId") Long departmentId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("roleCode") String roleCode,
                          @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal sumAmountByScope(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("roleCode") String roleCode,
                                @Param("scopeDepartmentId") Long scopeDepartmentId);

    BigDecimal sumAmountByBudgetScope(@Param("budgetYear") Integer budgetYear,
                                      @Param("departmentId") Long departmentId,
                                      @Param("instrumentId") Long instrumentId,
                                      @Param("roleCode") String roleCode,
                                      @Param("scopeDepartmentId") Long scopeDepartmentId);
}
