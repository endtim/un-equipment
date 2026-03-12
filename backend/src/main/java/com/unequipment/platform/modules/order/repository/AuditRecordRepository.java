package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.AuditRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditRecordRepository {

    List<AuditRecord> findByOrderId(Long orderId);

    Integer findMaxNodeNo(Long orderId);

    int insert(AuditRecord auditRecord);
}
