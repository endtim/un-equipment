package com.unequipment.platform.modules.content.repository;

import com.unequipment.platform.modules.content.entity.HelpDoc;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * HelpDocRepository 数据访问接口。
 * 负责对应模块的持久化读写，不承载业务决策。
 */
public interface HelpDocRepository {

    List<HelpDoc> findAll();

    List<HelpDoc> findPage(@Param("keyword") String keyword,
                           @Param("publishStatus") String publishStatus,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    long countPage(@Param("keyword") String keyword, @Param("publishStatus") String publishStatus);

    HelpDoc findById(@Param("id") Long id);

    HelpDoc findPublishedById(@Param("id") Long id);

    int insert(HelpDoc helpDoc);

    int update(HelpDoc helpDoc);

    int softDelete(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);
}
