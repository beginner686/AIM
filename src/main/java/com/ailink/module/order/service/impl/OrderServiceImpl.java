package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.escrow.service.EscrowService;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.entity.OrderStatusLog;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.mapper.OrderStatusLogMapper;
import com.ailink.module.order.payment.PaymentGatewayFactory;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.service.PlatformConfigService;
import com.ailink.module.order.vo.OrderVO;
import com.ailink.module.order.vo.OrderStatusLogVO;
import com.ailink.module.worker.entity.WorkerProfile;
import com.ailink.module.worker.mapper.WorkerProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final DemandMapper demandMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final EscrowService escrowService;
    private final PlatformConfigService platformConfigService;
    private final PaymentGatewayFactory paymentGatewayFactory;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderStatusLogMapper orderStatusLogMapper,
                            DemandMapper demandMapper,
                            WorkerProfileMapper workerProfileMapper,
                            EscrowService escrowService,
                            PlatformConfigService platformConfigService,
                            PaymentGatewayFactory paymentGatewayFactory) {
        this.orderMapper = orderMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.demandMapper = demandMapper;
        this.workerProfileMapper = workerProfileMapper;
        this.escrowService = escrowService;
        this.platformConfigService = platformConfigService;
        this.paymentGatewayFactory = paymentGatewayFactory;
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

        BigDecimal platformFeeRate = platformConfigService.getDecimal(
                PlatformConfigService.PLATFORM_FEE_RATE_KEY,
                new BigDecimal("0.06"));
        BigDecimal escrowFeeRate = platformConfigService.getDecimal(
                PlatformConfigService.ESCROW_FEE_RATE_KEY,
                new BigDecimal("0.005"));

        BigDecimal platformFee = request.getAmount().multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal escrowFee = request.getAmount().multiply(escrowFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal workerIncome = request.getAmount().subtract(platformFee).subtract(escrowFee).setScale(2, RoundingMode.HALF_UP);
        if (workerIncome.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "worker income cannot be negative");
        }

        Order order = new Order();
        order.setDemandId(request.getDemandId());
        order.setEmployerId(employerId);
        order.setWorkerProfileId(workerProfile.getId());
        order.setWorkerUserId(workerProfile.getUserId());
        order.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.CREATED.name());
        order.setPayStatus("UNPAID");
        order.setPaymentChannel(StringUtils.hasText(request.getPaymentChannel()) ? request.getPaymentChannel() : "MANUAL");
        order.setPlatformFeeRate(platformFeeRate);
        order.setPlatformFee(platformFee);
        order.setEscrowFeeRate(escrowFeeRate);
        order.setEscrowFee(escrowFee);
        order.setWorkerIncome(workerIncome);
        order.setRiskStatus("NORMAL");
        orderMapper.insert(order);

        demand.setStatus("MATCHED");
        demandMapper.updateById(demand);

        saveStatusLog(order.getId(), null, OrderStatus.CREATED.name(), employerId, "order created");
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payEscrow(Long employerId, Long orderId, String remark) {
        Order order = findById(orderId);
        if (!employerId.equals(order.getEmployerId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only employer can pay escrow");
        }
        transit(order, OrderStatus.ESCROWED, employerId, StringUtils.hasText(remark) ? remark : "escrow paid");
        order.setPayStatus("PAID");
        orderMapper.updateById(order);
        paymentGatewayFactory.getOrDefault(order.getPaymentChannel()).hold("ORDER-" + order.getId(), order.getAmount());
        escrowService.hold(order.getId(), order.getAmount(), order.getPaymentChannel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWork(Long workerUserId, Long orderId, String remark) {
        Order order = findById(orderId);
        if (!workerUserId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only worker can start work");
        }
        transit(order, OrderStatus.IN_PROGRESS, workerUserId, StringUtils.hasText(remark) ? remark : "work started");
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmComplete(Long employerId, Long orderId, String remark) {
        Order order = findById(orderId);
        if (!employerId.equals(order.getEmployerId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only employer can confirm complete");
        }
        transit(order, OrderStatus.COMPLETED, employerId, StringUtils.hasText(remark) ? remark : "order completed");
        orderMapper.updateById(order);
        escrowService.release(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void raiseDispute(Long userId, Long orderId, String reason) {
        Order order = findById(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only related users can raise dispute");
        }
        transit(order, OrderStatus.DISPUTED, userId, "dispute raised");
        order.setRiskStatus("ABNORMAL");
        order.setDisputeReason(reason);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderByAdmin(Long adminId, Long orderId, String reason) {
        Order order = findById(orderId);
        String statusBeforeClose = order.getStatus();
        transit(order, OrderStatus.CLOSED, adminId, "force closed by admin");
        order.setRiskStatus("ABNORMAL");
        order.setClosedReason(reason);
        orderMapper.updateById(order);
        if ("PAID".equals(order.getPayStatus()) && !OrderStatus.COMPLETED.name().equals(statusBeforeClose)) {
            try {
                escrowService.refund(orderId);
            } catch (Exception ignored) {
            }
        }
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

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setDemandId(order.getDemandId());
        vo.setEmployerId(order.getEmployerId());
        vo.setWorkerProfileId(order.getWorkerProfileId());
        vo.setWorkerUserId(order.getWorkerUserId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
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
