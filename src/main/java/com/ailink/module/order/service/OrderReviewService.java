package com.ailink.module.order.service;

import com.ailink.module.order.vo.OrderReviewVO;

import java.util.List;

public interface OrderReviewService {

    OrderReviewVO createReview(Long reviewerId, Long orderId, Integer score, String content);

    List<OrderReviewVO> listByOrder(Long userId, Long orderId);
}
