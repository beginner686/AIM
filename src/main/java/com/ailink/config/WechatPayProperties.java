package com.ailink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    private boolean enabled = false;
    private boolean mockAutoSuccess = false;
    private String baseUrl = "https://api.mch.weixin.qq.com";
    private String appId;
    private String mchId;
    private String merchantSerialNo;
    private String platformSerialNo;
    private String privateKeyPath;
    private String platformPublicKeyPath;
    private String apiV3Key;
    private String notifyUrl;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private int maxRetryTimes = 2;
    private long retryBackoffMs = 200;
}
