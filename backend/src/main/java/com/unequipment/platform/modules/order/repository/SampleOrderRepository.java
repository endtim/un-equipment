package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.SampleOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SampleOrderRepository {

    @Select("select * from biz_sample_order where order_id = #{orderId} limit 1")
    SampleOrder findByOrderId(Long orderId);

    @Insert("insert into biz_sample_order(order_id, sample_name, sample_count, sample_type, sample_spec, test_requirement, danger_flag, danger_desc, receive_status, received_time, receiver_user_id, testing_status, result_summary, create_time, update_time) "
        + "values(#{orderId}, #{sampleName}, #{sampleCount}, #{sampleType}, #{sampleSpec}, #{testRequirement}, #{dangerFlag}, #{dangerDesc}, #{receiveStatus}, #{receivedTime}, #{receiverUserId}, #{testingStatus}, #{resultSummary}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SampleOrder sampleOrder);

    @Update("update biz_sample_order set sample_name=#{sampleName}, sample_count=#{sampleCount}, sample_type=#{sampleType}, sample_spec=#{sampleSpec}, test_requirement=#{testRequirement}, danger_flag=#{dangerFlag}, danger_desc=#{dangerDesc}, receive_status=#{receiveStatus}, received_time=#{receivedTime}, receiver_user_id=#{receiverUserId}, testing_status=#{testingStatus}, result_summary=#{resultSummary}, update_time=#{updateTime} where id=#{id}")
    int update(SampleOrder sampleOrder);

    @Select("select ifnull(sum(s.sample_count), 0) "
        + "from biz_sample_order s "
        + "join biz_reservation_order o on o.id = s.order_id "
        + "where o.instrument_id = #{instrumentId} and o.deleted = 0")
    long sumSampleCountByInstrumentId(Long instrumentId);
}
