package com.ailink.module.stat.service.impl;

import com.ailink.module.stat.mapper.ProfitStatMapper;
import com.ailink.module.stat.service.ProfitStatService;
import com.ailink.module.stat.vo.CategoryProfitStatVO;
import com.ailink.module.stat.vo.CountryProfitStatVO;
import com.ailink.module.stat.vo.ProfitSummaryVO;
import com.ailink.module.stat.vo.WorkerIncomeRankVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfitStatServiceImpl implements ProfitStatService {

    private final ProfitStatMapper profitStatMapper;

    public ProfitStatServiceImpl(ProfitStatMapper profitStatMapper) {
        this.profitStatMapper = profitStatMapper;
    }

    @Override
    public ProfitSummaryVO summary() {
        return profitStatMapper.summary();
    }

    @Override
    public List<CountryProfitStatVO> byCountry() {
        return profitStatMapper.byCountry();
    }

    @Override
    public List<CategoryProfitStatVO> byCategory() {
        return profitStatMapper.byCategory();
    }

    @Override
    public List<WorkerIncomeRankVO> workerIncomeRank(Integer limit) {
        int safeLimit = (limit == null || limit <= 0 || limit > 100) ? 10 : limit;
        return profitStatMapper.workerIncomeRank(safeLimit);
    }
}
