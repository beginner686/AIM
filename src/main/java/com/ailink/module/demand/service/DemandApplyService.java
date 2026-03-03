package com.ailink.module.demand.service;

import com.ailink.module.demand.dto.DemandApplySubmitRequest;
import com.ailink.module.demand.vo.DemandApplyVO;

import java.math.BigDecimal;
import java.util.List;

public interface DemandApplyService {

    DemandApplyVO submit(Long workerUserId, Long demandId, DemandApplySubmitRequest request);

    List<DemandApplyVO> listMyApplications(Long workerUserId);

    List<DemandApplyVO> listByDemandOwner(Long demandOwnerId, Long demandId, String status);

    Long acceptByDemandOwner(Long demandOwnerId, Long applyId, BigDecimal amount, String reviewNote);

    void rejectByDemandOwner(Long demandOwnerId, Long applyId, String reviewNote);

    void cancelByWorker(Long workerUserId, Long applyId);
}

