package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.MaintenanceRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MaintenanceRecordRepository {

    @Select("select * from biz_maintenance_record where instrument_id = #{instrumentId} order by create_time desc")
    List<MaintenanceRecord> findByInstrumentId(Long instrumentId);

    @Insert("insert into biz_maintenance_record(instrument_id, maint_type, title, content, start_time, end_time, operator_user_id, status, create_time, update_time) "
        + "values(#{instrumentId}, #{maintType}, #{title}, #{content}, #{startTime}, #{endTime}, #{operatorUserId}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MaintenanceRecord maintenanceRecord);
}
