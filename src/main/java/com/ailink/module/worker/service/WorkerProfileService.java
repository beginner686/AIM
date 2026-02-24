package com.ailink.module.worker.service;

import com.ailink.module.worker.dto.WorkerProfileUpsertRequest;
import com.ailink.module.worker.dto.WorkerQueryRequest;
import com.ailink.module.worker.vo.WorkerProfileVO;

import java.util.List;

public interface WorkerProfileService {

    Long upsertProfile(Long userId, WorkerProfileUpsertRequest request);

    WorkerProfileVO getByUserId(Long userId);

    List<WorkerProfileVO> list(WorkerQueryRequest request);

    void verifyByAdmin(Long profileId, Integer verified);
}
