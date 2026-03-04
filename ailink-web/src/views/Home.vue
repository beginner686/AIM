<template>
  <div class="home-page">
    <section class="hero-strip reveal-up">
      <div class="orb orb-a" />
      <div class="orb orb-b" />

      <div class="hero-copy hero-content">
        <p class="hero-kicker">{{ t('dashboard.kicker') }}</p>
        <h1 class="hero-title">
          <span class="gradient-text">{{ t('dashboard.greetingLine', { greeting, username: userStore.userInfo?.username || t('dashboard.user') }) }}</span>
        </h1>
        <p class="hero-sub">{{ t('dashboard.heroSub') }}</p>
        <div class="hero-tags reveal-up delay-1">
          <span class="chip">{{ t('dashboard.countryCover') }} {{ countryCount }}</span>
          <span class="chip">{{ t('dashboard.completionRate') }} {{ completionRate }}%</span>
          <span class="chip">{{ t('dashboard.gmv') }} {{ formatMoney(totalAmount) }}</span>
        </div>
      </div>

      <div class="hero-actions hero-content reveal-up delay-1">
        <el-button class="hero-btn-primary" size="large" @click="$router.push('/publish-demand')">
          <el-icon><Plus /></el-icon>
          {{ t('dashboard.publishDemandBtn') }}
        </el-button>
        <el-button class="hero-btn-ghost" size="large" @click="$router.push('/worker-pool')">
          <el-icon><Search /></el-icon>
          {{ t('dashboard.browseWorkerBtn') }}
        </el-button>
      </div>
    </section>

    <section class="kpi-grid reveal-up delay-1">
      <article v-for="card in kpiCards" :key="card.key" class="kpi-card">
        <div class="kpi-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="20"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-text">
          <p class="kpi-label">{{ card.label }}</p>
          <p class="kpi-value">{{ loading ? '—' : card.value }}</p>
          <p class="kpi-foot">{{ card.foot }}</p>
        </div>
      </article>
    </section>

    <section class="panel-grid reveal-up delay-1">
      <article class="panel">
        <header class="panel-header">
          <h2>{{ t('dashboard.latestDemands') }}</h2>
          <el-button link type="primary" @click="$router.push('/publish-demand')">{{ t('dashboard.continuePub') }}</el-button>
        </header>

        <div v-if="loading" class="panel-loading">
          <el-skeleton :rows="4" animated />
        </div>
        <el-empty v-else-if="latestDemands.length === 0" :description="t('dashboard.noDemands')" :image-size="72" />

        <ul v-else class="demand-list">
          <li
            v-for="(item, index) in latestDemands"
            :key="item.id"
            class="demand-item reveal-up"
            :style="{ animationDelay: `${index * 0.05}s` }"
            :class="{ 'is-clickable': canMatchDemand(item) }"
            @click="handleDemandMatch(item)"
          >
            <div class="item-head">
              <span class="title">{{ item.category || t('dashboard.noCategory') }}</span>
              <el-tag size="small" :type="demandStatusType(item.status)">{{ demandStatusText(item.status) }}</el-tag>
            </div>
            <p class="desc">{{ item.description || t('dashboard.noDesc') }}</p>
            <div class="meta">
              <span>{{ item.targetCountry || '—' }}</span>
              <span>{{ t('dashboard.budget') }} {{ formatMoney(item.budget) }}</span>
              <span>{{ formatDate(item.createdTime) }}</span>
            </div>
          </li>
        </ul>
      </article>

      <article class="panel">
        <header class="panel-header">
          <h2>{{ t('dashboard.myOrders') }}</h2>
          <el-button link type="primary" @click="$router.push('/orders')">{{ t('dashboard.viewAll') }}</el-button>
        </header>

        <div v-if="loading" class="panel-loading">
          <el-skeleton :rows="4" animated />
        </div>
        <el-empty v-else-if="latestOrders.length === 0" :description="t('dashboard.noOrders')" :image-size="72" />

        <ul v-else class="order-list">
          <li
            v-for="(order, index) in latestOrders"
            :key="order.id"
            class="order-item reveal-up"
            :style="{ animationDelay: `${index * 0.05}s` }"
            @click="openOrder(order)"
          >
            <div class="item-head">
              <span class="title">{{ t('dashboard.orderWithId', { id: order.id }) }}</span>
              <el-tag size="small" :type="orderStatusType(order.status)">{{ orderStatusText(order.status) }}</el-tag>
            </div>
            <div class="order-amount-row">
              <span class="amount">{{ formatMoney(order.amount) }}</span>
            </div>
            <div class="meta">
              <el-tag size="small" type="info">{{ payStatusText(order.payStatus) }}</el-tag>
              <span>{{ formatDate(order.createdTime) }}</span>
            </div>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { Plus, Search, Document, ShoppingCart, DataLine, Finished } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/store/modules/user';
