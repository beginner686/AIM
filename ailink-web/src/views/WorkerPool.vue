<template>
  <div class="worker-page">
    <section class="hero">
      <div class="hero-left">
        <p class="kicker">Worker Matching Workspace</p>
        <h1>执行者资源池</h1>
        <p class="sub">
          基于国家与类目快速筛选可用执行者，直接创建订单进入托管履约流程。
        </p>
        <div class="hero-tags">
          <span class="chip">在线资源 {{ workers.length }}</span>
          <span class="chip">平均评分 {{ avgRating.toFixed(1) }}</span>
          <span class="chip">需求ID {{ demandId || '未携带' }}</span>
        </div>
      </div>

      <div class="hero-right">
        <el-button type="primary" @click="$router.push('/publish-demand')">发布新需求</el-button>
        <el-button @click="resetFilters">清空筛选</el-button>
      </div>
    </section>

    <el-alert
      v-if="!demandId"
      title="未检测到 demandId，当前只能浏览资源，无法创建订单。请先发布需求。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card class="filter-card" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>筛选执行者</h2>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="国家">
          <el-input v-model="filters.country" placeholder="例如：Singapore" clearable />
        </el-form-item>
        <el-form-item label="类目">
          <el-input v-model="filters.category" placeholder="例如：翻译" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="listLoading" @click="loadWorkers">搜索</el-button>
        </el-form-item>
      </el-form>

      <div class="preset-row">
        <span class="preset-label">常用国家：</span>
        <button
          v-for="item in countryPresets"
          :key="item"
          type="button"
          class="preset-btn"
          @click="filters.country = item"
        >
          {{ item }}
        </button>
      </div>

      <div class="preset-row">
        <span class="preset-label">常用类目：</span>
        <button
          v-for="item in categoryPresets"
          :key="item"
          type="button"
          class="preset-btn"
          @click="filters.category = item"
        >
          {{ item }}
        </button>
      </div>
    </el-card>

    <section class="metric-grid">
      <article class="metric">
        <p class="label">筛选结果</p>
        <p class="value">{{ workers.length }}</p>
      </article>
      <article class="metric">
        <p class="label">平均评分</p>
        <p class="value">{{ avgRating.toFixed(1) }}</p>
      </article>
      <article class="metric">
        <p class="label">参考均价</p>
        <p class="value">{{ formatMoney(avgQuote) }}</p>
      </article>
    </section>

    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>执行者列表</h2>
          <span class="count">共 {{ workers.length }} 位</span>
        </div>
      </template>

      <div v-if="listLoading" class="loading-wrap">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty v-else-if="workers.length === 0" description="暂无符合条件的执行者" :image-size="88" />

      <div v-else class="worker-grid">
        <article v-for="row in workers" :key="row.workerId" class="worker-card">
          <div class="worker-top">
            <div>
              <h3>{{ row.name }}</h3>
              <p>ID: {{ row.workerId }}</p>
            </div>
            <el-tag type="success">{{ row.country || '—' }}</el-tag>
          </div>

          <div class="skill-row">
            <el-tag
              v-for="tag in splitSkills(row.category)"
              :key="tag"
              size="small"
              type="info"
              effect="plain"
            >
              {{ tag }}
            </el-tag>
          </div>

          <div class="meta-row">
            <span>成交单数 {{ row.dealCount }}</span>
            <span>评分 {{ row.rating.toFixed(1) }}</span>
            <span>参考报价 {{ formatMoney(getEstimatedAmount(row)) }}</span>
          </div>

          <div class="action-row">
            <el-button
              type="primary"
              :disabled="!demandId"
              :loading="Boolean(createLoadingMap[row.workerId])"
              @click="handleCreateOrder(row)"
            >
              选择并创建订单
            </el-button>
          </div>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getWorkerListApi } from '@/api/worker';
import { createOrderApi } from '@/api/order';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const route = useRoute();

const demandId = ref(route.query?.demandId ? Number(route.query.demandId) : 0);
const listLoading = ref(false);
const workers = ref([]);
const createLoadingMap = reactive({});
const filters = reactive({
  country: '',
  category: '',
});

const countryPresets = ['Singapore', 'Japan', 'UAE', 'Germany', 'Australia'];
const categoryPresets = ['翻译本地化', '远程助理', '视频剪辑', '海外投放', '客服支持'];

