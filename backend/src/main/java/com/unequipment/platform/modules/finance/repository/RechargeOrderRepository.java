package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RechargeOrderRepository {

    List<RechargeOrder> findAll();

    List<RechargeOrder> findByUserId(Long userId);

    RechargeOrder findById(Long id);

    int insert(RechargeOrder rechargeOrder);

    int update(RechargeOrder rechargeOrder);

    int updateIfPending(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("remark") String remark,
                        @Param("auditUserId") Long auditUserId,
                        @Param("auditTime") LocalDateTime auditTime,
                        @Param("updateTime") LocalDateTime updateTime);
}
