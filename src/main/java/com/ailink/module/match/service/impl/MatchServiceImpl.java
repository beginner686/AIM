package com.ailink.module.match.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.match.service.MatchService;
import com.ailink.module.match.vo.MatchWorkerVO;
import com.ailink.module.worker.entity.WorkerProfile;
import com.ailink.module.worker.mapper.WorkerProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MatchServiceImpl implements MatchService {

    private final DemandMapper demandMapper;
    private final WorkerProfileMapper workerProfileMapper;

    public MatchServiceImpl(DemandMapper demandMapper, WorkerProfileMapper workerProfileMapper) {
        this.demandMapper = demandMapper;
        this.workerProfileMapper = workerProfileMapper;
    }

    @Override
    public List<MatchWorkerVO> matchWorkers(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "demand not found");
        }

        List<WorkerProfile> workers = workerProfileMapper.selectList(new LambdaQueryWrapper<WorkerProfile>()
                .eq(WorkerProfile::getVerified, 1));

        return workers.stream()
                .map(worker -> toMatchVO(worker, demand))
                .sorted(Comparator.comparing(MatchWorkerVO::getMatchScore).reversed())
                .limit(20)
                .toList();
    }

    private MatchWorkerVO toMatchVO(WorkerProfile worker, Demand demand) {
        BigDecimal score = new BigDecimal("60");

        if (StringUtils.hasText(worker.getCountry()) && worker.getCountry().equalsIgnoreCase(demand.getTargetCountry())) {
            score = score.add(new BigDecimal("10"));
        }

        if (StringUtils.hasText(worker.getSkillTags()) && StringUtils.hasText(demand.getCategory())) {
            String tags = worker.getSkillTags().toLowerCase(Locale.ROOT);
            String category = demand.getCategory().toLowerCase(Locale.ROOT);
            if (tags.contains(category)) {
                score = score.add(new BigDecimal("20"));
            }
        }

        Float rating = worker.getRating() == null ? 5.0f : worker.getRating();
        score = score.add(new BigDecimal(String.valueOf(rating)).multiply(new BigDecimal("2")));

        if (worker.getPriceMin() != null && worker.getPriceMax() != null && demand.getBudget() != null
                && demand.getBudget().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgPrice = worker.getPriceMin().add(worker.getPriceMax()).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal diffRatio = avgPrice.subtract(demand.getBudget()).abs().divide(demand.getBudget(), 4, RoundingMode.HALF_UP);
            BigDecimal penalty = diffRatio.multiply(new BigDecimal("20"));
            score = score.subtract(penalty);
        }

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(new BigDecimal("100")) > 0) {
            score = new BigDecimal("100");
        }

        MatchWorkerVO vo = new MatchWorkerVO();
        vo.setWorkerProfileId(worker.getId());
        vo.setWorkerUserId(worker.getUserId());
        vo.setCountry(worker.getCountry());
        vo.setCity(worker.getCity());
        vo.setSkillTags(worker.getSkillTags());
        vo.setRating(worker.getRating());
        vo.setVerified(worker.getVerified());
        vo.setPriceMin(worker.getPriceMin());
        vo.setPriceMax(worker.getPriceMax());
        vo.setMatchScore(score.setScale(2, RoundingMode.HALF_UP));
        return vo;
    }
}
