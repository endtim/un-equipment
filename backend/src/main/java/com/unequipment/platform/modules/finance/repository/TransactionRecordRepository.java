package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.TransactionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TransactionRecordRepository {

    @Select("select * from biz_transaction_record where user_id = #{userId} order by create_time desc")
    List<TransactionRecord> findByUserId(Long userId);

    @Insert("insert into biz_transaction_record(txn_no, user_id, order_id, recharge_id, txn_type, inout_type, amount, balance_before, balance_after, remark, create_time) "
        + "values(#{txnNo}, #{userId}, #{orderId}, #{rechargeId}, #{txnType}, #{inoutType}, #{amount}, #{balanceBefore}, #{balanceAfter}, #{remark}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TransactionRecord transactionRecord);
}
