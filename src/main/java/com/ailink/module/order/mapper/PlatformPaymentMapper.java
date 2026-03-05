package com.ailink.module.order.mapper;

import com.ailink.module.order.entity.PlatformPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlatformPaymentMapper extends BaseMapper<PlatformPayment> {

    @Select("SELECT * FROM platform_payment WHERE payment_no = #{paymentNo} AND deleted = 0 LIMIT 1")
    PlatformPayment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT * FROM platform_payment " +
            "WHERE order_id = #{orderId} AND status = 'SUCCESS' AND deleted = 0 " +
            "ORDER BY id DESC LIMIT 1 FOR UPDATE")
    PlatformPayment selectSuccessByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Select("SELECT * FROM platform_payment " +
            "WHERE order_id = #{orderId} AND status = 'UNPAID' AND deleted = 0 " +
            "ORDER BY id DESC LIMIT 1 FOR UPDATE")
    PlatformPayment selectUnpaidByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Select("SELECT * FROM platform_payment WHERE payment_no = #{paymentNo} AND deleted = 0 LIMIT 1 FOR UPDATE")
    PlatformPayment selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);
}
