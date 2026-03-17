package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.FinanceAnomalyHandle;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinanceAnomalyHandleRepository {

    FinanceAnomalyHandle findByTypeAndOrderId(@Param("anomalyType") String anomalyType,
                                              @Param("orderId") Long orderId);

    int upsert(@Param("anomalyType") String anomalyType,
               @Param("orderId") Long orderId,
               @Param("settlementId") Long settlementId,
               @Param("handleStatus") String handleStatus,
               @Param("handleComment") String handleComment,
               @Param("handlerUserId") Long handlerUserId,
               @Param("handleTime") LocalDateTime handleTime,
               @Param("createTime") LocalDateTime createTime,
               @Param("updateTime") LocalDateTime updateTime);
}

