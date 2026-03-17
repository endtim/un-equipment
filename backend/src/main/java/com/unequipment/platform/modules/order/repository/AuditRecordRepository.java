package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.AuditRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * AuditRecordRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface AuditRecordRepository {

    List<AuditRecord> findByOrderId(Long orderId);

    Integer findMaxNodeNo(Long orderId);

    int insert(AuditRecord auditRecord);
}
