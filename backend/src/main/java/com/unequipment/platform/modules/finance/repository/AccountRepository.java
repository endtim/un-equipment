package com.unequipment.platform.modules.finance.repository;

import com.unequipment.platform.modules.finance.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * AccountRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface AccountRepository {

    Account findByUserId(@Param("userId") Long userId);

    int insert(Account account);

    int update(Account account);

    int increaseBalanceForRecharge(@Param("accountId") Long accountId,
                                   @Param("amount") BigDecimal amount,
                                   @Param("updateTime") LocalDateTime updateTime);

    int decreaseBalanceForConsume(@Param("accountId") Long accountId,
                                  @Param("amount") BigDecimal amount,
                                  @Param("updateTime") LocalDateTime updateTime);

    int increaseBalanceForRefund(@Param("accountId") Long accountId,
                                 @Param("amount") BigDecimal amount,
                                 @Param("updateTime") LocalDateTime updateTime);

    int freezeAmountIfAvailable(@Param("accountId") Long accountId,
                                @Param("amount") BigDecimal amount,
                                @Param("updateTime") LocalDateTime updateTime);

    int unfreezeAmount(@Param("accountId") Long accountId,
                       @Param("amount") BigDecimal amount,
                       @Param("updateTime") LocalDateTime updateTime);

    int consumeWithFreeze(@Param("accountId") Long accountId,
                          @Param("finalAmount") BigDecimal finalAmount,
                          @Param("frozenAmount") BigDecimal frozenAmount,
                          @Param("updateTime") LocalDateTime updateTime);
}
