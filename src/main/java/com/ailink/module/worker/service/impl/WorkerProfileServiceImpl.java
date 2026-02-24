package com.ailink.module.worker.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.worker.dto.WorkerProfileUpsertRequest;
import com.ailink.module.worker.dto.WorkerQueryRequest;
import com.ailink.module.worker.entity.WorkerProfile;
import com.ailink.module.worker.mapper.WorkerProfileMapper;
import com.ailink.module.worker.service.WorkerProfileService;
import com.ailink.module.worker.vo.WorkerProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WorkerProfileServiceImpl implements WorkerProfileService {

    private final WorkerProfileMapper workerProfileMapper;

    public WorkerProfileServiceImpl(WorkerProfileMapper workerProfileMapper) {
        this.workerProfileMapper = workerProfileMapper;
    }

    @Override
    public Long upsertProfile(Long userId, WorkerProfileUpsertRequest request) {
        WorkerProfile profile = workerProfileMapper.selectOne(new LambdaQueryWrapper<WorkerProfile>()
                .eq(WorkerProfile::getUserId, userId));

        if (profile == null) {
            profile = new WorkerProfile();
            profile.setUserId(userId);
            profile.setRating(5.0f);
            profile.setVerified(0);
            fillProfile(profile, request);
            workerProfileMapper.insert(profile);
        } else {
            fillProfile(profile, request);
            profile.setVerified(0);
            workerProfileMapper.updateById(profile);
        }
        return profile.getId();
    }

    @Override
    public WorkerProfileVO getByUserId(Long userId) {
        WorkerProfile profile = workerProfileMapper.selectOne(new LambdaQueryWrapper<WorkerProfile>()
                .eq(WorkerProfile::getUserId, userId));
        if (profile == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return toVO(profile);
    }

    @Override
    public List<WorkerProfileVO> list(WorkerQueryRequest request) {
        LambdaQueryWrapper<WorkerProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WorkerProfile::getCreatedTime);
        if (StringUtils.hasText(request.getCountry())) {
            wrapper.eq(WorkerProfile::getCountry, request.getCountry());
        }
        if (StringUtils.hasText(request.getCity())) {
            wrapper.eq(WorkerProfile::getCity, request.getCity());
        }
        if (StringUtils.hasText(request.getSkillKeyword())) {
            wrapper.like(WorkerProfile::getSkillTags, request.getSkillKeyword());
        }
        if (request.getVerified() != null) {
            wrapper.eq(WorkerProfile::getVerified, request.getVerified());
        }
        return workerProfileMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public void verifyByAdmin(Long profileId, Integer verified) {
        WorkerProfile profile = workerProfileMapper.selectById(profileId);
        if (profile == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        WorkerProfile update = new WorkerProfile();
        update.setId(profileId);
        update.setVerified(verified);
        workerProfileMapper.updateById(update);
    }

    private void fillProfile(WorkerProfile profile, WorkerProfileUpsertRequest request) {
        profile.setCountry(request.getCountry());
        profile.setCity(request.getCity());
        profile.setSkillTags(request.getSkillTags());
        profile.setPriceMin(request.getPriceMin());
        profile.setPriceMax(request.getPriceMax());
        profile.setExperience(request.getExperience());
        profile.setRealName(request.getRealName());
        profile.setIdNoHash(request.getIdNoHash());
    }

    private WorkerProfileVO toVO(WorkerProfile profile) {
        WorkerProfileVO vo = new WorkerProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setCountry(profile.getCountry());
        vo.setCity(profile.getCity());
        vo.setSkillTags(profile.getSkillTags());
        vo.setPriceMin(profile.getPriceMin());
        vo.setPriceMax(profile.getPriceMax());
        vo.setExperience(profile.getExperience());
        vo.setRating(profile.getRating());
        vo.setVerified(profile.getVerified());
        vo.setRealName(profile.getRealName());
        vo.setCreatedTime(profile.getCreatedTime());
        return vo;
    }
}
