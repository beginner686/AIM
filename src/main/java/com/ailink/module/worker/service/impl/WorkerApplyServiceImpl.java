package com.ailink.module.worker.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.WorkerApplyStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.user.entity.User;
import com.ailink.module.user.mapper.UserMapper;
import com.ailink.module.worker.dto.WorkerApplyQueryRequest;
import com.ailink.module.worker.dto.WorkerApplySubmitRequest;
import com.ailink.module.worker.entity.WorkerApply;
import com.ailink.module.worker.entity.WorkerProfile;
import com.ailink.module.worker.mapper.WorkerApplyMapper;
import com.ailink.module.worker.mapper.WorkerProfileMapper;
import com.ailink.module.worker.service.WorkerApplyService;
import com.ailink.module.worker.vo.WorkerApplyVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
public class WorkerApplyServiceImpl implements WorkerApplyService {

    private final WorkerApplyMapper workerApplyMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final UserMapper userMapper;

    public WorkerApplyServiceImpl(WorkerApplyMapper workerApplyMapper,
                                  WorkerProfileMapper workerProfileMapper,
                                  UserMapper userMapper) {
        this.workerApplyMapper = workerApplyMapper;
        this.workerProfileMapper = workerProfileMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkerApplyVO submit(Long userId, String role, WorkerApplySubmitRequest request) {
        if ("WORKER".equalsIgnoreCase(role)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "already worker");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        WorkerApply latest = findLatestByUserId(userId);
        if (latest != null && WorkerApplyStatus.APPROVED.name().equalsIgnoreCase(latest.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "worker apply already approved");
        }
        if (request.getPriceMin() != null && request.getPriceMax() != null
                && request.getPriceMax().compareTo(request.getPriceMin()) < 0) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "priceMax cannot be less than priceMin");
        }

        WorkerApply apply = latest == null ? new WorkerApply() : latest;
        apply.setUserId(userId);
        apply.setStatus(WorkerApplyStatus.PENDING.name());
        apply.setCountry(request.getCountry());
        apply.setCity(request.getCity());
        apply.setSkillTags(request.getSkillTags());
        apply.setPriceMin(request.getPriceMin() == null ? BigDecimal.ZERO : request.getPriceMin());
        apply.setPriceMax(request.getPriceMax() == null ? BigDecimal.ZERO : request.getPriceMax());
        apply.setExperience(request.getExperience());
        apply.setRealName(request.getRealName());
        apply.setIdNoHash(request.getIdNoHash());
        apply.setApplyNote(request.getApplyNote());
        apply.setReviewNote(null);
        apply.setReviewedBy(null);
        apply.setReviewedTime(null);

        if (apply.getId() == null) {
            workerApplyMapper.insert(apply);
        } else {
            workerApplyMapper.updateById(apply);
        }

