package com.ailink.module.demand.service;

import com.ailink.module.demand.dto.DemandCreateRequest;
import com.ailink.module.demand.dto.DemandQueryRequest;
import com.ailink.module.demand.vo.DemandVO;

import java.util.List;

public interface DemandService {

    Long createDemand(Long userId, DemandCreateRequest request);

    List<DemandVO> listDemand(DemandQueryRequest request);

    List<DemandVO> listMyDemand(Long userId);

    DemandVO getById(Long demandId);
}
