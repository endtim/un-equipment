package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.MaintenanceRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * MaintenanceRecordRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface MaintenanceRecordRepository {

    List<MaintenanceRecord> findByInstrumentId(Long instrumentId);

    int insert(MaintenanceRecord maintenanceRecord);
}
