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
import com.ailink.module.worker.vo.WorkerApplyAttachmentVO;
import com.ailink.module.worker.vo.WorkerApplyVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkerApplyServiceImpl implements WorkerApplyService {

    private static final long ATTACHMENT_MAX_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of("doc", "docx", "jpg", "jpeg", "png");

    private final WorkerApplyMapper workerApplyMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final UserMapper userMapper;
    private final String attachmentDir;

    public WorkerApplyServiceImpl(WorkerApplyMapper workerApplyMapper,
                                  WorkerProfileMapper workerProfileMapper,
                                  UserMapper userMapper,
                                  @Value("${worker-apply.attachment.dir:uploads/worker-apply}") String attachmentDir) {
        this.workerApplyMapper = workerApplyMapper;
        this.workerProfileMapper = workerProfileMapper;
        this.userMapper = userMapper;
        this.attachmentDir = attachmentDir;
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
        apply.setRealName(resolveApplyRealName(latest, user));
        apply.setIdNoHash(resolveApplyIdNoHash(latest, userId));
        apply.setApplyNote(trimToNull(request.getApplyNote()));
        apply.setApplyAttachmentName(trimToNull(request.getApplyAttachmentName()));
        apply.setApplyAttachmentUrl(trimToNull(request.getApplyAttachmentUrl()));
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
    public WorkerApplyAttachmentVO uploadAttachment(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "file is required");
        }
        if (file.getSize() > ATTACHMENT_MAX_SIZE) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "file size cannot exceed 5MB");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().trim()
                : "attachment";
        String extension = getFileExtension(originalName);
        if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "unsupported file type");
        }
        String dateSegment = LocalDate.now().toString();
        String generatedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativePath = userId + "/" + dateSegment + "/" + generatedName;
        Path basePath = Paths.get(attachmentDir).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "invalid file path");
        }
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "upload failed");
        }
        return new WorkerApplyAttachmentVO(originalName, relativePath.replace("\\", "/"));
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
            profile.setRealName(resolveProfileRealName(apply, null));
            profile.setIdNoHash(resolveProfileIdNoHash(apply, null));
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
        profile.setRealName(resolveProfileRealName(apply, profile));
        profile.setIdNoHash(resolveProfileIdNoHash(apply, profile));
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

    private String resolveApplyRealName(WorkerApply latest, User user) {
        if (latest != null && StringUtils.hasText(latest.getRealName())) {
            return latest.getRealName().trim();
        }
        if (user != null && StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "worker-" + (user == null ? "" : user.getId());
    }

    private String resolveApplyIdNoHash(WorkerApply latest, Long userId) {
        if (latest != null && StringUtils.hasText(latest.getIdNoHash())) {
            return latest.getIdNoHash().trim();
        }
        return "AUTO-" + userId;
    }

    private String resolveProfileRealName(WorkerApply apply, WorkerProfile existingProfile) {
        if (apply != null && StringUtils.hasText(apply.getRealName())) {
            return apply.getRealName().trim();
        }
        if (existingProfile != null && StringUtils.hasText(existingProfile.getRealName())) {
            return existingProfile.getRealName().trim();
        }
        return "worker-" + (apply == null ? "" : apply.getUserId());
    }

    private String resolveProfileIdNoHash(WorkerApply apply, WorkerProfile existingProfile) {
        if (apply != null && StringUtils.hasText(apply.getIdNoHash())) {
            return apply.getIdNoHash().trim();
        }
        if (existingProfile != null && StringUtils.hasText(existingProfile.getIdNoHash())) {
            return existingProfile.getIdNoHash().trim();
        }
        return "AUTO-" + (apply == null ? "" : apply.getUserId());
    }

    private String getFileExtension(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        if (!StringUtils.hasText(ext)) {
            return "";
        }
        return ext.trim().toLowerCase(Locale.ROOT);
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
        vo.setApplyAttachmentName(row.getApplyAttachmentName());
        vo.setApplyAttachmentUrl(row.getApplyAttachmentUrl());
        vo.setReviewNote(row.getReviewNote());
        vo.setReviewedBy(row.getReviewedBy());
        vo.setReviewedTime(row.getReviewedTime());
        vo.setCreatedTime(row.getCreatedTime());
        vo.setUpdatedTime(row.getUpdatedTime());
        return vo;
    }
}
