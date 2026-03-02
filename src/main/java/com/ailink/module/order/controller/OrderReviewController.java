package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.OrderReviewCreateRequest;
import com.ailink.module.order.service.OrderReviewService;
import com.ailink.module.order.vo.OrderReviewVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order/{orderId}/review")
public class OrderReviewController {

    private final OrderReviewService orderReviewService;

    public OrderReviewController(OrderReviewService orderReviewService) {
        this.orderReviewService = orderReviewService;
    }

    @PostMapping
    public Result<OrderReviewVO> create(@PathVariable Long orderId,
                                        @Valid @RequestBody OrderReviewCreateRequest request) {
        return Result.success(orderReviewService.createReview(
                SecurityContextUtil.currentUserId(),
                orderId,
                request.getScore(),
                request.getContent()));
    }

    @GetMapping
    public Result<List<OrderReviewVO>> list(@PathVariable Long orderId) {
        return Result.success(orderReviewService.listByOrder(SecurityContextUtil.currentUserId(), orderId));
    }
}
