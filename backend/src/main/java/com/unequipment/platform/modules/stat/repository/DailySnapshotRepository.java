package com.unequipment.platform.modules.stat.repository;

import com.unequipment.platform.modules.stat.entity.DailySnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DailySnapshotRepository {

    @Select("select * from stat_daily_snapshot order by stat_date desc")
    List<DailySnapshot> findAll();
}
