package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstrumentAttachmentRepository {

    @Select("select * from biz_instrument_attachment where deleted = 0 order by instrument_id asc, sort_no asc, id asc")
    List<InstrumentAttachment> findAll();

    @Select("select * from biz_instrument_attachment where instrument_id = #{instrumentId} and deleted = 0 order by sort_no asc, id asc")
    List<InstrumentAttachment> findByInstrumentId(Long instrumentId);

    @Select("select * from biz_instrument_attachment where id = #{id} and deleted = 0")
    InstrumentAttachment findById(Long id);

    @Insert("insert into biz_instrument_attachment(instrument_id, file_name, file_url, file_type, sort_no, create_time, deleted) "
        + "values(#{instrumentId}, #{fileName}, #{fileUrl}, #{fileType}, #{sortNo}, #{createTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InstrumentAttachment attachment);

    @Update("update biz_instrument_attachment set instrument_id=#{instrumentId}, file_name=#{fileName}, file_url=#{fileUrl}, file_type=#{fileType}, sort_no=#{sortNo} where id=#{id}")
    int update(InstrumentAttachment attachment);

    @Update("update biz_instrument_attachment set deleted = 1 where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
