package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SettlementRecordRepository {

    @Select("select * from biz_settlement_record order by create_time desc")
    List<SettlementRecord> findAll();

    @Insert("insert into biz_settlement_record(settlement_no, order_id, user_id, instrument_id, bill_type, price_desc, estimated_amount, discount_amount, final_amount, settle_status, settled_time, operator_user_id, create_time) "
        + "values(#{settlementNo}, #{orderId}, #{userId}, #{instrumentId}, #{billType}, #{priceDesc}, #{estimatedAmount}, #{discountAmount}, #{finalAmount}, #{settleStatus}, #{settledTime}, #{operatorUserId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SettlementRecord settlementRecord);
}
