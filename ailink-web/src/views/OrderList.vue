<template>
  <div class="order-list-page">
    <section class="hero-strip reveal-up">
      <div class="orb orb-a" />
      <div class="orb orb-b" />

      <div class="hero-copy hero-content">
        <p class="hero-kicker">{{ t('orderList.kicker') }}</p>
        <h1>
          <span class="gradient-text">{{ t('orderList.title') }}</span>
        </h1>
        <p>{{ t('orderList.subtitle') }}</p>
      </div>
      <div class="hero-actions hero-content reveal-up delay-1">
        <el-button class="hero-btn-primary" @click="router.push('/worker-pool')">{{ t('orderList.createOrder') }}</el-button>
        <el-button class="hero-btn-ghost" :icon="Refresh" @click="loadOrders">{{ t('orderList.refreshOrders') }}</el-button>
      </div>
    </section>

    <section class="kpi-grid reveal-up delay-1">
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

    <section class="filter-panel reveal-up delay-1">
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

    <section class="list-panel reveal-up delay-1">
      <div v-if="loading" class="panel-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <el-empty v-else-if="filteredOrders.length === 0" :description="t('orderList.empty')" :image-size="96" />
      <div v-else class="order-list">
        <article
          v-for="(order, index) in filteredOrders"
          :key="order.id"
          class="order-card reveal-up"
          :style="{ animationDelay: `${index * 0.05}s` }"
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
  --surface: rgba(255, 255, 255, 0.76);
  --border-light: rgba(216, 228, 246, 0.85);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;
}

.hero-strip {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  padding: 28px 30px;
  background:
    radial-gradient(circle at 18% -6%, rgba(68, 125, 255, 0.58), transparent 45%),
    radial-gradient(circle at 88% 10%, rgba(61, 225, 226, 0.4), transparent 40%),
    linear-gradient(135deg, #08132f 0%, #0f2451 46%, #11386e 100%);
  color: #f6f9ff;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  box-shadow: 0 16px 32px rgba(9, 23, 56, 0.12);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(12px);
  pointer-events: none;
}

.orb-a {
  width: 200px;
  height: 200px;
  left: -40px;
  top: -40px;
  background: rgba(86, 140, 255, 0.45);
  animation: floatY 6s ease-in-out infinite;
}

.orb-b {
  width: 160px;
  height: 160px;
  right: 18%;
  top: 10%;
  background: rgba(65, 234, 203, 0.3);
  animation: floatY 8s ease-in-out infinite reverse;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: #cad5f3;
}

.hero-strip h1 {
  margin: 0;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.gradient-text {
  background: linear-gradient(92deg, #36c9f7, #73f2ba);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-strip p {
  margin: 10px 0 0;
  color: rgba(229, 238, 255, 0.9);
  font-size: 14.5px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  align-self: center;
}

.hero-btn-primary {
  border-radius: 999px;
  padding: 12px 24px;
  font-weight: 600;
  border: none;
  color: #052241;
  background: linear-gradient(120deg, #75e9ff, #8fffb7);
  box-shadow: 0 12px 24px rgba(31, 227, 208, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(31, 227, 208, 0.35);
  background: linear-gradient(120deg, #8cf0ff, #a6ffc6);
}

.hero-btn-ghost {
  border-radius: 999px;
  padding: 12px 24px;
  font-weight: 600;
  color: #ecf6ff;
  border: 1px solid rgba(212, 230, 255, 0.45);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;
}

.hero-btn-ghost:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  transform: translateY(-2px);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 16px 18px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 24px rgba(27, 49, 90, 0.03);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(27, 49, 90, 0.05);
}

.kpi-label {
  margin: 0;
  color: #6a8396;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-value {
  margin: 6px 0 0;
  color: #102c42;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.filter-panel {
  background: var(--surface);
  border: 1px solid var(--border-light);
  border-radius: 18px;
  padding: 16px 20px;
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(27, 49, 90, 0.04);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-hint {
  margin: 10px 0 0;
  color: #65819e;
  font-size: 13.5px;
}

.list-panel {
  background: var(--surface);
  border: 1px solid var(--border-light);
  border-radius: 18px;
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(27, 49, 90, 0.04);
}

/* 深度定制表单输入框样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(216, 228, 246, 0.9) inset;
  background: #fdfeff;
  transition: all 0.25s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #a4c5f4 inset;
  background: #fff;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px #5a9cf8 inset !important;
  background: #fff;
}

.filter-row :deep(.el-button) {
  border-radius: 999px;
  font-weight: 600;
  padding: 8px 18px;
}

.panel-loading {
  padding: 24px;
}

.order-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border: 1px solid rgba(216, 228, 246, 0.85);
  border-radius: 16px;
  padding: 18px 20px;
  background: #fdfdff;
  box-shadow: 0 12px 24px rgba(31, 57, 107, 0.03);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.order-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 36px rgba(31, 57, 107, 0.08);
  border-color: #bad2f9;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.head-main h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #102449;
  line-height: 1.2;
}

.head-main p {
  margin: 6px 0 0;
  font-size: 13.5px;
  color: #65819e;
}

.head-tags {
  display: flex;
  gap: 8px;
}

.amount-grid {
  margin-top: 14px;
  padding: 14px 0;
  border-top: 1px dashed rgba(200, 216, 238, 0.7);
  border-bottom: 1px dashed rgba(200, 216, 238, 0.7);
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.amount-grid span {
  display: block;
  font-size: 13px;
  color: #6a8396;
}

.amount-grid strong {
  display: block;
  margin-top: 4px;
  font-size: 20px;
  line-height: 1.2;
  color: #102c42;
}

.card-foot {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: #65819e;
}

@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.reveal-up {
  animation: fadeInUp 0.55s ease both;
}

.delay-1 {
  animation-delay: 0.1s;
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .hero-strip {
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
