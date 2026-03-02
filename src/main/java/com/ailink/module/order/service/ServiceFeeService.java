package com.ailink.module.order.service;

import com.ailink.module.order.vo.ServiceFeePayVO;

public interface ServiceFeeService {

    ServiceFeePayVO payServiceFee(Long userId, String role, Long orderId, String paymentChannel, String remark);

    String handleWechatNotify(String signature, String timestamp, String nonce, String body);
}
