package com.unequipment.platform.modules.content.repository;

import com.unequipment.platform.modules.content.entity.HelpDoc;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HelpDocRepository {

    @Select("select * from content_help_doc where deleted = 0 order by sort_no asc, publish_time desc, id desc")
    List<HelpDoc> findAll();

    @Select("<script>"
        + "select * from content_help_doc where deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and title like concat('%', #{keyword}, '%') "
        + "</if>"
        + "<if test='publishStatus != null and publishStatus != \"\"'>"
        + "and publish_status = #{publishStatus} "
        + "</if>"
        + "order by sort_no asc, publish_time desc, id desc limit #{offset}, #{pageSize}"
        + "</script>")
    List<HelpDoc> findPage(@Param("keyword") String keyword, @Param("publishStatus") String publishStatus,
                           @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("<script>"
        + "select count(1) from content_help_doc where deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and title like concat('%', #{keyword}, '%') "
        + "</if>"
        + "<if test='publishStatus != null and publishStatus != \"\"'>"
        + "and publish_status = #{publishStatus} "
        + "</if>"
        + "</script>")
    long countPage(@Param("keyword") String keyword, @Param("publishStatus") String publishStatus);

    @Select("select * from content_help_doc where id = #{id} and deleted = 0")
    HelpDoc findById(Long id);

    @Insert("insert into content_help_doc(title, doc_type, summary, content, file_url, sort_no, publish_status, publish_time, create_time, update_time, deleted) "
        + "values(#{title}, #{docType}, #{summary}, #{content}, #{fileUrl}, #{sortNo}, #{publishStatus}, #{publishTime}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HelpDoc helpDoc);

    @Update("update content_help_doc set title=#{title}, doc_type=#{docType}, summary=#{summary}, content=#{content}, file_url=#{fileUrl}, sort_no=#{sortNo}, publish_status=#{publishStatus}, publish_time=#{publishTime}, update_time=#{updateTime} where id=#{id}")
    int update(HelpDoc helpDoc);

    @Update("update content_help_doc set deleted = 1, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
