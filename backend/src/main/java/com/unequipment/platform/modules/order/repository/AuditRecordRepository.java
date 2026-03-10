package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.AuditRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditRecordRepository {

    @Select("select * from biz_reservation_audit where order_id = #{orderId} order by node_no asc, id asc")
    List<AuditRecord> findByOrderId(Long orderId);

    @Select("select coalesce(max(node_no), 0) from biz_reservation_audit where order_id = #{orderId}")
    Integer findMaxNodeNo(Long orderId);

    @Insert("insert into biz_reservation_audit(order_id, node_no, auditor_id, auditor_role, audit_result, audit_opinion, audit_time, create_time) "
        + "values(#{orderId}, #{nodeNo}, #{auditorId}, #{auditorRole}, #{auditResult}, #{auditOpinion}, #{auditTime}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditRecord auditRecord);
}
