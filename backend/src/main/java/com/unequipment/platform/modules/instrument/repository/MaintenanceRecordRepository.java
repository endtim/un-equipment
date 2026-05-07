package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.MaintenanceRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * MaintenanceRecordRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface MaintenanceRecordRepository {

    List<MaintenanceRecord> findByInstrumentId(Long instrumentId);

    List<MaintenanceRecord> findPageByScope(@Param("instrumentId") Long instrumentId,
                                            @Param("status") String status,
                                            @Param("roleCode") String roleCode,
                                            @Param("operatorId") Long operatorId,
                                            @Param("departmentId") Long departmentId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    long countPageByScope(@Param("instrumentId") Long instrumentId,
                          @Param("status") String status,
                          @Param("roleCode") String roleCode,
                          @Param("operatorId") Long operatorId,
                          @Param("departmentId") Long departmentId);

    MaintenanceRecord findById(Long id);

    int insert(MaintenanceRecord maintenanceRecord);

    int update(MaintenanceRecord maintenanceRecord);

    int deleteById(Long id);
}
