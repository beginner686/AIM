<template>
  <div class="order-list-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="hero-kicker">Order Center</p>
        <h1>我的订单</h1>
        <p>查看每一笔订单的金额拆分、服务费状态和履约进度，点击可进入详情页。</p>
      </div>
      <div class="hero-actions">
        <el-button @click="router.push('/worker-pool')">去创建订单</el-button>
        <el-button type="primary" :icon="Refresh" @click="loadOrders">刷新订单</el-button>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card">
        <p class="kpi-label">订单总数</p>
        <p class="kpi-value">{{ orders.length }}</p>
      </article>
      <article class="kpi-card">
        <p class="kpi-label">履约中</p>
        <p class="kpi-value">{{ activeCount }}</p>
      </article>
      <article class="kpi-card">
        <p class="kpi-label">已完成</p>
        <p class="kpi-value">{{ completedCount }}</p>
      </article>
      <article class="kpi-card">
        <p class="kpi-label">总成交额</p>
        <p class="kpi-value">¥{{ formatMoney(totalAmount) }}</p>
      </article>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="搜索订单ID / 需求ID / 执行者ID"
          style="width: 260px;"
        />
        <el-select v-model="filterStatus" clearable placeholder="订单状态" style="width: 160px;">
          <el-option label="全部状态" value="" />
          <el-option
            v-for="item in orderStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterPayStatus" clearable placeholder="支付状态" style="width: 160px;">
          <el-option label="全部支付状态" value="" />
          <el-option
            v-for="item in payStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button @click="clearFilters">重置筛选</el-button>
      </div>
      <p class="filter-hint">
        当前显示 {{ filteredOrders.length }} / {{ orders.length }} 笔订单，完成率 {{ completionRate }}%
      </p>
    </section>

    <section class="list-panel">
      <div v-if="loading" class="panel-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <el-empty v-else-if="filteredOrders.length === 0" description="暂无订单记录" :image-size="96" />
      <div v-else class="order-list">
        <article
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
          @click="router.push(`/order/${order.id}`)"
        >
          <div class="card-head">
            <div class="head-main">
              <h3>订单 #{{ order.id }}</h3>
              <p>
                需求ID {{ order.demandId || '—' }} · 执行者ID
                {{ order.workerProfileId || order.workerId || order.workerUserId || '—' }}
              </p>
            </div>
            <div class="head-tags">
              <el-tag size="small" :type="orderStatusType(order.status)">
                {{ orderStatusText(order.status) }}
              </el-tag>
              <el-tag size="small" :type="payStatusType(order.payStatus)">
                {{ payStatusText(order.payStatus) }}
              </el-tag>
            </div>
          </div>

          <div class="amount-grid">
            <div>
              <span>订单金额</span>
              <strong>¥{{ formatMoney(order.amount) }}</strong>
            </div>
            <div>
              <span>平台抽成</span>
              <strong>¥{{ formatMoney(order.platformFee) }}</strong>
            </div>
            <div>
              <span>执行者收入</span>
              <strong>¥{{ formatMoney(order.workerIncome) }}</strong>
            </div>
          </div>

          <div class="card-foot">
            <span>服务费状态：{{ serviceFeeText(order) }}</span>
            <span>{{ formatDate(order.createdTime) }}</span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Refresh } from '@element-plus/icons-vue';
import { getMyOrderListApi } from '@/api/order';
import {
  ORDER_ACTIVE_STATUSES,
  ORDER_CLOSED_STATUSES,
  ORDER_COMPLETED_STATUSES,
  ORDER_STATUS_DICT,
  PAY_STATUS,
  PAY_STATUS_DICT,
  getOrderStatusLabel,
  getOrderStatusTag,
  getPayStatusLabel,
  getPayStatusTag,
  toSelectOptions,
} from '@/dicts';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const loading = ref(true);
const orders = ref([]);
const filterStatus = ref('');
const filterPayStatus = ref('');
const keyword = ref('');

const orderStatusOptions = computed(() => toSelectOptions(ORDER_STATUS_DICT));
const payStatusOptions = computed(() => toSelectOptions(PAY_STATUS_DICT));

