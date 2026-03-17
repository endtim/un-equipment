package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.ResultFile;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * ResultFileRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface ResultFileRepository {

    List<ResultFile> findByOrderId(Long orderId);

    int insert(ResultFile resultFile);
}
