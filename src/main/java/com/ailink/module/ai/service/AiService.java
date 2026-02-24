package com.ailink.module.ai.service;

import com.ailink.module.ai.dto.AiDemandStructRequest;
import com.ailink.module.ai.vo.AiDemandStructVO;

public interface AiService {

    AiDemandStructVO structureDemand(AiDemandStructRequest request);
}
