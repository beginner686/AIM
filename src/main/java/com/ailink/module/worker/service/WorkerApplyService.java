package com.ailink.module.worker.service;

import com.ailink.module.worker.dto.WorkerApplyQueryRequest;
import com.ailink.module.worker.dto.WorkerApplySubmitRequest;
import com.ailink.module.worker.vo.WorkerApplyVO;

import java.util.List;

public interface WorkerApplyService {

    WorkerApplyVO submit(Long userId, String role, WorkerApplySubmitRequest request);

    WorkerApplyVO getMyLatest(Long userId);

    List<WorkerApplyVO> listByAdmin(WorkerApplyQueryRequest request);

    void approve(Long adminId, Long applyId, String reviewNote);

    void reject(Long adminId, Long applyId, String reviewNote);
}
