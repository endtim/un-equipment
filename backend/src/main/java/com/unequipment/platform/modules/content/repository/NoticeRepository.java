package com.unequipment.platform.modules.content.repository;

import com.unequipment.platform.modules.content.entity.Notice;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NoticeRepository {

    @Select("select * from content_notice where deleted = 0 order by top_flag desc, publish_time desc, id desc")
    List<Notice> findAll();

    @Select("<script>"
        + "select * from content_notice where deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and title like concat('%', #{keyword}, '%') "
        + "</if>"
        + "<if test='publishStatus != null and publishStatus != \"\"'>"
        + "and publish_status = #{publishStatus} "
        + "</if>"
        + "order by top_flag desc, publish_time desc, id desc limit #{offset}, #{pageSize}"
        + "</script>")
    List<Notice> findPage(@Param("keyword") String keyword, @Param("publishStatus") String publishStatus,
                          @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("<script>"
        + "select count(1) from content_notice where deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and title like concat('%', #{keyword}, '%') "
        + "</if>"
        + "<if test='publishStatus != null and publishStatus != \"\"'>"
        + "and publish_status = #{publishStatus} "
        + "</if>"
        + "</script>")
    long countPage(@Param("keyword") String keyword, @Param("publishStatus") String publishStatus);

    @Select("select * from content_notice where id = #{id} and deleted = 0")
    Notice findById(Long id);

    @Insert("insert into content_notice(title, category, summary, content, cover_url, instrument_id, top_flag, publish_status, publish_time, view_count, create_time, update_time, deleted) "
        + "values(#{title}, #{category}, #{summary}, #{content}, #{coverUrl}, #{instrumentId}, #{topFlag}, #{publishStatus}, #{publishTime}, #{viewCount}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notice notice);

    @Update("update content_notice set title=#{title}, category=#{category}, summary=#{summary}, content=#{content}, cover_url=#{coverUrl}, instrument_id=#{instrumentId}, top_flag=#{topFlag}, publish_status=#{publishStatus}, publish_time=#{publishTime}, view_count=#{viewCount}, update_time=#{updateTime} where id=#{id}")
    int update(Notice notice);

    @Update("update content_notice set deleted = 1, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
