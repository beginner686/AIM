<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="hero-kicker">AI-Link Global Workforce Platform</p>
        <h1 class="hero-title">{{ greeting }}，{{ userStore.userInfo?.username || '用户' }}</h1>
        <p class="hero-sub">
          用托管交易保障跨国协作，覆盖发布需求、执行匹配、订单履约、收益结算的完整闭环。
        </p>
        <div class="hero-tags">
          <span class="chip">覆盖国家 {{ countryCount }}</span>
          <span class="chip">订单完成率 {{ completionRate }}%</span>
          <span class="chip">平台抽成 {{ formatMoney(platformRevenue) }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/publish-demand')">
          <el-icon><Plus /></el-icon>
          发布跨国需求
        </el-button>
        <el-button size="large" @click="$router.push('/worker-pool')">
          <el-icon><Search /></el-icon>
          浏览执行者资源池
        </el-button>
      </div>
    </section>

    <section class="kpi-grid">
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

    <section class="panel-grid">
      <article class="panel">
        <header class="panel-header">
          <h2>最新需求</h2>
          <el-button link type="primary" @click="$router.push('/publish-demand')">继续发布</el-button>
        </header>

        <div v-if="loading" class="panel-loading">
          <el-skeleton :rows="4" animated />
        </div>
        <el-empty v-else-if="latestDemands.length === 0" description="暂无需求记录" :image-size="72" />

        <ul v-else class="demand-list">
          <li v-for="item in latestDemands" :key="item.id" class="demand-item">
            <div class="item-head">
              <span class="title">{{ item.category || '未分类' }}</span>
              <el-tag size="small" :type="demandStatusType(item.status)">{{ demandStatusText(item.status) }}</el-tag>
            </div>
            <p class="desc">{{ item.description || '暂无描述' }}</p>
            <div class="meta">
              <span>{{ item.targetCountry || '—' }}</span>
              <span>预算 {{ formatMoney(item.budget) }}</span>
              <span>{{ formatDate(item.createdTime) }}</span>
            </div>
          </li>
        </ul>
      </article>

      <article class="panel">
        <header class="panel-header">
          <h2>我的订单</h2>
          <el-button link type="primary" @click="$router.push('/orders')">查看全部</el-button>
        </header>

        <div v-if="loading" class="panel-loading">
          <el-skeleton :rows="4" animated />
        </div>
        <el-empty v-else-if="latestOrders.length === 0" description="暂无订单记录" :image-size="72" />

        <ul v-else class="order-list">
          <li v-for="order in latestOrders" :key="order.id" class="order-item" @click="$router.push(`/order/${order.id}`)">
            <div class="item-head">
              <span class="title">订单 #{{ order.id }}</span>
              <el-tag size="small" :type="orderStatusType(order.status)">{{ orderStatusText(order.status) }}</el-tag>
            </div>
            <div class="order-amount-row">
              <span class="amount">{{ formatMoney(order.amount) }}</span>
              <span class="sub">平台抽成 {{ formatMoney(order.platformFee) }}</span>
              <span class="sub">执行者收入 {{ formatMoney(order.workerIncome) }}</span>
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
import { useUserStore } from '@/store/modules/user';
import { getDemandListApi, getMyDemandListApi } from '@/api/demand';
import { getMyOrderListApi } from '@/api/order';
import { formatMoney } from '@/utils/format';

const userStore = useUserStore();
const loading = ref(true);
const demands = ref([]);
const myDemands = ref([]);
const myOrders = ref([]);

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const latestDemands = computed(() => (Array.isArray(demands.value) ? demands.value.slice(0, 5) : []));
const latestOrders = computed(() => (Array.isArray(myOrders.value) ? myOrders.value.slice(0, 5) : []));

const totalAmount = computed(() =>
  myOrders.value.reduce((sum, order) => sum + Number(order?.amount || 0), 0),
);

const platformRevenue = computed(() =>
  myOrders.value.reduce((sum, order) => sum + Number(order?.platformFee || 0), 0),
);

const activeOrderCount = computed(
  () => myOrders.value.filter((o) => ['CREATED', 'ESCROWED', 'IN_PROGRESS', 'DISPUTED'].includes(o?.status)).length,
);

const completionRate = computed(() => {
  const total = myOrders.value.length;
  if (!total) return '0.00';
  const done = myOrders.value.filter((o) => ['COMPLETED', 'CLOSED'].includes(o?.status)).length;
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
    label: '我的需求',
    value: myDemands.value.length,
    foot: '待匹配与履约需求总数',
    icon: Document,
    color: '#0d9488',
    bg: 'rgba(13, 148, 136, 0.12)',
  },
  {
    key: 'order',
    label: '我的订单',
    value: myOrders.value.length,
    foot: '订单全流程可追踪',
    icon: ShoppingCart,
    color: '#0284c7',
    bg: 'rgba(2, 132, 199, 0.12)',
  },
  {
    key: 'active',
    label: '履约中',
    value: activeOrderCount.value,
    foot: '进行中/托管中/争议中',
    icon: DataLine,
    color: '#ea580c',
    bg: 'rgba(234, 88, 12, 0.12)',
  },
  {
    key: 'gmv',
    label: '总成交额',
    value: formatMoney(totalAmount.value),
    foot: `完成率 ${completionRate.value}%`,
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

const demandStatusMap = {
  OPEN: '开放中',
  MATCHED: '已匹配',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  CLOSED: '已关闭',
};
function demandStatusText(status) {
  return demandStatusMap[status] || status || '未知';
}
function demandStatusType(status) {
  if (status === 'OPEN') return 'success';
  if (status === 'MATCHED' || status === 'IN_PROGRESS') return 'warning';
  if (status === 'DONE') return 'primary';
  if (status === 'CLOSED') return 'info';
  return '';
}

const orderStatusMap = {
  CREATED: '已创建',
  ESCROWED: '托管中',
  IN_PROGRESS: '执行中',
  COMPLETED: '已完成',
  DISPUTED: '争议中',
  CLOSED: '已关闭',
};
function orderStatusText(status) {
  return orderStatusMap[status] || status || '未知';
}
function orderStatusType(status) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'ESCROWED' || status === 'IN_PROGRESS') return 'warning';
  if (status === 'DISPUTED') return 'danger';
  if (status === 'CLOSED') return 'info';
  return '';
}

const payStatusMap = {
  UNPAID: '待支付',
  PAID: '已支付',
  RELEASED: '已释放',
  REFUNDED: '已退款',
};
function payStatusText(status) {
  return payStatusMap[status] || status || '—';
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
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  padding: 34px 36px;
  background:
    radial-gradient(circle at 15% 20%, rgba(56, 189, 248, 0.22), transparent 34%),
    radial-gradient(circle at 88% 88%, rgba(30, 64, 175, 0.32), transparent 40%),
    linear-gradient(120deg, #06243d 0%, #0a3657 52%, #0d4f66 100%);
  color: #e2ecf4;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.hero::after {
  content: '';
  position: absolute;
  width: 380px;
  height: 380px;
  right: -120px;
  top: -150px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(148, 213, 255, 0.18), rgba(148, 213, 255, 0));
  pointer-events: none;
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
  font-size: 34px;
  line-height: 1.2;
  font-weight: 700;
  color: #f6fbff;
}

.hero-sub {
  margin: 12px 0 0;
  max-width: 640px;
  font-size: 15px;
  color: #bcd2e3;
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
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #dbe7f1;
  border-radius: 14px;
  padding: 14px 14px 12px;
  display: flex;
  gap: 12px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(2, 32, 71, 0.08);
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
  border-radius: 14px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe7f1;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #082339;
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
  gap: 10px;
  padding: 0;
  margin: 0;
}

.demand-item,
.order-item {
  border-radius: 12px;
  border: 1px solid #e6edf4;
  padding: 12px;
  background: #fbfdff;
}

.order-item {
  cursor: pointer;
}

.order-item:hover {
  border-color: #9ac3df;
  background: #f2f9ff;
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

@media (max-width: 1120px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 24px 18px;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-sub {
    font-size: 14px;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .panel-header h2 {
    font-size: 17px;
  }
}
</style>
