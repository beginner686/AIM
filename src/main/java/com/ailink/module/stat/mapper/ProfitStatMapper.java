package com.ailink.module.stat.mapper;

import com.ailink.module.stat.vo.CategoryProfitStatVO;
import com.ailink.module.stat.vo.CountryProfitStatVO;
import com.ailink.module.stat.vo.ProfitSummaryVO;
import com.ailink.module.stat.vo.WorkerIncomeRankVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProfitStatMapper {

    @Select("""
            SELECT
              COALESCE(SUM(o.amount), 0) AS totalAmount,
              COALESCE(SUM(o.platform_fee), 0) AS totalPlatformFee,
              COALESCE(SUM(o.escrow_fee), 0) AS totalEscrowFee,
              COUNT(1) AS orderCount
            FROM orders o
            WHERE o.status IN ('COMPLETED', 'CLOSED')
            """)
    ProfitSummaryVO summary();

    @Select("""
            SELECT
              d.target_country AS country,
              COALESCE(SUM(o.amount), 0) AS totalAmount,
              COALESCE(SUM(o.platform_fee), 0) AS totalPlatformFee,
              COUNT(1) AS orderCount
            FROM orders o
            JOIN demand d ON o.demand_id = d.id
            WHERE o.status IN ('COMPLETED', 'CLOSED')
            GROUP BY d.target_country
            ORDER BY totalAmount DESC
            """)
    List<CountryProfitStatVO> byCountry();

    @Select("""
            SELECT
              d.category AS category,
              COALESCE(SUM(o.amount), 0) AS totalAmount,
              COALESCE(SUM(o.platform_fee), 0) AS totalPlatformFee,
              COUNT(1) AS orderCount
            FROM orders o
            JOIN demand d ON o.demand_id = d.id
            WHERE o.status IN ('COMPLETED', 'CLOSED')
            GROUP BY d.category
            ORDER BY totalAmount DESC
            """)
    List<CategoryProfitStatVO> byCategory();

    @Select("""
            SELECT
              o.worker_user_id AS workerUserId,
              COALESCE(SUM(o.worker_income), 0) AS totalIncome,
              COUNT(1) AS orderCount
            FROM orders o
            WHERE o.status IN ('COMPLETED', 'CLOSED')
            GROUP BY o.worker_user_id
            ORDER BY totalIncome DESC
            LIMIT #{limit}
            """)
    List<WorkerIncomeRankVO> workerIncomeRank(@Param("limit") Integer limit);
}
