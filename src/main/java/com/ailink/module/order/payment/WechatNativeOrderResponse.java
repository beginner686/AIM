package com.ailink.module.order.payment;

import lombok.Data;

@Data
public class WechatNativeOrderResponse {

    private String codeUrl;
    private String prepayId;
}
