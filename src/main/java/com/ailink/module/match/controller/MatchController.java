package com.ailink.module.match.controller;

import com.ailink.common.Result;
import com.ailink.module.match.service.MatchService;
import com.ailink.module.match.vo.MatchWorkerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/demand/{demandId}")
    public Result<List<MatchWorkerVO>> matchByDemand(@PathVariable Long demandId) {
        return Result.success(matchService.matchWorkers(demandId));
    }
}
