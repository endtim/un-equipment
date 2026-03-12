package com.unequipment.platform.modules.content.repository;

import com.unequipment.platform.modules.content.entity.Notice;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeRepository {

    List<Notice> findAll();

    List<Notice> findPage(@Param("keyword") String keyword,
                          @Param("publishStatus") String publishStatus,
                          @Param("offset") int offset,
                          @Param("pageSize") int pageSize);

    long countPage(@Param("keyword") String keyword, @Param("publishStatus") String publishStatus);

    Notice findById(@Param("id") Long id);

    int insert(Notice notice);

    int update(Notice notice);

    int softDelete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