import { getDemandListApi, getMyDemandListApi } from '@/api/demand';
import { getMyOrderListApi } from '@/api/order';
import {
  DEMAND_OPEN_STATUSES,
  ORDER_ACTIVE_STATUSES,
  ORDER_FINISHED_STATUSES,
  getDemandStatusLabel,
  getDemandStatusTag,
  getOrderStatusLabel,
  getOrderStatusTag,
  getPayStatusLabel,
} from '@/dicts';
import { formatMoney } from '@/utils/format';

const userStore = useUserStore();
const router = useRouter();
const { t } = useI18n();
const loading = ref(true);
const demands = ref([]);
const myDemands = ref([]);
const myOrders = ref([]);

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return t('dashboard.greetNight');
  if (hour < 12) return t('dashboard.greetMorning');
  if (hour < 14) return t('dashboard.greetNoon');
  if (hour < 18) return t('dashboard.greetAfternoon');
  return t('dashboard.greetEvening');
});

const latestDemands = computed(() => {
  if (!Array.isArray(demands.value)) return [];
  return demands.value.filter(canMatchDemand).slice(0, 5);
});
const latestOrders = computed(() => (Array.isArray(myOrders.value) ? myOrders.value.slice(0, 5) : []));

const totalAmount = computed(() =>
  myOrders.value.reduce((sum, order) => sum + Number(order?.amount || 0), 0),
);

const platformRevenue = computed(() =>
  myOrders.value.reduce((sum, order) => sum + Number(order?.platformFee || 0), 0),
);

const activeOrderCount = computed(
  () => myOrders.value.filter((o) => ORDER_ACTIVE_STATUSES.includes(o?.status)).length,
);

const completionRate = computed(() => {
  const total = myOrders.value.length;
  if (!total) return '0.00';
  const done = myOrders.value.filter((o) => ORDER_FINISHED_STATUSES.includes(o?.status)).length;
  return ((done / total) * 100).toFixed(2);
});

const countryCount = computed(() => {
  const set = new Set();
  myDemands.value.forEach((d) => {
    if (d?.targetCountry) set.add(d.targetCountry);
  });
  demands.value.forEach((d) => {
    if (d?.targetCountry) set.add(d.targetCountry);
  });
  return set.size;
});

const kpiCards = computed(() => [
  {
    key: 'demand',
    label: t('dashboard.myDemands'),
    value: myDemands.value.length,
    foot: t('dashboard.demandFoot'),
    icon: Document,
    color: '#0d9488',
    bg: 'rgba(13, 148, 136, 0.12)',
  },
  {
    key: 'order',
    label: t('dashboard.myOrders'),
    value: myOrders.value.length,
    foot: t('dashboard.orderFoot'),
    icon: ShoppingCart,
    color: '#0284c7',
    bg: 'rgba(2, 132, 199, 0.12)',
  },
  {
    key: 'active',
    label: t('dashboard.active'),
    value: activeOrderCount.value,
    foot: t('dashboard.activeFoot'),
    icon: DataLine,
    color: '#ea580c',
    bg: 'rgba(234, 88, 12, 0.12)',
  },
  {
    key: 'gmv',
    label: t('dashboard.gmv'),
    value: formatMoney(totalAmount.value),
    foot: t('dashboard.gmvFoot', { rate: completionRate.value }),
    icon: Finished,
    color: '#15803d',
    bg: 'rgba(21, 128, 61, 0.12)',
  },
]);

