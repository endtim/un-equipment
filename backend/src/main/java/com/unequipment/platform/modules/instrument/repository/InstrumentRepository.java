package com.unequipment.platform.modules.instrument.repository;

import com.unequipment.platform.modules.instrument.entity.Instrument;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstrumentRepository {

    @Select("select i.*, c.category_name, d.dept_name as department_name, u.real_name as owner_user_name "
        + "from biz_instrument i "
        + "left join biz_instrument_category c on c.id = i.category_id "
        + "left join sys_department d on d.id = i.department_id "
        + "left join sys_user u on u.id = i.owner_user_id "
        + "where i.deleted = 0 order by i.sort_no asc, i.id desc")
    List<Instrument> findAll();

    @Select("<script>"
        + "select i.*, c.category_name, d.dept_name as department_name, u.real_name as owner_user_name "
        + "from biz_instrument i "
        + "left join biz_instrument_category c on c.id = i.category_id "
        + "left join sys_department d on d.id = i.department_id "
        + "left join sys_user u on u.id = i.owner_user_id "
        + "where i.deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and (i.instrument_name like concat('%', #{keyword}, '%') or i.instrument_no like concat('%', #{keyword}, '%')) "
        + "</if>"
        + "<if test='status != null and status != \"\"'>"
        + "and i.status = #{status} "
        + "</if>"
        + "<if test='categoryId != null'>"
        + "and i.category_id = #{categoryId} "
        + "</if>"
        + "order by i.sort_no asc, i.id desc limit #{offset}, #{pageSize}"
        + "</script>")
    List<Instrument> findPageByCondition(@Param("keyword") String keyword, @Param("status") String status,
                                         @Param("categoryId") Long categoryId, @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("<script>"
        + "select count(1) "
        + "from biz_instrument i "
        + "where i.deleted = 0 "
        + "<if test='keyword != null and keyword != \"\"'>"
        + "and (i.instrument_name like concat('%', #{keyword}, '%') or i.instrument_no like concat('%', #{keyword}, '%')) "
        + "</if>"
        + "<if test='status != null and status != \"\"'>"
        + "and i.status = #{status} "
        + "</if>"
        + "<if test='categoryId != null'>"
        + "and i.category_id = #{categoryId} "
        + "</if>"
        + "</script>")
    long countPageByCondition(@Param("keyword") String keyword, @Param("status") String status,
                              @Param("categoryId") Long categoryId);

    @Select("select count(1) from biz_instrument where deleted = 0")
    long countAll();

    @Select("select i.*, c.category_name, d.dept_name as department_name, u.real_name as owner_user_name "
        + "from biz_instrument i "
        + "left join biz_instrument_category c on c.id = i.category_id "
        + "left join sys_department d on d.id = i.department_id "
        + "left join sys_user u on u.id = i.owner_user_id "
        + "where i.id = #{id} and i.deleted = 0 limit 1")
    Instrument findById(Long id);

    @Select("select i.*, c.category_name, d.dept_name as department_name, u.real_name as owner_user_name "
        + "from biz_instrument i "
        + "left join biz_instrument_category c on c.id = i.category_id "
        + "left join sys_department d on d.id = i.department_id "
        + "left join sys_user u on u.id = i.owner_user_id "
        + "where i.deleted = 0 order by i.sort_no asc, i.id desc limit #{offset}, #{pageSize}")
    List<Instrument> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Insert("insert into biz_instrument(instrument_no, instrument_name, model, brand, category_id, department_id, owner_user_id, location, status, open_mode, open_status, support_external, need_audit, require_training, booking_unit, price_internal, price_external, min_reserve_minutes, max_reserve_minutes, step_minutes, cover_url, intro, usage_desc, sample_desc, notice_text, is_hot, sort_no, create_time, update_time, deleted) "
        + "values(#{instrumentNo}, #{instrumentName}, #{model}, #{brand}, #{categoryId}, #{departmentId}, #{ownerUserId}, #{location}, #{status}, #{openMode}, #{openStatus}, #{supportExternal}, #{needAudit}, #{requireTraining}, #{bookingUnit}, #{priceInternal}, #{priceExternal}, #{minReserveMinutes}, #{maxReserveMinutes}, #{stepMinutes}, #{coverUrl}, #{intro}, #{usageDesc}, #{sampleDesc}, #{noticeText}, #{isHot}, #{sortNo}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Instrument instrument);

    @Update("update biz_instrument set instrument_no=#{instrumentNo}, instrument_name=#{instrumentName}, model=#{model}, brand=#{brand}, category_id=#{categoryId}, department_id=#{departmentId}, owner_user_id=#{ownerUserId}, location=#{location}, status=#{status}, open_mode=#{openMode}, open_status=#{openStatus}, support_external=#{supportExternal}, need_audit=#{needAudit}, require_training=#{requireTraining}, booking_unit=#{bookingUnit}, price_internal=#{priceInternal}, price_external=#{priceExternal}, min_reserve_minutes=#{minReserveMinutes}, max_reserve_minutes=#{maxReserveMinutes}, step_minutes=#{stepMinutes}, cover_url=#{coverUrl}, intro=#{intro}, usage_desc=#{usageDesc}, sample_desc=#{sampleDesc}, notice_text=#{noticeText}, is_hot=#{isHot}, sort_no=#{sortNo}, update_time=#{updateTime} where id=#{id}")
    int update(Instrument instrument);

    @Update("update biz_instrument set deleted = 1, update_time=#{updateTime} where id=#{id}")
    int softDelete(@Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
