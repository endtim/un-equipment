package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.FinanceBudget;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinanceBudgetRepository {

    int insert(FinanceBudget budget);

    int update(FinanceBudget budget);

    FinanceBudget findById(@Param("id") Long id);

    FinanceBudget findByScope(@Param("budgetYear") Integer budgetYear,
                              @Param("departmentId") Long departmentId,
                              @Param("instrumentId") Long instrumentId);

    List<FinanceBudget> findPageByScope(@Param("budgetYear") Integer budgetYear,
                                        @Param("departmentId") Long departmentId,
                                        @Param("instrumentId") Long instrumentId,
                                        @Param("status") String status,
                                        @Param("roleCode") String roleCode,
                                        @Param("scopeDepartmentId") Long scopeDepartmentId,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    long countPageByScope(@Param("budgetYear") Integer budgetYear,
                          @Param("departmentId") Long departmentId,
                          @Param("instrumentId") Long instrumentId,
                          @Param("status") String status,
                          @Param("roleCode") String roleCode,
                          @Param("scopeDepartmentId") Long scopeDepartmentId);

    List<FinanceBudget> findAllForWarning(@Param("budgetYear") Integer budgetYear,
                                          @Param("roleCode") String roleCode,
                                          @Param("scopeDepartmentId") Long scopeDepartmentId);
}
