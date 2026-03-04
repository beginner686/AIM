package com.ailink.module.order.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderBatchDeleteResultVO {

    private Integer requestedCount = 0;
    private Integer deletedCount = 0;
    private Integer blockedCount = 0;
    private List<Long> blockedIds = new ArrayList<>();
}
