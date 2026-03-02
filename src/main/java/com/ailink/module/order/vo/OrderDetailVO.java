package com.ailink.module.order.vo;

import com.ailink.module.worker.vo.RunnerPaymentProfileVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailVO {

    private OrderVO order;
    private Boolean showContact;
    private Boolean showChat;
    private String runnerContact;
    private String runnerDisplayName;
    private RunnerPaymentProfileVO runnerPaymentProfile;
}