        updateUserApplyStatus(userId, WorkerApplyStatus.PENDING.name(), null);
        upsertWorkerProfileFromApply(apply, 0);
        return toVO(apply);
    }

    @Override
    public WorkerApplyVO getMyLatest(Long userId) {
        WorkerApply latest = findLatestByUserId(userId);
        return latest == null ? null : toVO(latest);
    }

    @Override
    public List<WorkerApplyVO> listByAdmin(WorkerApplyQueryRequest request) {
        LambdaQueryWrapper<WorkerApply> wrapper = new LambdaQueryWrapper<WorkerApply>()
                .orderByDesc(WorkerApply::getCreatedTime)
                .orderByDesc(WorkerApply::getId);
        String status = normalizeStatus(request == null ? null : request.getStatus());
        if (StringUtils.hasText(status) && !WorkerApplyStatus.NONE.name().equals(status)) {
            wrapper.eq(WorkerApply::getStatus, status);
        }
        return workerApplyMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long adminId, Long applyId, String reviewNote) {
        WorkerApply apply = findByIdForReview(applyId);
        apply.setStatus(WorkerApplyStatus.APPROVED.name());
        apply.setReviewNote(trimToNull(reviewNote));
        apply.setReviewedBy(adminId);
        apply.setReviewedTime(LocalDateTime.now());
        workerApplyMapper.updateById(apply);

        updateUserApplyStatus(apply.getUserId(), WorkerApplyStatus.APPROVED.name(), "WORKER");
        upsertWorkerProfileFromApply(apply, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long adminId, Long applyId, String reviewNote) {
        WorkerApply apply = findByIdForReview(applyId);
        apply.setStatus(WorkerApplyStatus.REJECTED.name());
        apply.setReviewNote(trimToNull(reviewNote));
        apply.setReviewedBy(adminId);
        apply.setReviewedTime(LocalDateTime.now());
        workerApplyMapper.updateById(apply);

        updateUserApplyStatus(apply.getUserId(), WorkerApplyStatus.REJECTED.name(), null);
        upsertWorkerProfileFromApply(apply, 0);
    }

    private WorkerApply findByIdForReview(Long applyId) {
        WorkerApply apply = workerApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "worker apply not found");
        }
        if (!WorkerApplyStatus.PENDING.name().equalsIgnoreCase(apply.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "worker apply is not pending");
        }
        return apply;
    }

    private WorkerApply findLatestByUserId(Long userId) {
        return workerApplyMapper.selectOne(new LambdaQueryWrapper<WorkerApply>()
                .eq(WorkerApply::getUserId, userId)
                .orderByDesc(WorkerApply::getCreatedTime)
                .orderByDesc(WorkerApply::getId)
                .last("limit 1"));
    }

    private void updateUserApplyStatus(Long userId, String applyStatus, String role) {
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "user not found");
        }
        User update = new User();
        update.setId(userId);
        update.setWorkerApplyStatus(applyStatus);
        if (StringUtils.hasText(role)) {
            update.setRole(role);
        } else if ("WORKER".equalsIgnoreCase(existing.getRole()) && !WorkerApplyStatus.APPROVED.name().equalsIgnoreCase(applyStatus)) {
            update.setWorkerApplyStatus(WorkerApplyStatus.APPROVED.name());
        }
        userMapper.updateById(update);
    }

    private void upsertWorkerProfileFromApply(WorkerApply apply, Integer verified) {
        WorkerProfile profile = workerProfileMapper.selectOne(new LambdaQueryWrapper<WorkerProfile>()
                .eq(WorkerProfile::getUserId, apply.getUserId()));
        if (profile == null) {
            profile = new WorkerProfile();
            profile.setUserId(apply.getUserId());
            profile.setRating(5.0f);
            profile.setCountry(apply.getCountry());
            profile.setCity(apply.getCity());
            profile.setSkillTags(apply.getSkillTags());
            profile.setPriceMin(apply.getPriceMin());
            profile.setPriceMax(apply.getPriceMax());
            profile.setExperience(apply.getExperience());
            profile.setRealName(apply.getRealName());
            profile.setIdNoHash(apply.getIdNoHash());
            profile.setVerified(verified);
            workerProfileMapper.insert(profile);
            return;
        }
        profile.setCountry(apply.getCountry());
        profile.setCity(apply.getCity());
        profile.setSkillTags(apply.getSkillTags());
        profile.setPriceMin(apply.getPriceMin());
        profile.setPriceMax(apply.getPriceMax());
        profile.setExperience(apply.getExperience());
        profile.setRealName(apply.getRealName());
        profile.setIdNoHash(apply.getIdNoHash());
        profile.setVerified(verified);
        workerProfileMapper.updateById(profile);
    }

    private String normalizeStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return "";
        }
        String status = rawStatus.trim().toUpperCase();
        try {
            WorkerApplyStatus.valueOf(status);
            return status;
        } catch (Exception e) {
            return "";
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private WorkerApplyVO toVO(WorkerApply row) {
        WorkerApplyVO vo = new WorkerApplyVO();
        vo.setId(row.getId());
        vo.setUserId(row.getUserId());
        vo.setStatus(row.getStatus());
        vo.setCountry(row.getCountry());
        vo.setCity(row.getCity());
        vo.setSkillTags(row.getSkillTags());
        vo.setPriceMin(row.getPriceMin());
        vo.setPriceMax(row.getPriceMax());
        vo.setExperience(row.getExperience());
        vo.setRealName(row.getRealName());
        vo.setIdNoHash(row.getIdNoHash());
        vo.setApplyNote(row.getApplyNote());
        vo.setReviewNote(row.getReviewNote());
        vo.setReviewedBy(row.getReviewedBy());
        vo.setReviewedTime(row.getReviewedTime());
        vo.setCreatedTime(row.getCreatedTime());
        vo.setUpdatedTime(row.getUpdatedTime());
        return vo;
    }
}
