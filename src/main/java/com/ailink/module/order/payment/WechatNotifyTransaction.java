package com.ailink.module.order.payment;

import lombok.Data;

@Data
public class WechatNotifyTransaction {

    private String outTradeNo;
    private String transactionId;
    private Integer totalFeeFen;
    private String tradeState;
    private String successTime;
}
