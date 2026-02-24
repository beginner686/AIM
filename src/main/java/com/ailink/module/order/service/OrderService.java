package com.ailink.module.order.service;

import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.vo.OrderVO;
import com.ailink.module.order.vo.OrderStatusLogVO;

import java.util.List;

public interface OrderService {

    Long createOrder(Long employerId, OrderCreateRequest request);

    void payEscrow(Long employerId, Long orderId, String remark);

    void startWork(Long workerUserId, Long orderId, String remark);

    void confirmComplete(Long employerId, Long orderId, String remark);

    void raiseDispute(Long userId, Long orderId, String reason);

    void closeOrderByAdmin(Long adminId, Long orderId, String reason);

    List<OrderVO> listMyOrders(Long userId);

    OrderVO getMyOrder(Long userId, Long orderId);

    List<OrderStatusLogVO> listMyOrderLogs(Long userId, Long orderId);
}
