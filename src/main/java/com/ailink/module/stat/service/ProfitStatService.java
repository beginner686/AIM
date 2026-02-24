package com.ailink.module.stat.service;

import com.ailink.module.stat.vo.CategoryProfitStatVO;
import com.ailink.module.stat.vo.CountryProfitStatVO;
import com.ailink.module.stat.vo.ProfitSummaryVO;
import com.ailink.module.stat.vo.WorkerIncomeRankVO;

import java.util.List;

public interface ProfitStatService {

    ProfitSummaryVO summary();

    List<CountryProfitStatVO> byCountry();

    List<CategoryProfitStatVO> byCategory();

    List<WorkerIncomeRankVO> workerIncomeRank(Integer limit);
}
