package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.entity.OrderReview;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.mapper.OrderReviewMapper;
import com.ailink.module.order.service.OrderReviewService;
import com.ailink.module.order.vo.OrderReviewVO;
import com.ailink.module.user.entity.User;
import com.ailink.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class OrderReviewServiceImpl implements OrderReviewService {

    private final OrderMapper orderMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final UserMapper userMapper;

    public OrderReviewServiceImpl(OrderMapper orderMapper, OrderReviewMapper orderReviewMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.orderReviewMapper = orderReviewMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderReviewVO createReview(Long reviewerId, Long orderId, Integer score, String content) {
        Order order = findOrder(orderId);
        if (!reviewerId.equals(order.getEmployerId()) && !reviewerId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only order participants can review");
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "review is available only when order is completed");
        }

        OrderReview existing = orderReviewMapper.selectOne(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId)
                .eq(OrderReview::getReviewerId, reviewerId));
        if (existing != null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "you have already reviewed this order");
        }

        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(reviewerId.equals(order.getEmployerId()) ? order.getWorkerUserId() : order.getEmployerId());
        review.setScore(score);
        review.setContent(content);
        orderReviewMapper.insert(review);
        refreshReviewScore(review.getRevieweeId());
        return toVO(review);
    }

    @Override
    public List<OrderReviewVO> listByOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only order participants can view reviews");
        }
        return orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                        .eq(OrderReview::getOrderId, orderId)
                        .orderByAsc(OrderReview::getCreatedTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private Order findOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
        }
        return order;
    }

    private OrderReviewVO toVO(OrderReview review) {
        OrderReviewVO vo = new OrderReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setReviewerId(review.getReviewerId());
        vo.setRevieweeId(review.getRevieweeId());
        vo.setScore(review.getScore());
        vo.setContent(review.getContent());
        vo.setCreatedTime(review.getCreatedTime());
        return vo;
    }

    private void refreshReviewScore(Long userId) {
        if (userId == null) {
            return;
        }
        List<OrderReview> reviews = orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getRevieweeId, userId));
        if (reviews.isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderReview review : reviews) {
            total = total.add(BigDecimal.valueOf(review.getScore() == null ? 0 : review.getScore()));
        }
        BigDecimal avg = total.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
        User update = new User();
        update.setId(userId);
        update.setReviewScore(avg);
        userMapper.updateById(update);
    }
}
