<template>
  <div class="worker-page">
    <section class="hero">
      <div class="hero-left">
        <p class="kicker">Worker Matching Workspace</p>
        <h1>执行者资源池</h1>
        <p class="sub">
          基于国家与类目快速筛选可用执行者，直接创建订单进入服务费解锁履约流程。
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
    <el-alert
      v-else-if="demandInfo"
      :title="`当前需求 #${demandInfo.id}：${getCountryLabel(demandInfo.targetCountry) || demandInfo.targetCountry || '—'} / ${resolveDemandCategoryLabel(demandInfo.category) || '—'}`"
      type="info"
      :closable="false"
      show-icon
    />
    <el-alert
      v-else-if="demandLoadError"
      :title="demandLoadError"
      type="error"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="demandFallbackActive"
      title="当前需求暂无直接匹配执行者，已展示全部可用执行者。你可以手动调整国家/类目继续筛选。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card class="filter-card" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>筛选执行者</h2>
          <span class="count">国家/类目支持模糊检索</span>
        </div>
      </template>

      <el-form :model="filters" class="filter-form" label-width="76px">
        <div class="filter-grid">
          <el-form-item label="国家">
            <el-select
              v-model="filters.country"
              filterable
              clearable
              placeholder="请选择国家"
              :loading="countryLoading"
              @change="handleFilterChange"
            >
              <el-option
                v-for="item in countryOptions"
                :key="item.code"
                :value="item.code"
                :label="item.label"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="类目">
            <el-select
              v-model="filters.category"
              filterable
              clearable
              placeholder="请选择类目"
              @change="handleFilterChange"
            >
              <el-option
                v-for="item in categoryOptions"
                :key="item.code"
                :value="item.label"
                :label="item.label"
              />
            </el-select>
          </el-form-item>

          <div class="filter-actions">
            <el-button type="primary" :loading="listLoading" @click="loadWorkers">搜索</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
        <el-form-item v-if="hotCountryOptions.length" label="热门国家：" class="quick-form-item">
          <div class="quick-tags">
            <el-tag
              v-for="item in hotCountryOptions"
              :key="item.code"
              class="quick-tag"
              :class="{ 'is-active': filters.country === item.code }"
              effect="plain"
              @click="pickCountry(item.code)"
            >
              {{ item.label }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item v-if="hotCategoryOptions.length" label="热门类目：" class="quick-form-item">
          <div class="quick-tags">
            <el-tag
              v-for="item in hotCategoryOptions"
              :key="item.code"
              class="quick-tag"
              :class="{ 'is-active': filters.category === item.label }"
              effect="plain"
              @click="pickCategory(item.label)"
            >
              {{ item.label }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>
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
        <article v-for="row in workers" :key="row.workerId || row.id" class="worker-card">
          <div class="worker-top">
            <div>
              <h3>{{ row.name }}</h3>
              <p>ID: {{ row.workerId }}</p>
            </div>
            <el-tag type="success">{{ getCountryLabel(row.country) || '—' }}</el-tag>
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
import { getDemandDetailApi } from '@/api/demand';
import { createOrderApi } from '@/api/order';
import { getCountryDictApi } from '@/api/dict';
import { CATEGORY_OPTIONS, CATEGORY_PRESETS } from '@/dicts';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const route = useRoute();

const demandId = ref(route.query?.demandId ? Number(route.query.demandId) : 0);
const listLoading = ref(false);
const countryLoading = ref(false);
const workers = ref([]);
const countryOptions = ref([]);
const demandInfo = ref(null);
const demandLoadError = ref('');
const demandFallbackActive = ref(false);
const createLoadingMap = reactive({});
const filters = reactive({
  country: '',
  category: '',
});

const categoryOptions = computed(() => {
  const fromDict = Array.isArray(CATEGORY_OPTIONS) ? CATEGORY_OPTIONS : [];
  if (fromDict.length > 0) {
    return fromDict
      .map((item) => ({
        code: String(item?.code || item?.label || '').trim(),
        label: String(item?.label || item?.code || '').trim(),
        sort: Number(item?.sort || 0),
        hot: Boolean(item?.hot),
      }))
      .filter((item) => item.code && item.label)
      .sort((a, b) => a.sort - b.sort);
  }
  return (Array.isArray(CATEGORY_PRESETS) ? CATEGORY_PRESETS : [])
    .map((label, index) => ({
      code: String(label),
      label: String(label),
      sort: (index + 1) * 10,
      hot: index < 6,
    }));
});

const hotCategoryOptions = computed(() => {
  const list = categoryOptions.value;
  const hotList = list.filter((item) => item.hot);
  return (hotList.length > 0 ? hotList : list).slice(0, 8);
});

const hotCountryOptions = computed(() => {
  const list = countryOptions.value;
  const hotList = list.filter((item) => item.hot);
  return (hotList.length > 0 ? hotList : list).slice(0, 8);
});

const countryLabelMap = computed(() => {
  const map = new Map();
  countryOptions.value.forEach((item) => {
    if (item.code) map.set(item.code.toUpperCase(), item.label);
    if (item.label) map.set(item.label, item.label);
  });
  return map;
});

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
  async (value) => {
    demandId.value = value ? Number(value) : 0;
    await syncDemandContextAndWorkers();
  },
);

function parseCountryExtra(extraJson) {
  if (!extraJson) return {};
  try {
    const parsed = JSON.parse(extraJson);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

async function loadCountries() {
  countryLoading.value = true;
  try {
    const data = await getCountryDictApi();
    const list = Array.isArray(data) ? data : [];
    const mapped = list
      .map((item, index) => {
        const code = String(item?.dict_code || item?.dictCode || '').trim().toUpperCase();
        const label = String(item?.dict_label || item?.dictLabel || '').trim();
        const sortNo = Number(item?.sort_no ?? item?.sortNo ?? (index + 1) * 10);
        const extra = parseCountryExtra(item?.extra_json || item?.extraJson || '');
        return {
          code,
          label,
          sortNo,
          hot: Boolean(extra?.hot),
        };
      })
      .filter((item) => /^[A-Z]{2}$/.test(item.code) && item.label);

    const dedup = new Map();
    mapped.forEach((item) => {
      if (!dedup.has(item.code)) {
        dedup.set(item.code, item);
      }
    });
    countryOptions.value = Array.from(dedup.values()).sort((a, b) => {
      if (a.hot !== b.hot) return a.hot ? -1 : 1;
      if (a.sortNo !== b.sortNo) return a.sortNo - b.sortNo;
      return a.code.localeCompare(b.code);
    });
  } catch {
    countryOptions.value = [];
    ElMessage.error('国家字典加载失败，请稍后重试');
  } finally {
    countryLoading.value = false;
  }
}

async function loadWorkers(options = {}) {
  const allowDemandFallback = Boolean(options?.allowDemandFallback);
  listLoading.value = true;
  try {
    const query = {
      country: String(filters.country || '').trim(),
      category: String(filters.category || '').trim(),
    };
    const filtered = await getWorkerListApi(query);
    if (
      allowDemandFallback
      && demandId.value
      && filtered.length === 0
      && (query.country || query.category)
    ) {
      demandFallbackActive.value = true;
      workers.value = await getWorkerListApi({
        country: '',
        category: '',
      });
      return;
    }
    demandFallbackActive.value = false;
    workers.value = filtered;
  } catch {
    demandFallbackActive.value = false;
    workers.value = [];
  } finally {
    listLoading.value = false;
  }
}

function getCountryLabel(raw) {
  const text = String(raw || '').trim();
  if (!text) return '';
  return countryLabelMap.value.get(text.toUpperCase()) || countryLabelMap.value.get(text) || text;
}

function handleFilterChange() {
  loadWorkers();
}

function pickCountry(code) {
  filters.country = code;
  loadWorkers();
}

function pickCategory(label) {
  filters.category = label;
  loadWorkers();
}

function resolveDemandCategoryLabel(rawCategory) {
  const text = String(rawCategory || '').trim();
  if (!text) return '';
  const byCode = categoryOptions.value.find((item) => String(item.code || '').trim() === text);
  if (byCode?.label) return byCode.label;
  return text;
}

async function syncDemandContextAndWorkers() {
  demandInfo.value = null;
  demandLoadError.value = '';
  if (!demandId.value) {
    await loadWorkers();
    return;
  }
  try {
    const data = await getDemandDetailApi(demandId.value);
    demandInfo.value = data || null;

    const demandCountry = String(data?.targetCountry || '').trim().toUpperCase();
    const demandCategory = resolveDemandCategoryLabel(data?.category);
    if (demandCountry) {
      filters.country = demandCountry;
    }
    if (demandCategory) {
      filters.category = demandCategory;
    }
  } catch {
    demandLoadError.value = '需求信息加载失败，已切换为手动筛选模式';
  }
  await loadWorkers({ allowDemandFallback: true });
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

function getOrderAmount(worker) {
  const demandBudget = Number(demandInfo.value?.budget || 0);
  if (demandBudget > 0) {
    return demandBudget;
  }
  return getEstimatedAmount(worker);
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
      amount: getOrderAmount(worker),
    });
    const orderId = data?.orderId || data?.id || data;
    if (!orderId) {
      throw new Error('未获取到订单ID');
    }
    ElMessage.success('订单创建成功');
    router.push(`/order/${orderId}`);
  } catch (error) {
    ElMessage.error(error?.message || '创建订单失败，请稍后重试');
  } finally {
    createLoadingMap[workerId] = false;
  }
}

onMounted(async () => {
  await loadCountries();
  await syncDemandContextAndWorkers();
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

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: end;
}

.filter-actions {
  display: flex;
  gap: 8px;
  padding-bottom: 2px;
}

.quick-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-form-item {
  margin-bottom: 8px;
}

:deep(.quick-form-item .el-form-item__label) {
  color: #4f6779;
  font-size: 12px;
  font-weight: 600;
}

:deep(.quick-form-item .el-form-item__content) {
  line-height: normal;
}

.quick-tag {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

:deep(.quick-tag.el-tag) {
  border-color: #b7cfe2;
  color: #1c4a69;
  background: #f2f8fd;
}

:deep(.quick-tag.el-tag:hover) {
  border-color: #7fa9c6;
  background: #e7f2fb;
  color: #133f5d;
  transform: translateY(-1px);
}

:deep(.quick-tag.is-active.el-tag) {
  border-color: #0f766e;
  color: #ffffff;
  background: linear-gradient(120deg, #0f766e 0%, #0f8b80 100%);
  box-shadow: 0 6px 16px rgba(15, 118, 110, 0.28);
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

@media (max-width: 920px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    padding-bottom: 0;
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
