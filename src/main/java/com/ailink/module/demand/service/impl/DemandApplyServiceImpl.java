package com.ailink.module.demand.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.DemandApplyStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.demand.dto.DemandApplySubmitRequest;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.entity.DemandApply;
import com.ailink.module.demand.mapper.DemandApplyMapper;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.demand.service.DemandApplyService;
import com.ailink.module.demand.vo.DemandApplyVO;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.worker.entity.WorkerProfile;
import com.ailink.module.worker.mapper.WorkerProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemandApplyServiceImpl implements DemandApplyService {

    private final DemandApplyMapper demandApplyMapper;
    private final DemandMapper demandMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final OrderService orderService;

    public DemandApplyServiceImpl(DemandApplyMapper demandApplyMapper,
                                  DemandMapper demandMapper,
                                  WorkerProfileMapper workerProfileMapper,
                                  OrderService orderService) {
        this.demandApplyMapper = demandApplyMapper;
        this.demandMapper = demandMapper;
        this.workerProfileMapper = workerProfileMapper;
        this.orderService = orderService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DemandApplyVO submit(Long workerUserId, Long demandId, DemandApplySubmitRequest request) {
        Demand demand = getDemandOrThrow(demandId);
        if (!"OPEN".equalsIgnoreCase(demand.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "需求非开放状态，无法申请");
        }
        if (workerUserId.equals(demand.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "不能申请自己发布的需求");
        }

        WorkerProfile workerProfile = getVerifiedWorkerProfileOrThrow(workerUserId);
        DemandApply latest = findLatestByDemandAndWorker(demandId, workerUserId);
        if (latest != null) {
            if (isStatus(latest, DemandApplyStatus.PENDING)) {
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "你已申请该需求，请等待处理");
            }
            if (isStatus(latest, DemandApplyStatus.ACCEPTED)) {
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "该需求已接受你的申请");
            }
            latest.setDemandOwnerId(demand.getUserId());
            latest.setWorkerProfileId(workerProfile.getId());
            latest.setQuoteAmount(request.getQuoteAmount());
            latest.setApplyNote(trimToNull(request.getApplyNote()));
            latest.setStatus(DemandApplyStatus.PENDING.name());
            latest.setReviewNote(null);
            latest.setOrderId(null);
            latest.setHandledTime(null);
            demandApplyMapper.updateById(latest);
            return toVO(latest, demand, workerProfile);
        }

        DemandApply row = new DemandApply();
        row.setDemandId(demandId);
        row.setDemandOwnerId(demand.getUserId());
        row.setWorkerUserId(workerUserId);
        row.setWorkerProfileId(workerProfile.getId());
        row.setQuoteAmount(request.getQuoteAmount());
        row.setApplyNote(trimToNull(request.getApplyNote()));
        row.setStatus(DemandApplyStatus.PENDING.name());
        demandApplyMapper.insert(row);
        return toVO(row, demand, workerProfile);
    }

    @Override
    public List<DemandApplyVO> listMyApplications(Long workerUserId) {
        return demandApplyMapper.selectList(new LambdaQueryWrapper<DemandApply>()
                        .eq(DemandApply::getWorkerUserId, workerUserId)
                        .orderByDesc(DemandApply::getCreatedTime)
                        .orderByDesc(DemandApply::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<DemandApplyVO> listByDemandOwner(Long demandOwnerId, Long demandId, String status) {
        Demand demand = getDemandOrThrow(demandId);
        if (!demandOwnerId.equals(demand.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "仅需求发布者可查看申请");
        }
        LambdaQueryWrapper<DemandApply> wrapper = new LambdaQueryWrapper<DemandApply>()
                .eq(DemandApply::getDemandId, demandId)
                .orderByDesc(DemandApply::getCreatedTime)
                .orderByDesc(DemandApply::getId);
        String normalizedStatus = normalizeStatus(status);
        if (StringUtils.hasText(normalizedStatus)) {
            wrapper.eq(DemandApply::getStatus, normalizedStatus);
        }
        return demandApplyMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long acceptByDemandOwner(Long demandOwnerId, Long applyId, BigDecimal amount, String reviewNote) {
        DemandApply row = getApplyOrThrow(applyId);
        Demand demand = getDemandOrThrow(row.getDemandId());
        if (!demandOwnerId.equals(demand.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "仅需求发布者可处理申请");
        }
        if (!isStatus(row, DemandApplyStatus.PENDING)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "该申请不是待处理状态");
        }
        if (!"OPEN".equalsIgnoreCase(demand.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "需求当前不可继续选人，请检查需求状态");
        }

        BigDecimal finalAmount = amount;
        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            finalAmount = row.getQuoteAmount();
        }
        if ((finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) && demand.getBudget() != null) {
            finalAmount = demand.getBudget();
        }
        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "订单金额无效");
        }

        OrderCreateRequest createRequest = new OrderCreateRequest();
        createRequest.setDemandId(row.getDemandId());
        createRequest.setWorkerProfileId(row.getWorkerProfileId());
        createRequest.setAmount(finalAmount);
        createRequest.setPaymentChannel("WECHAT_PAY");
        Long orderId = orderService.createOrder(demandOwnerId, createRequest);

        row.setStatus(DemandApplyStatus.ACCEPTED.name());
        row.setReviewNote(trimToNull(reviewNote));
        row.setOrderId(orderId);
        row.setHandledTime(LocalDateTime.now());
        demandApplyMapper.updateById(row);

        demandApplyMapper.update(null, new LambdaUpdateWrapper<DemandApply>()
                .eq(DemandApply::getDemandId, row.getDemandId())
                .eq(DemandApply::getStatus, DemandApplyStatus.PENDING.name())
                .ne(DemandApply::getId, row.getId())
                .set(DemandApply::getStatus, DemandApplyStatus.REJECTED.name())
                .set(DemandApply::getReviewNote, "需求方已选择其他执行者")
                .set(DemandApply::getHandledTime, LocalDateTime.now()));
        return orderId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByDemandOwner(Long demandOwnerId, Long applyId, String reviewNote) {
        DemandApply row = getApplyOrThrow(applyId);
        Demand demand = getDemandOrThrow(row.getDemandId());
        if (!demandOwnerId.equals(demand.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "仅需求发布者可处理申请");
        }
        if (!isStatus(row, DemandApplyStatus.PENDING)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "该申请不是待处理状态");
        }
        row.setStatus(DemandApplyStatus.REJECTED.name());
        row.setReviewNote(trimToNull(reviewNote));
        row.setHandledTime(LocalDateTime.now());
        demandApplyMapper.updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByWorker(Long workerUserId, Long applyId) {
        DemandApply row = getApplyOrThrow(applyId);
        if (!workerUserId.equals(row.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "仅申请人可取消申请");
        }
        if (!isStatus(row, DemandApplyStatus.PENDING)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "该申请当前不可取消");
        }
        row.setStatus(DemandApplyStatus.CANCELED.name());
        row.setHandledTime(LocalDateTime.now());
        demandApplyMapper.updateById(row);
    }

    private DemandApply findLatestByDemandAndWorker(Long demandId, Long workerUserId) {
        return demandApplyMapper.selectOne(new LambdaQueryWrapper<DemandApply>()
                .eq(DemandApply::getDemandId, demandId)
                .eq(DemandApply::getWorkerUserId, workerUserId)
                .orderByDesc(DemandApply::getUpdatedTime)
                .orderByDesc(DemandApply::getId)
                .last("limit 1"));
    }

    private DemandApply getApplyOrThrow(Long applyId) {
        DemandApply row = demandApplyMapper.selectById(applyId);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "申请记录不存在");
        }
        return row;
    }

    private Demand getDemandOrThrow(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "需求不存在");
        }
        return demand;
    }

    private WorkerProfile getVerifiedWorkerProfileOrThrow(Long workerUserId) {
        WorkerProfile profile = workerProfileMapper.selectOne(new LambdaQueryWrapper<WorkerProfile>()
                .eq(WorkerProfile::getUserId, workerUserId)
                .last("limit 1"));
        if (profile == null || profile.getVerified() == null || profile.getVerified() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "仅已认证执行者可申请需求");
        }
        return profile;
    }

    private DemandApplyVO toVO(DemandApply row) {
        Demand demand = demandMapper.selectById(row.getDemandId());
        WorkerProfile workerProfile = workerProfileMapper.selectById(row.getWorkerProfileId());
        return toVO(row, demand, workerProfile);
    }

    private DemandApplyVO toVO(DemandApply row, Demand demand, WorkerProfile workerProfile) {
        DemandApplyVO vo = new DemandApplyVO();
        vo.setId(row.getId());
        vo.setDemandId(row.getDemandId());
        vo.setDemandOwnerId(row.getDemandOwnerId());
        vo.setWorkerUserId(row.getWorkerUserId());
        vo.setWorkerProfileId(row.getWorkerProfileId());
        vo.setQuoteAmount(row.getQuoteAmount());
        vo.setApplyNote(row.getApplyNote());
        vo.setStatus(row.getStatus());
        vo.setReviewNote(row.getReviewNote());
        vo.setOrderId(row.getOrderId());
        vo.setHandledTime(row.getHandledTime());
        vo.setCreatedTime(row.getCreatedTime());
        vo.setUpdatedTime(row.getUpdatedTime());

        if (workerProfile != null) {
            vo.setWorkerName(workerProfile.getRealName());
            vo.setWorkerCountry(workerProfile.getCountry());
            vo.setWorkerSkillTags(workerProfile.getSkillTags());
        }
        if (demand != null) {
            vo.setDemandCategory(demand.getCategory());
            vo.setDemandCountry(demand.getTargetCountry());
            vo.setDemandBudget(demand.getBudget());
            vo.setDemandStatus(demand.getStatus());
        }
        return vo;
    }

    private boolean isStatus(DemandApply row, DemandApplyStatus status) {
        return status.name().equalsIgnoreCase(row.getStatus());
    }

    private String normalizeStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return "";
        }
        String status = rawStatus.trim().toUpperCase();
        try {
            DemandApplyStatus.valueOf(status);
            return status;
        } catch (Exception ignore) {
            return "";
        }
    }

    private String trimToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}

