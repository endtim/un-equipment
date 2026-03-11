package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.UsageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UsageRecordRepository {

    @Select("select * from biz_usage_record where order_id = #{orderId} limit 1")
    UsageRecord findByOrderId(Long orderId);

    @Insert("insert into biz_usage_record(order_id, instrument_id, operator_user_id, checkin_time, start_time, end_time, actual_minutes, abnormal_flag, abnormal_desc, owner_confirm_user_id, owner_confirm_time, create_time, update_time) "
        + "values(#{orderId}, #{instrumentId}, #{operatorUserId}, #{checkinTime}, #{startTime}, #{endTime}, #{actualMinutes}, #{abnormalFlag}, #{abnormalDesc}, #{ownerConfirmUserId}, #{ownerConfirmTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UsageRecord usageRecord);

    @Update("update biz_usage_record set checkin_time=#{checkinTime}, start_time=#{startTime}, end_time=#{endTime}, actual_minutes=#{actualMinutes}, abnormal_flag=#{abnormalFlag}, abnormal_desc=#{abnormalDesc}, owner_confirm_user_id=#{ownerConfirmUserId}, owner_confirm_time=#{ownerConfirmTime}, update_time=#{updateTime} where id=#{id}")
    int update(UsageRecord usageRecord);

    @Select("select ifnull(sum(actual_minutes), 0) from biz_usage_record where instrument_id = #{instrumentId}")
    long sumActualMinutesByInstrumentId(Long instrumentId);
}
