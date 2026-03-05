package com.ailink.module.order.mapper;

import com.ailink.module.order.entity.PaymentNotifyLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentNotifyLogMapper extends BaseMapper<PaymentNotifyLog> {

    @Select("SELECT * FROM payment_notify_log WHERE event_id = #{eventId} AND deleted = 0 LIMIT 1")
    PaymentNotifyLog selectByEventId(@Param("eventId") String eventId);
}