const filteredOrders = computed(() => {
  const key = keyword.value.toLowerCase();
  return orders.value.filter((order) => {
    if (filterStatus.value && order?.status !== filterStatus.value) {
      return false;
    }
    if (filterPayStatus.value && order?.payStatus !== filterPayStatus.value) {
      return false;
    }
    if (!key) {
      return true;
    }
    const searchable = [
      order?.id,
      order?.demandId,
      order?.workerProfileId,
      order?.workerId,
      order?.workerUserId,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return searchable.includes(key);
  });
});

const activeCount = computed(
  () => orders.value.filter((order) => ORDER_ACTIVE_STATUSES.includes(order?.status)).length,
);

const completedCount = computed(
  () => orders.value.filter((order) => ORDER_COMPLETED_STATUSES.includes(order?.status)).length,
);

const totalAmount = computed(
  () => orders.value.reduce((sum, order) => sum + Number(order?.amount || 0), 0),
);

const completionRate = computed(() => {
  if (!orders.value.length) return '0.00';
  return ((completedCount.value / orders.value.length) * 100).toFixed(2);
});

function orderStatusText(status) {
  return getOrderStatusLabel(status);
}

function orderStatusType(status) {
  return getOrderStatusTag(status);
}

function payStatusText(status) {
  return getPayStatusLabel(status);
}

function payStatusType(status) {
  return getPayStatusTag(status);
}

function serviceFeeText(order) {
  const status = order?.status;
  const feeStatus = order?.serviceFeeStatus;
  if (ORDER_CLOSED_STATUSES.includes(status)) return '订单已关闭';
  if (status === 'WAIT_WORKER_ACCEPT' || status === 'SERVICE_FEE_PAID') return '已支付，待执行者接单';
  if (status === 'SERVICE_FEE_REQUIRED' || status === 'CREATED') return '待支付服务费';
  if (ORDER_COMPLETED_STATUSES.includes(status)) return '服务费已支付';
  if (feeStatus === PAY_STATUS.PAID || feeStatus === 'PAID') return '服务费已支付';
  if (ORDER_ACTIVE_STATUSES.includes(status)) return '服务费处理中';
  return '未开始';
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function clearFilters() {
  filterStatus.value = '';
  filterPayStatus.value = '';
  keyword.value = '';
}

async function loadOrders() {
  loading.value = true;
  try {
    const data = await getMyOrderListApi();
    orders.value = Array.isArray(data) ? data : [];
  } catch {
    orders.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(loadOrders);
</script>

<style scoped>
.order-list-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  border-radius: var(--radius-xl);
  padding: 26px 28px;
  background:
    radial-gradient(circle at 10% 18%, rgba(56, 189, 248, 0.2), transparent 35%),
    linear-gradient(120deg, #06243d 0%, #0a3657 48%, #0d4f66 100%);
  color: #e5edf5;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.hero-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(229, 237, 245, 0.75);
}

.hero h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
  color: #ffffff;
}

.hero p {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(229, 237, 245, 0.88);
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
}

.kpi-label {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.kpi-value {
  margin: 6px 0 0;
  font-size: 28px;
  line-height: 1.05;
  font-weight: 700;
  color: var(--color-text);
}

.filter-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  box-shadow: var(--shadow-sm);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-hint {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.list-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.panel-loading {
  padding: 20px;
}

.order-list {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.order-card:hover {
  border-color: rgba(11, 75, 111, 0.28);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.head-main h3 {
  margin: 0;
  font-size: 17px;
  line-height: 1.2;
}

.head-main p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.head-tags {
  display: flex;
  gap: 8px;
}

.amount-grid {
  margin-top: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.amount-grid span {
  display: block;
  font-size: 12px;
  color: var(--color-text-muted);
}

.amount-grid strong {
  display: block;
  margin-top: 4px;
  font-size: 20px;
  line-height: 1.2;
  color: var(--color-text);
}

.card-foot {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    flex: 1;
  }
}

@media (max-width: 680px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .amount-grid {
    grid-template-columns: 1fr;
  }

  .card-head,
  .card-foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
