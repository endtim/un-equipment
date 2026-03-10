package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountRepository {

    @Select("select * from biz_account where user_id = #{userId} limit 1")
    Account findByUserId(Long userId);

    @Insert("insert into biz_account(user_id, balance, frozen_amount, total_recharge, total_consume, status, create_time, update_time) "
        + "values(#{userId}, #{balance}, #{frozenAmount}, #{totalRecharge}, #{totalConsume}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account account);

    @Update("update biz_account set balance=#{balance}, frozen_amount=#{frozenAmount}, total_recharge=#{totalRecharge}, total_consume=#{totalConsume}, status=#{status}, update_time=#{updateTime} where id=#{id}")
    int update(Account account);
}
