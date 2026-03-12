package com.unequipment.platform.modules.stat.repository;

import com.unequipment.platform.modules.stat.entity.DailySnapshot;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailySnapshotRepository {

    List<DailySnapshot> findAll();

    List<DailySnapshot> findLatestLimit(@Param("limit") int limit);

    List<DailySnapshot> findByDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
