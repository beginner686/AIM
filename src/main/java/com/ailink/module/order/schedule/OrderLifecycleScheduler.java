package com.ailink.module.order.schedule;

import com.ailink.common.enums.OrderStatus;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.user.entity.User;
import com.ailink.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderLifecycleScheduler.class);

    private final OrderMapper orderMapper;
    private final DemandMapper demandMapper;
    private final OrderService orderService;
    private final UserMapper userMapper;

    @Value("${order.auto.close-unpaid-hours:2}")
    private long closeUnpaidHours;

    @Value("${order.auto.complete-grace-days:3}")
    private long completeGraceDays;

    @Value("${order.auto.abuse-threshold-per-hour:8}")
    private int abuseThresholdPerHour;

    public OrderLifecycleScheduler(OrderMapper orderMapper,
                                   DemandMapper demandMapper,
                                   OrderService orderService,
                                   UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.demandMapper = demandMapper;
        this.orderService = orderService;
        this.userMapper = userMapper;
    }

    @Scheduled(fixedDelayString = "${order.auto.scan-delay-ms:120000}")
    @Transactional(rollbackFor = Exception.class)
    public void scanAndHealLifecycle() {
        autoCloseUnpaidOrders();
        autoCompleteOverdueOrders();
        blockAbusiveNoPayUsers();
    }

    private void autoCloseUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(closeUnpaidHours);
        List<Order> staleOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.SERVICE_FEE_REQUIRED.name())
                .le(Order::getCreatedTime, deadline)
                .orderByAsc(Order::getCreatedTime));
        for (Order order : staleOrders) {
            try {
                orderService.cancelByEmployer(order.getEmployerId(), order.getId(), "auto close: service fee timeout");
            } catch (Exception e) {
                log.warn("auto close unpaid order failed, orderId={}", order.getId(), e);
            }
        }
    }

    private void autoCompleteOverdueOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> inProgressOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.IN_PROGRESS.name())
                .orderByAsc(Order::getUpdatedTime));
        for (Order order : inProgressOrders) {
            Demand demand = demandMapper.selectById(order.getDemandId());
            if (demand == null || demand.getDeadline() == null) {
                continue;
            }
            LocalDateTime completeAt = demand.getDeadline().plusDays(completeGraceDays);
            if (now.isBefore(completeAt)) {
                continue;
            }
            try {
                orderService.confirmComplete(order.getEmployerId(), order.getId(), "auto completed after deadline grace period");
            } catch (Exception e) {
                log.warn("auto complete overdue order failed, orderId={}", order.getId(), e);
            }
        }
    }

    private void blockAbusiveNoPayUsers() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Order> recentUnpaidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.SERVICE_FEE_REQUIRED.name())
                .eq(Order::getPayStatus, "UNPAID")
                .ge(Order::getCreatedTime, oneHourAgo));
        if (recentUnpaidOrders.isEmpty()) {
            return;
        }
        Map<Long, Integer> counter = new HashMap<>();
        for (Order order : recentUnpaidOrders) {
            Long employerId = order.getEmployerId();
            if (employerId == null) {
                continue;
            }
            counter.put(employerId, counter.getOrDefault(employerId, 0) + 1);
        }
        for (Map.Entry<Long, Integer> entry : counter.entrySet()) {
            if (entry.getValue() < abuseThresholdPerHour) {
                continue;
            }
            User user = userMapper.selectById(entry.getKey());
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                continue;
            }
            User update = new User();
            update.setId(user.getId());
            update.setStatus(0);
            userMapper.updateById(update);
            log.warn("user blocked by abuse strategy, userId={}, unpaidCountLastHour={}", user.getId(), entry.getValue());
        }
    }
}