function formatDate(dt) {
  if (!dt) return '—';
  const d = new Date(dt);
  if (Number.isNaN(d.getTime())) return '—';
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function demandStatusText(status) {
  return getDemandStatusLabel(status);
}
function demandStatusType(status) {
  return getDemandStatusTag(status);
}

function orderStatusText(status) {
  return getOrderStatusLabel(status);
}
function orderStatusType(status) {
  return getOrderStatusTag(status);
}

function payStatusText(status) {
  return getPayStatusLabel(status);
}

function canMatchDemand(item) {
  const status = String(item?.status || '').trim();
  if (!status) return false;
  if (DEMAND_OPEN_STATUSES.length > 0) {
    return DEMAND_OPEN_STATUSES.includes(status);
  }
  return status === 'OPEN';
}

function handleDemandMatch(item) {
  if (!canMatchDemand(item)) return;
  const demandId = Number(item?.id || 0);
  if (!demandId) return;
  router.push({
    path: '/worker-pool',
    query: { demandId: String(demandId) },
  });
}

function resolveOrderEntryPath(order) {
  const id = Number(order?.id || 0);
  if (!id) return '';
  const status = String(order?.status || '').trim().toUpperCase();
  if (status === 'CREATED' || status === 'SERVICE_FEE_REQUIRED') {
    return `/order/checkout/${id}`;
  }
  if (status === 'SERVICE_FEE_PAID' || status === 'WAIT_WORKER_ACCEPT' || status === 'MATCH_UNLOCKED') {
    return `/order/match/${id}`;
  }
  return `/order/${id}`;
}

function openOrder(order) {
  const path = resolveOrderEntryPath(order);
  if (!path) return;
  router.push(path);
}

onMounted(async () => {
  loading.value = true;
  try {
    const [allDemandResult, myDemandResult, myOrderResult] = await Promise.allSettled([
      getDemandListApi(),
      getMyDemandListApi(),
      getMyOrderListApi(),
    ]);

    demands.value = allDemandResult.status === 'fulfilled' && Array.isArray(allDemandResult.value)
      ? allDemandResult.value
      : [];
    myDemands.value = myDemandResult.status === 'fulfilled' && Array.isArray(myDemandResult.value)
      ? myDemandResult.value
      : [];
    myOrders.value = myOrderResult.status === 'fulfilled' && Array.isArray(myOrderResult.value)
      ? myOrderResult.value
      : [];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.home-page {
  --surface: rgba(255, 255, 255, 0.76);
  --border-light: rgba(216, 228, 246, 0.85);
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
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
  justify-content: space-between;
  gap: 24px;
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

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 720px;
}

.hero-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8ad0ff;
}

.hero-title {
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

.hero-sub {
  margin: 12px 0 0;
  max-width: 640px;
  font-size: 14.5px;
  color: rgba(229, 238, 255, 0.9);
  line-height: 1.75;
}

.hero-tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #eaf4ff;
  font-size: 12px;
}

.hero-actions {
  z-index: 1;
  display: flex;
  align-items: center;
  align-self: center;
  gap: 12px;
  flex-wrap: wrap;
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
  display: flex;
  gap: 14px;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(27, 49, 90, 0.05);
}

.kpi-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-text {
  min-width: 0;
}

.kpi-label {
  margin: 0;
  font-size: 12px;
  color: #5a7488;
}

.kpi-value {
  margin: 4px 0 2px;
  font-size: 27px;
  line-height: 1.1;
  color: #0d2438;
  font-weight: 700;
}

.kpi-foot {
  margin: 0;
  font-size: 12px;
  color: #8ba2b4;
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  border-radius: 18px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--border-light);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(27, 49, 90, 0.04);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #102449;
  letter-spacing: -0.01em;
}

.panel-loading {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.demand-list,
.order-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0;
  margin: 0;
}

.demand-item,
.order-item {
  border-radius: 16px;
  border: 1px solid rgba(216, 228, 246, 0.85);
  padding: 16px;
  background: #fdfdff;
  box-shadow: 0 12px 24px rgba(31, 57, 107, 0.02);
  transition: border-color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}

.demand-item.is-clickable {
  cursor: pointer;
}

.demand-item.is-clickable:hover,
.order-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 36px rgba(31, 57, 107, 0.08);
  border-color: #bad2f9;
  background: #fbfdff;
}

.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.title {
  font-size: 15px;
  font-weight: 700;
  color: #14344c;
}

.desc {
  margin: 8px 0 10px;
  color: #4d6678;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.order-amount-row {
  margin: 8px 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.amount {
  font-size: 20px;
  line-height: 1.1;
  color: #0d2438;
  font-weight: 700;
}

.sub {
  color: #658195;
  font-size: 12px;
}

.meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #7e95a7;
  font-size: 12px;
  align-items: center;
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

@media (max-width: 1120px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-strip {
    padding: 24px 18px;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
