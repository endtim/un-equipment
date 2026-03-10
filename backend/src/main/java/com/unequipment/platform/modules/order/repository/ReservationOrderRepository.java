package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.ReservationOrder;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReservationOrderRepository {

    @Select("select o.*, u.real_name as user_name, i.instrument_name "
        + "from biz_reservation_order o "
        + "left join sys_user u on u.id = o.user_id "
        + "left join biz_instrument i on i.id = o.instrument_id "
        + "where o.id = #{id} and o.deleted = 0 limit 1")
    ReservationOrder findById(Long id);

    @Select("select o.*, u.real_name as user_name, i.instrument_name "
        + "from biz_reservation_order o "
        + "left join sys_user u on u.id = o.user_id "
        + "left join biz_instrument i on i.id = o.instrument_id "
        + "where o.deleted = 0 order by o.create_time desc")
    List<ReservationOrder> findAll();

    @Select("select o.*, u.real_name as user_name, i.instrument_name "
        + "from biz_reservation_order o "
        + "left join sys_user u on u.id = o.user_id "
        + "left join biz_instrument i on i.id = o.instrument_id "
        + "where o.user_id = #{userId} and o.deleted = 0 order by o.create_time desc")
    List<ReservationOrder> findByUserId(Long userId);

    @Select("select count(1) from biz_reservation_order "
        + "where instrument_id = #{instrumentId} and order_type = 'MACHINE' and deleted = 0 "
        + "and order_status in ('PENDING_AUDIT', 'APPROVED', 'WAITING_USE', 'IN_USE') "
        + "and reserve_start < #{reserveEnd} and reserve_end > #{reserveStart}")
    int countMachineConflict(@Param("instrumentId") Long instrumentId, @Param("reserveEnd") LocalDateTime reserveEnd,
                             @Param("reserveStart") LocalDateTime reserveStart);

    @Select("select * from biz_reservation_order "
        + "where instrument_id = #{instrumentId} and order_type = 'MACHINE' and deleted = 0 "
        + "and order_status in ('PENDING_AUDIT', 'APPROVED', 'WAITING_USE', 'IN_USE') "
        + "and reserve_start < #{dayEnd} and reserve_end > #{dayStart} "
        + "order by reserve_start asc")
    List<ReservationOrder> findMachineReservedSlots(@Param("instrumentId") Long instrumentId,
                                                    @Param("dayStart") LocalDateTime dayStart,
                                                    @Param("dayEnd") LocalDateTime dayEnd);

    @Insert("insert into biz_reservation_order(order_no, order_type, user_id, instrument_id, department_id, owner_user_id, contact_name, contact_phone, purpose, project_name, reserve_start, reserve_end, reserve_minutes, order_status, audit_status, pay_status, settlement_status, estimated_amount, final_amount, source, submit_time, approve_time, finish_time, cancel_reason, remark, create_time, update_time, deleted) "
        + "values(#{orderNo}, #{orderType}, #{userId}, #{instrumentId}, #{departmentId}, #{ownerUserId}, #{contactName}, #{contactPhone}, #{purpose}, #{projectName}, #{reserveStart}, #{reserveEnd}, #{reserveMinutes}, #{orderStatus}, #{auditStatus}, #{payStatus}, #{settlementStatus}, #{estimatedAmount}, #{finalAmount}, #{source}, #{submitTime}, #{approveTime}, #{finishTime}, #{cancelReason}, #{remark}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReservationOrder order);

    @Update("update biz_reservation_order set order_status=#{orderStatus}, audit_status=#{auditStatus}, pay_status=#{payStatus}, settlement_status=#{settlementStatus}, final_amount=#{finalAmount}, approve_time=#{approveTime}, finish_time=#{finishTime}, cancel_reason=#{cancelReason}, remark=#{remark}, update_time=#{updateTime} where id=#{id}")
    int update(ReservationOrder order);
}
