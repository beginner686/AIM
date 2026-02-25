<template>
  <div class="admin-dashboard">
    <!-- 顶部标题 -->
    <section class="page-header">
      <h1 class="page-title">📊 管理后台</h1>
      <p class="page-sub">平台数据一览 · 收益统计 · 费率配置</p>
    </section>

    <!-- 收益概览卡片 -->
    <section class="summary-row">
      <div class="summary-card" style="--accent:#6366f1">
        <span class="sum-label">总成交额</span>
        <span class="sum-value">¥{{ fmt(summary.totalAmount) }}</span>
      </div>
      <div class="summary-card" style="--accent:#10b981">
        <span class="sum-label">平台抽成</span>
        <span class="sum-value">¥{{ fmt(summary.totalPlatformFee) }}</span>
      </div>
      <div class="summary-card" style="--accent:#f59e0b">
        <span class="sum-label">托管手续费</span>
        <span class="sum-value">¥{{ fmt(summary.totalEscrowFee) }}</span>
      </div>
      <div class="summary-card" style="--accent:#0ea5e9">
        <span class="sum-label">总订单数</span>
        <span class="sum-value">{{ summary.orderCount ?? '—' }}</span>
      </div>
    </section>

    <!-- Tab 区域 -->
    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 国家维度 -->
      <el-tab-pane label="按国家统计" name="country">
        <div v-if="loading" class="tab-loading"><el-skeleton :rows="3" animated /></div>
        <el-empty v-else-if="countryStats.length === 0" description="暂无数据" />
        <el-table v-else :data="countryStats" stripe>
          <el-table-column prop="country" label="国家" min-width="120" />
          <el-table-column label="成交额" min-width="120">
            <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="平台抽成" min-width="120">
            <template #default="{ row }">¥{{ fmt(row.totalPlatformFee) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" min-width="80" />
        </el-table>
      </el-tab-pane>

      <!-- 类目维度 -->
      <el-tab-pane label="按类目统计" name="category">
        <div v-if="loading" class="tab-loading"><el-skeleton :rows="3" animated /></div>
        <el-empty v-else-if="categoryStats.length === 0" description="暂无数据" />
        <el-table v-else :data="categoryStats" stripe>
          <el-table-column prop="category" label="类目" min-width="120" />
          <el-table-column label="成交额" min-width="120">
            <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="平台抽成" min-width="120">
            <template #default="{ row }">¥{{ fmt(row.totalPlatformFee) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" min-width="80" />
        </el-table>
      </el-tab-pane>

      <!-- 执行者排行 -->
      <el-tab-pane label="执行者收入排行" name="rank">
        <div v-if="loading" class="tab-loading"><el-skeleton :rows="3" animated /></div>
        <el-empty v-else-if="workerRank.length === 0" description="暂无数据" />
        <el-table v-else :data="workerRank" stripe>
          <el-table-column label="排名" width="70">
            <template #default="{ $index }">
              <span :class="['rank-badge', $index < 3 ? 'top' : '']">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="workerUserId" label="执行者 ID" min-width="100" />
          <el-table-column label="总收入" min-width="120">
            <template #default="{ row }">¥{{ fmt(row.totalIncome) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="完成订单" min-width="80" />
        </el-table>
      </el-tab-pane>

      <!-- 费率配置 -->
      <el-tab-pane label="费率配置" name="fees">
        <div class="fee-section">
          <el-form :model="feeForm" label-width="120px" label-position="left" class="fee-form">
            <el-form-item label="平台抽成比例">
              <el-input-number
                v-model="feeForm.platformFeeRate"
                :min="0" :max="1" :step="0.01" :precision="4"
                controls-position="right"
              />
              <span class="fee-hint">当前 {{ (feeForm.platformFeeRate * 100).toFixed(2) }}%</span>
            </el-form-item>
            <el-form-item label="托管手续费率">
              <el-input-number
                v-model="feeForm.escrowFeeRate"
                :min="0" :max="1" :step="0.001" :precision="4"
                controls-position="right"
              />
              <span class="fee-hint">当前 {{ (feeForm.escrowFeeRate * 100).toFixed(2) }}%</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingFee" @click="handleSaveFee">保存配置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import {
  getStatSummaryApi,
  getStatByCountryApi,
  getStatByCategoryApi,
  getWorkerIncomeRankApi,
  getFeeConfigApi,
  updateFeeConfigApi,
} from '@/api/admin';
import { ElMessage } from 'element-plus';

const loading = ref(true);
const activeTab = ref('country');

const summary = ref({ totalAmount: 0, totalPlatformFee: 0, totalEscrowFee: 0, orderCount: 0 });
const countryStats = ref([]);
const categoryStats = ref([]);
const workerRank = ref([]);

const feeForm = ref({ platformFeeRate: 0.06, escrowFeeRate: 0.005 });
const savingFee = ref(false);

function fmt(val) {
  return Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

async function handleSaveFee() {
  savingFee.value = true;
  try {
    await updateFeeConfigApi({
      platformFeeRate: feeForm.value.platformFeeRate,
      escrowFeeRate: feeForm.value.escrowFeeRate,
    });
    ElMessage.success('费率配置已更新');
  } catch {
    ElMessage.error('更新失败，请重试');
  } finally {
    savingFee.value = false;
  }
}

onMounted(async () => {
  try {
    const results = await Promise.allSettled([
      getStatSummaryApi(),
      getStatByCountryApi(),
      getStatByCategoryApi(),
      getWorkerIncomeRankApi(10),
      getFeeConfigApi(),
    ]);

    if (results[0].status === 'fulfilled' && results[0].value) summary.value = results[0].value;
    if (results[1].status === 'fulfilled') countryStats.value = results[1].value || [];
    if (results[2].status === 'fulfilled') categoryStats.value = results[2].value || [];
    if (results[3].status === 'fulfilled') workerRank.value = results[3].value || [];
    if (results[4].status === 'fulfilled' && results[4].value) {
      feeForm.value.platformFeeRate = Number(results[4].value.platformFeeRate) || 0.06;
      feeForm.value.escrowFeeRate = Number(results[4].value.escrowFeeRate) || 0.005;
    }
  } catch { /* 静默 */ }
  finally { loading.value = false; }
});
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
  animation: fadeIn 0.4s ease;
}
.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}
.page-sub {
  margin: 4px 0 0;
  font-size: 14px;
  color: #94a3b8;
}

/* ===== 概览卡片 ===== */
.summary-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.summary-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border-left: 4px solid var(--accent);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  animation: fadeIn 0.5s ease;
}
.summary-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
}
.sum-label {
  font-size: 13px;
  color: #94a3b8;
}
.sum-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

/* ===== Tab ===== */
.admin-tabs {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  animation: fadeIn 0.6s ease;
}
.tab-loading {
  padding: 20px 0;
}

/* 排行徽章 */
.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
}
.rank-badge.top {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #ffffff;
}

/* 费率配置 */
.fee-section {
  max-width: 520px;
  padding: 12px 0;
}
.fee-hint {
  margin-left: 12px;
  font-size: 13px;
  color: #94a3b8;
}

/* 响应式 */
@media (max-width: 900px) {
  .summary-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 520px) {
  .summary-row { grid-template-columns: 1fr; }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
