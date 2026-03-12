package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.ResultFile;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResultFileRepository {

    List<ResultFile> findByOrderId(Long orderId);

    int insert(ResultFile resultFile);
}
