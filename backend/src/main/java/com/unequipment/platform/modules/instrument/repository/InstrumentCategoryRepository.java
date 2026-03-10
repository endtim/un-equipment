package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstrumentCategoryRepository {

    @Select("select * from biz_instrument_category where deleted = 0 order by sort_no asc, id asc")
    List<InstrumentCategory> findAll();

    @Select("select * from biz_instrument_category where id = #{id} and deleted = 0")
    InstrumentCategory findById(Long id);

    @Insert("insert into biz_instrument_category(parent_id, category_name, category_code, sort_no, status, create_time, update_time, deleted) "
        + "values(#{parentId}, #{categoryName}, #{categoryCode}, #{sortNo}, #{status}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InstrumentCategory category);

    @Update("update biz_instrument_category set parent_id=#{parentId}, category_name=#{categoryName}, category_code=#{categoryCode}, sort_no=#{sortNo}, status=#{status}, update_time=#{updateTime} where id=#{id}")
    int update(InstrumentCategory category);

    @Update("update biz_instrument_category set deleted = 1, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
