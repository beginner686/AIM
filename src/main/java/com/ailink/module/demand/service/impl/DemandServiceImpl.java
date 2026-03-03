package com.ailink.module.demand.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.demand.dto.DemandCreateRequest;
import com.ailink.module.demand.dto.DemandQueryRequest;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.demand.service.DemandService;
import com.ailink.module.demand.vo.DemandVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DemandServiceImpl implements DemandService {

    private final DemandMapper demandMapper;

    public DemandServiceImpl(DemandMapper demandMapper) {
        this.demandMapper = demandMapper;
    }

    @Override
    public Long createDemand(Long userId, DemandCreateRequest request) {
        Demand demand = new Demand();
        demand.setUserId(userId);
        demand.setTargetCountry(request.getTargetCountry());
        demand.setCategory(request.getCategory());
        demand.setBudget(request.getBudget());
        demand.setDeadline(request.getDeadline());
        demand.setDescription(request.getDescription());
        demand.setStatus("OPEN");
        demandMapper.insert(demand);
        return demand.getId();
    }

    @Override
    public List<DemandVO> listDemand(DemandQueryRequest request) {
        LambdaQueryWrapper<Demand> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Demand::getCreatedTime);
        if (StringUtils.hasText(request.getTargetCountry())) {
            wrapper.eq(Demand::getTargetCountry, request.getTargetCountry());
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Demand::getCategory, request.getCategory());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Demand::getStatus, request.getStatus());
        }
        return demandMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<DemandVO> listMyDemand(Long userId) {
        return demandMapper.selectList(new LambdaQueryWrapper<Demand>()
                        .eq(Demand::getUserId, userId)
                        .orderByDesc(Demand::getCreatedTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public DemandVO getById(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return toVO(demand);
    }

    @Override
    public void cancelMyDemand(Long userId, Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!userId.equals(demand.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only demand owner can cancel demand");
        }
        if ("CLOSED".equalsIgnoreCase(demand.getStatus())) {
            return;
        }
        if (!"OPEN".equalsIgnoreCase(demand.getStatus())) {
            String status = StringUtils.hasText(demand.getStatus()) ? demand.getStatus().toUpperCase() : "UNKNOWN";
            String guidance = "demand cannot be canceled in current status: " + status;
            if ("MATCHED".equals(status) || "IN_PROGRESS".equals(status)) {
                guidance = "demand status is " + status + ", please cancel related orders first";
            } else if ("DONE".equals(status)) {
                guidance = "demand status is DONE, completed demand cannot be canceled";
            }
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(),
                    guidance);
        }

        Demand update = new Demand();
        update.setId(demandId);
        update.setStatus("CLOSED");
        demandMapper.updateById(update);
    }

    private DemandVO toVO(Demand demand) {
        DemandVO vo = new DemandVO();
        vo.setId(demand.getId());
        vo.setUserId(demand.getUserId());
        vo.setTargetCountry(demand.getTargetCountry());
        vo.setCategory(demand.getCategory());
        vo.setBudget(demand.getBudget());
        vo.setDeadline(demand.getDeadline());
        vo.setDescription(demand.getDescription());
        vo.setAiStructured(demand.getAiStructured());
        vo.setStatus(demand.getStatus());
        vo.setCreatedTime(demand.getCreatedTime());
        return vo;
    }
}
