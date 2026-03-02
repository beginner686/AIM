package com.ailink.module.order.mapper;

import com.ailink.module.order.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE id = #{id} AND deleted = 0 LIMIT 1 FOR UPDATE")
    Order selectByIdForUpdate(@Param("id") Long id);
}
