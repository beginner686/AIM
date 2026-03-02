package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.enums.ServiceFeeStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.entity.OrderStatusLog;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.mapper.OrderStatusLogMapper;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.service.PlatformConfigService;
import com.ailink.module.order.vo.OrderDetailVO;
import com.ailink.module.order.vo.OrderStatusLogVO;
import com.ailink.module.order.vo.OrderVO;
import com.ailink.module.user.entity.User;
import com.ailink.module.user.mapper.UserMapper;
import com.ailink.module.worker.entity.WorkerProfile;
import com.ailink.module.worker.mapper.WorkerProfileMapper;
import com.ailink.module.worker.service.RunnerPaymentProfileService;
import com.ailink.module.worker.vo.RunnerPaymentProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final BigDecimal DEFAULT_PLATFORM_FEE_RATE = new BigDecimal("0.06");
    private static final BigDecimal DEFAULT_SERVICE_FEE_RATE = new BigDecimal("0.10");

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final DemandMapper demandMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final UserMapper userMapper;
    private final PlatformConfigService platformConfigService;
    private final RunnerPaymentProfileService runnerPaymentProfileService;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderStatusLogMapper orderStatusLogMapper,
                            DemandMapper demandMapper,
                            WorkerProfileMapper workerProfileMapper,
                            UserMapper userMapper,
                            PlatformConfigService platformConfigService,
                            RunnerPaymentProfileService runnerPaymentProfileService) {
        this.orderMapper = orderMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.demandMapper = demandMapper;
        this.workerProfileMapper = workerProfileMapper;
        this.userMapper = userMapper;
        this.platformConfigService = platformConfigService;
        this.runnerPaymentProfileService = runnerPaymentProfileService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long employerId, OrderCreateRequest request) {
        Demand demand = demandMapper.selectById(request.getDemandId());
        if (demand == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "demand not found");
        }
        if (!employerId.equals(demand.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only demand owner can create order");
        }

        WorkerProfile workerProfile = workerProfileMapper.selectById(request.getWorkerProfileId());
        if (workerProfile == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "worker profile not found");
        }
        if (workerProfile.getVerified() == null || workerProfile.getVerified() != 1) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "worker is not verified");
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFeeRate = platformConfigService.getDecimal(
                PlatformConfigService.PLATFORM_FEE_RATE_KEY,
                DEFAULT_PLATFORM_FEE_RATE);
        BigDecimal serviceFeeRate = platformConfigService.getDecimal(
                PlatformConfigService.SERVICE_FEE_RATE_KEY,
                DEFAULT_SERVICE_FEE_RATE);

        BigDecimal platformFee = amount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal workerIncome = amount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFeeAmount = amount.multiply(serviceFeeRate).setScale(2, RoundingMode.HALF_UP);
        if (workerIncome.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "worker income cannot be negative");
        }

        Order order = new Order();
        order.setOrderNo(buildOrderNo());
        order.setDemandId(request.getDemandId());
        order.setEmployerId(employerId);
        order.setWorkerProfileId(workerProfile.getId());
        order.setWorkerUserId(workerProfile.getUserId());
        order.setAmount(amount);
        order.setStatus(OrderStatus.CREATED.name());
        order.setServiceFeeStatus(ServiceFeeStatus.REQUIRED.name());
        order.setServiceFeeAmount(serviceFeeAmount);
        order.setServiceFeePaidTime(null);
        order.setPayStatus("UNPAID");
        order.setPaymentChannel(StringUtils.hasText(request.getPaymentChannel())
                ? request.getPaymentChannel().trim().toUpperCase()
                : "WECHAT_PAY");
        order.setPlatformFeeRate(platformFeeRate);
        order.setPlatformFee(platformFee);
        order.setEscrowFeeRate(BigDecimal.ZERO);
        order.setEscrowFee(BigDecimal.ZERO);
        order.setWorkerIncome(workerIncome);
        order.setRiskStatus("NORMAL");
        orderMapper.insert(order);

        saveStatusLog(order.getId(), null, OrderStatus.CREATED.name(), employerId, "order created");
        updateOrderStatus(employerId, order.getId(), OrderStatus.SERVICE_FEE_REQUIRED, "service fee required");

        demand.setStatus("MATCHED");
        demandMapper.updateById(demand);
        log.info("order created, orderId={}, orderNo={}, demandId={}, employerId={}, workerUserId={}",
                order.getId(), order.getOrderNo(), order.getDemandId(), employerId, order.getWorkerUserId());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long operatorId, Long orderId, OrderStatus target, String remark) {
        Order order = findById(orderId);
        transit(order, target, operatorId, remark);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWork(Long workerUserId, Long orderId, String remark) {
        Order order = findById(orderId);
        if (!workerUserId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only runner can start work");
        }
        if (OrderStatus.SERVICE_FEE_PAID.name().equals(order.getStatus())) {
            updateOrderStatus(workerUserId, orderId, OrderStatus.MATCH_UNLOCKED, "match info unlocked");
            order = findById(orderId);
        }
        if (!OrderStatus.MATCH_UNLOCKED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "work can only be started when status is MATCH_UNLOCKED");
        }
        updateOrderStatus(workerUserId, orderId, OrderStatus.IN_PROGRESS, StringUtils.hasText(remark) ? remark : "work started");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmComplete(Long employerId, Long orderId, String remark) {
        Order order = findById(orderId);
        if (!employerId.equals(order.getEmployerId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only client can confirm complete");
        }
        if (!OrderStatus.IN_PROGRESS.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "order can be completed only when status is IN_PROGRESS");
        }
        updateOrderStatus(employerId, orderId, OrderStatus.COMPLETED, StringUtils.hasText(remark) ? remark : "order completed");

        Order update = new Order();
        update.setId(orderId);
        update.setPayStatus("RELEASED");
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void raiseDispute(Long userId, Long orderId, String reason, String evidenceUrl) {
        Order order = findById(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only participants can raise dispute");
        }
        if (!OrderStatus.MATCH_UNLOCKED.name().equals(order.getStatus())
                && !OrderStatus.IN_PROGRESS.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "dispute can only be raised after match unlock");
        }
        updateOrderStatus(userId, orderId, OrderStatus.DISPUTE, "dispute raised");

        Order update = new Order();
        update.setId(orderId);
        update.setRiskStatus("ABNORMAL");
        update.setDisputeReason(StringUtils.hasText(evidenceUrl) ? reason + " | evidence: " + evidenceUrl : reason);
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderByAdmin(Long adminId, Long orderId, String reason) {
        Order order = findById(orderId);
        if (!OrderStatus.CLOSED.name().equals(order.getStatus())) {
            updateOrderStatus(adminId, orderId, OrderStatus.CLOSED, "force closed by admin");
        }

        Order update = new Order();
        update.setId(orderId);
        update.setRiskStatus("ABNORMAL");
        update.setClosedReason(reason);
        if ("PAID".equals(order.getPayStatus()) && !OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            update.setPayStatus("REFUNDED");
        }
        orderMapper.updateById(update);
    }

    @Override
    public List<OrderVO> listMyOrders(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                        .and(wrapper -> wrapper.eq(Order::getEmployerId, userId).or().eq(Order::getWorkerUserId, userId))
                        .orderByDesc(Order::getCreatedTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public OrderVO getMyOrder(Long userId, Long orderId) {
        Order order = findById(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "no permission on this order");
        }
        return toVO(order);
    }

    @Override
    public OrderDetailVO getMyOrderDetail(Long userId, Long orderId) {
        Order order = findById(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "no permission on this order");
        }

        boolean showContact = ServiceFeeStatus.PAID.name().equals(order.getServiceFeeStatus());
        OrderStatus currentStatus = parseStatus(order.getStatus());

        WorkerProfile workerProfile = workerProfileMapper.selectById(order.getWorkerProfileId());
        User workerUser = userMapper.selectById(order.getWorkerUserId());
        RunnerPaymentProfileVO paymentProfile = showContact
                ? runnerPaymentProfileService.getByUserId(order.getWorkerUserId())
                : null;

        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setOrder(toVO(order));
        detailVO.setShowContact(showContact);
        detailVO.setShowChat(showContact ? currentStatus.canChat() : null);
        detailVO.setRunnerDisplayName(workerProfile == null ? null : workerProfile.getRealName());
        detailVO.setRunnerContact(showContact && workerUser != null ? workerUser.getEmail() : null);
        detailVO.setRunnerPaymentProfile(showContact ? paymentProfile : null);
        return detailVO;
    }

    @Override
    public List<OrderStatusLogVO> listMyOrderLogs(Long userId, Long orderId) {
        Order order = findById(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "no permission on this order");
        }
        return orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getCreatedTime))
                .stream()
                .map(this::toLogVO)
                .toList();
    }

    private Order findById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
        }
        return order;
    }

    private void transit(Order order, OrderStatus target, Long operatorId, String remark) {
        OrderStatus current = parseStatus(order.getStatus());
        if (!current.canTransitTo(target)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(),
                    "illegal order status transition: " + current + " -> " + target);
        }
        String fromStatus = order.getStatus();
        order.setStatus(target.name());
        saveStatusLog(order.getId(), fromStatus, target.name(), operatorId, remark);
        log.info("order status changed, orderId={}, from={}, to={}, operatorId={}, remark={}",
                order.getId(), fromStatus, target.name(), operatorId, remark);
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "unknown order status: " + status);
        }
    }

    private void saveStatusLog(Long orderId, String fromStatus, String toStatus, Long operatorId, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        orderStatusLogMapper.insert(log);
    }

    private String buildOrderNo() {
        long ts = System.currentTimeMillis();
        int suffix = (int) (Math.random() * 1000);
        return "ORD" + ts + String.format("%03d", suffix);
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setDemandId(order.getDemandId());
        vo.setEmployerId(order.getEmployerId());
        vo.setWorkerProfileId(order.getWorkerProfileId());
        vo.setWorkerUserId(order.getWorkerUserId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setServiceFeeStatus(order.getServiceFeeStatus());
        vo.setServiceFeeAmount(order.getServiceFeeAmount());
        vo.setServiceFeePaidTime(order.getServiceFeePaidTime());
        vo.setPayStatus(order.getPayStatus());
        vo.setPaymentChannel(order.getPaymentChannel());
        vo.setPlatformFeeRate(order.getPlatformFeeRate());
        vo.setPlatformFee(order.getPlatformFee());
        vo.setEscrowFeeRate(order.getEscrowFeeRate());
        vo.setEscrowFee(order.getEscrowFee());
        vo.setWorkerIncome(order.getWorkerIncome());
        vo.setRiskStatus(order.getRiskStatus());
        vo.setDisputeReason(order.getDisputeReason());
        vo.setClosedReason(order.getClosedReason());
        vo.setCreatedTime(order.getCreatedTime());
        vo.setUpdatedTime(order.getUpdatedTime());
        return vo;
    }

    private OrderStatusLogVO toLogVO(OrderStatusLog log) {
        OrderStatusLogVO vo = new OrderStatusLogVO();
        vo.setId(log.getId());
        vo.setOrderId(log.getOrderId());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setOperatorId(log.getOperatorId());
        vo.setRemark(log.getRemark());
        vo.setCreatedTime(log.getCreatedTime());
        return vo;
    }
}
