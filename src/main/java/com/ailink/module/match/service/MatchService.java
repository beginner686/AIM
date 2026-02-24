package com.ailink.module.match.service;

import com.ailink.module.match.vo.MatchWorkerVO;

import java.util.List;

public interface MatchService {

    List<MatchWorkerVO> matchWorkers(Long demandId);
}
