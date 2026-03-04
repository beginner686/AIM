<template>
  <div class="order-list-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="hero-kicker">{{ t('orderList.kicker') }}</p>
        <h1>{{ t('orderList.title') }}</h1>
        <p>{{ t('orderList.subtitle') }}</p>
      </div>
      <div class="hero-actions">
        <el-button @click="router.push('/worker-pool')">{{ t('orderList.createOrder') }}</el-button>
        <el-button type="primary" :icon="Refresh" @click="loadOrders">{{ t('orderList.refreshOrders') }}</el-button>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card">
        <p class="kpi-label">{{ t('orderList.kpiTotalOrders') }}</p>
        <p class="kpi-value">{{ orders.length }}</p>
      </article>
      <article class="kpi-card">
        <p class="kpi-label">{{ t('orderList.kpiActive') }}</p>
        <p class="kpi-value">{{ activeCount }}</p>
      </article>
      <article class="kpi-card">
        <p class="kpi-label">{{ t('orderList.kpiCompleted') }}</p>
        <p class="kpi-value">{{ completedCount }}</p>
      </article>
      <article class="kpi-card">
        <p class="kpi-label">{{ t('orderList.kpiTotalAmount') }}</p>
        <p class="kpi-value">¥{{ formatMoney(totalAmount) }}</p>
      </article>
    </section>

    <section class="filter-panel">
      <div class="filter-row">
        <el-input
          v-model.trim="keyword"
          clearable
          :placeholder="t('orderList.searchPlaceholder')"
          style="width: 260px;"
        />
        <el-select v-model="filterStatus" clearable :placeholder="t('orderList.filterOrderStatus')" style="width: 160px;">
          <el-option :label="t('orderList.allStatus')" value="" />
          <el-option
            v-for="item in orderStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filterPayStatus" clearable :placeholder="t('orderList.filterPayStatus')" style="width: 160px;">
          <el-option :label="t('orderList.allPayStatus')" value="" />
          <el-option
            v-for="item in payStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button @click="clearFilters">{{ t('orderList.resetFilters') }}</el-button>
      </div>
      <p class="filter-hint">
        {{ t('orderList.filterHint', { shown: filteredOrders.length, total: orders.length, rate: completionRate }) }}
      </p>
    </section>

    <section class="list-panel">
      <div v-if="loading" class="panel-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <el-empty v-else-if="filteredOrders.length === 0" :description="t('orderList.empty')" :image-size="96" />
      <div v-else class="order-list">
        <article
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
          @click="router.push(`/order/${order.id}`)"
        >
            <div class="card-head">
              <div class="head-main">
                <h3>{{ t('orderList.orderWithId', { id: order.id }) }}</h3>
                <p>
                  {{ t('orderList.demandId') }} {{ order.demandId || '—' }} · {{ t('orderList.workerId') }}
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
                <span>{{ t('orderList.amountOrder') }}</span>
                <strong>¥{{ formatMoney(order.amount) }}</strong>
              </div>
              <div>
                <span>{{ t('orderList.amountPlatformFee') }}</span>
                <strong>¥{{ formatMoney(order.platformFee) }}</strong>
              </div>
              <div>
                <span>{{ t('orderList.amountWorkerIncome') }}</span>
                <strong>¥{{ formatMoney(order.workerIncome) }}</strong>
              </div>
            </div>

            <div class="card-foot">
              <span>{{ t('orderList.serviceFeeStatus') }}{{ serviceFeeText(order) }}</span>
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
import { useI18n } from 'vue-i18n';
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
const { t } = useI18n();
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
  if (ORDER_CLOSED_STATUSES.includes(status)) return t('orderList.feeClosed');
  if (status === 'WAIT_WORKER_ACCEPT' || status === 'SERVICE_FEE_PAID') return t('orderList.feePaidWaiting');
  if (status === 'SERVICE_FEE_REQUIRED' || status === 'CREATED') return t('orderList.feeRequired');
  if (ORDER_COMPLETED_STATUSES.includes(status)) return t('orderList.feePaid');
  if (feeStatus === PAY_STATUS.PAID || feeStatus === 'PAID') return t('orderList.feePaid');
  if (ORDER_ACTIVE_STATUSES.includes(status)) return t('orderList.feeProcessing');
  return t('orderList.feeNotStarted');
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
