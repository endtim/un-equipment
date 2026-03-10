package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.RechargeOrder;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RechargeOrderRepository {

    @Select("select r.*, u.real_name as user_name from biz_recharge_order r left join sys_user u on u.id = r.user_id order by r.create_time desc")
    List<RechargeOrder> findAll();

    @Select("select r.*, u.real_name as user_name from biz_recharge_order r left join sys_user u on u.id = r.user_id where r.user_id = #{userId} order by r.create_time desc")
    List<RechargeOrder> findByUserId(Long userId);

    @Select("select r.*, u.real_name as user_name from biz_recharge_order r left join sys_user u on u.id = r.user_id where r.id = #{id} limit 1")
    RechargeOrder findById(Long id);

    @Insert("insert into biz_recharge_order(recharge_no, user_id, amount, pay_method, voucher_url, status, remark, audit_user_id, audit_time, create_time, update_time) "
        + "values(#{rechargeNo}, #{userId}, #{amount}, #{payMethod}, #{voucherUrl}, #{status}, #{remark}, #{auditUserId}, #{auditTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RechargeOrder rechargeOrder);

    @Update("update biz_recharge_order set status=#{status}, remark=#{remark}, audit_user_id=#{auditUserId}, audit_time=#{auditTime}, update_time=#{updateTime} where id=#{id}")
    int update(RechargeOrder rechargeOrder);
}
