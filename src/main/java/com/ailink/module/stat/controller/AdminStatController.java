package com.ailink.module.stat.controller;

import com.ailink.common.Result;
import com.ailink.module.stat.service.ProfitStatService;
import com.ailink.module.stat.vo.CategoryProfitStatVO;
import com.ailink.module.stat.vo.CountryProfitStatVO;
import com.ailink.module.stat.vo.ProfitSummaryVO;
import com.ailink.module.stat.vo.WorkerIncomeRankVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stat")
public class AdminStatController {

    private final ProfitStatService profitStatService;

    public AdminStatController(ProfitStatService profitStatService) {
        this.profitStatService = profitStatService;
    }

    @GetMapping("/summary")
    public Result<ProfitSummaryVO> summary() {
        return Result.success(profitStatService.summary());
    }

    @GetMapping("/country")
    public Result<List<CountryProfitStatVO>> country() {
        return Result.success(profitStatService.byCountry());
    }

    @GetMapping("/category")
    public Result<List<CategoryProfitStatVO>> category() {
        return Result.success(profitStatService.byCategory());
    }

    @GetMapping("/worker-income-rank")
    public Result<List<WorkerIncomeRankVO>> workerIncomeRank(@RequestParam(required = false) Integer limit) {
        return Result.success(profitStatService.workerIncomeRank(limit));
    }
}