const avgRating = computed(() => {
  if (workers.value.length === 0) return 0;
  const total = workers.value.reduce((sum, item) => sum + Number(item.rating || 0), 0);
  return total / workers.value.length;
});

const avgQuote = computed(() => {
  if (workers.value.length === 0) return 0;
  const total = workers.value.reduce((sum, item) => sum + getEstimatedAmount(item), 0);
  return total / workers.value.length;
});

watch(
  () => route.query.demandId,
  (value) => {
    demandId.value = value ? Number(value) : 0;
  },
);

async function loadWorkers() {
  listLoading.value = true;
  try {
    workers.value = await getWorkerListApi(filters);
  } catch {
    workers.value = [];
  } finally {
    listLoading.value = false;
  }
}

function getEstimatedAmount(worker) {
  const min = Number(worker?.priceMin || 0);
  const max = Number(worker?.priceMax || 0);
  if (min > 0 && max > 0) {
    return (min + max) / 2;
  }
  if (min > 0) return min;
  if (max > 0) return max;
  return 100;
}

function splitSkills(text) {
  const source = String(text || '').trim();
  if (!source) return ['未标注'];
  return source
    .split(/[，,]/)
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, 4);
}

function resetFilters() {
  filters.country = '';
  filters.category = '';
  loadWorkers();
}

async function handleCreateOrder(worker) {
  if (!demandId.value) {
    ElMessage.warning('缺少 demandId，请先发布需求');
    return;
  }
  const workerId = Number(worker?.workerId || worker?.id || 0);
  if (!workerId) {
    ElMessage.error('执行者ID无效');
    return;
  }

  createLoadingMap[workerId] = true;
  try {
    const data = await createOrderApi({
      demandId: demandId.value,
      workerId,
      amount: getEstimatedAmount(worker),
    });
    const orderId = data?.orderId || data?.id || data;
    if (!orderId) {
      throw new Error('未获取到订单ID');
    }
    ElMessage.success('订单创建成功');
    router.push(`/order/${orderId}`);
  } finally {
    createLoadingMap[workerId] = false;
  }
}

onMounted(() => {
  loadWorkers();
});
</script>

<style scoped>
.worker-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 16px;
  padding: 18px 20px;
  background:
    radial-gradient(circle at 82% 0%, rgba(76, 166, 212, 0.18), transparent 32%),
    linear-gradient(120deg, #0a3754 0%, #0f5a66 55%, #0b6a5a 100%);
  color: #e7f4ff;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.kicker {
  margin: 0 0 5px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #9dd3ef;
}

.hero h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
}

.sub {
  margin: 8px 0 0;
  color: #c3dcec;
  font-size: 14px;
}

.hero-tags {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.hero-right {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.filter-card,
.list-card {
  border-radius: 14px;
  border: 1px solid #dbe7f1;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
  color: #0f2c42;
}

.count {
  color: #6d8698;
  font-size: 13px;
}

.filter-form {
  margin-bottom: 8px;
}

.preset-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 6px;
}

.preset-label {
  font-size: 12px;
  color: #6f8597;
}

.preset-btn {
  border: 1px solid #d0dfec;
  background: #f8fbff;
  color: #214962;
  border-radius: 999px;
  font-size: 12px;
  padding: 4px 10px;
  cursor: pointer;
}

.preset-btn:hover {
  border-color: #9fc4df;
  background: #edf5fc;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  border: 1px solid #dbe7f1;
  border-radius: 12px;
  padding: 12px 14px;
  background: #ffffff;
}

.metric .label {
  margin: 0;
  color: #6a8396;
  font-size: 12px;
}

.metric .value {
  margin: 4px 0 0;
  color: #102c42;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

.loading-wrap {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.worker-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.worker-card {
  border: 1px solid #d9e7f2;
  border-radius: 12px;
  padding: 12px;
  background: #fbfdff;
}

.worker-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.worker-top h3 {
  margin: 0;
  color: #11344d;
  font-size: 19px;
}

.worker-top p {
  margin: 4px 0 0;
  color: #6f8699;
  font-size: 12px;
}

.skill-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-row {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: #425c71;
  font-size: 13px;
}

.action-row {
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .worker-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero h1 {
    font-size: 24px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .hero-right {
    width: 100%;
  }
}
</style>
