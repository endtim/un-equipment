package com.unequipment.platform.modules.order.repository;

import com.unequipment.platform.modules.order.entity.ResultFile;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResultFileRepository {

    @Select("select * from biz_result_file where order_id = #{orderId} and deleted = 0 order by create_time desc")
    List<ResultFile> findByOrderId(Long orderId);

    @Insert("insert into biz_result_file(order_id, file_name, file_url, file_type, upload_user_id, create_time, deleted) "
        + "values(#{orderId}, #{fileName}, #{fileUrl}, #{fileType}, #{uploadUserId}, #{createTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResultFile resultFile);
}
