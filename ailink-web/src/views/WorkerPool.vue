<template>
  <div class="worker-page">
    <section class="hero">
      <div class="hero-left">
        <p class="kicker">{{ t('workerPool.kicker') }}</p>
        <h1>{{ t('workerPool.title') }}</h1>
        <p class="sub">{{ t('workerPool.subtitle') }}</p>
        <div class="hero-tags">
          <span class="chip">{{ t('workerPool.onlineResources') }} {{ workers.length }}</span>
          <span class="chip">{{ t('workerPool.avgRating') }} {{ avgRating.toFixed(1) }}</span>
          <span class="chip">{{ t('workerPool.demandId') }} {{ demandId || t('workerPool.notProvided') }}</span>
        </div>
      </div>

      <div class="hero-right">
        <el-button type="primary" @click="$router.push('/publish-demand')">{{ t('workerPool.publishNewDemand') }}</el-button>
        <el-button @click="resetFilters">{{ t('workerPool.clearFilters') }}</el-button>
      </div>
    </section>

    <el-alert
      v-if="!demandId"
      :title="t('workerPool.alertDemandIdMissing')"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-alert
      v-else-if="demandInfo && !isDemandOwner"
      :title="t('workerPool.alertNotDemandOwner')"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-alert
      v-else-if="demandInfo"
      :title="demandInfoAlertTitle"
      type="info"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="preferredWorkerAlertTitle"
      :title="preferredWorkerAlertTitle"
      type="success"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="demandLoadError"
      :title="demandLoadError"
      type="error"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="demandFallbackActive"
      :title="t('workerPool.alertFallback')"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card class="filter-card" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('workerPool.filterTitle') }}</h2>
          <span class="count">{{ t('workerPool.filterHint') }}</span>
        </div>
      </template>

      <el-form :model="filters" class="filter-form" label-width="76px">
        <div class="filter-grid">
          <el-form-item :label="t('workerPool.fieldCountry')">
            <el-select
              v-model="filters.country"
              filterable
              clearable
              :placeholder="t('workerPool.placeholderCountry')"
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

          <el-form-item :label="t('workerPool.fieldCategory')">
            <el-select
              v-model="filters.category"
              filterable
              clearable
              :placeholder="t('workerPool.placeholderCategory')"
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
            <el-button type="primary" :loading="listLoading" @click="loadWorkers">{{ t('workerPool.search') }}</el-button>
            <el-button @click="resetFilters">{{ t('workerPool.reset') }}</el-button>
          </div>
        </div>
        <el-form-item v-if="hotCountryOptions.length" :label="t('workerPool.hotCountries')" class="quick-form-item">
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

        <el-form-item v-if="hotCategoryOptions.length" :label="t('workerPool.hotCategories')" class="quick-form-item">
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
        <p class="label">{{ t('workerPool.metricResults') }}</p>
        <p class="value">{{ workers.length }}</p>
      </article>
      <article class="metric">
        <p class="label">{{ t('workerPool.metricAvgRating') }}</p>
        <p class="value">{{ avgRating.toFixed(1) }}</p>
      </article>
      <article class="metric">
        <p class="label">{{ t('workerPool.metricAvgQuote') }}</p>
        <p class="value">{{ formatMoney(avgQuote) }}</p>
      </article>
    </section>

    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('workerPool.listTitle') }}</h2>
          <span class="count">{{ t('workerPool.totalWorkers', { count: workers.length }) }}</span>
        </div>
      </template>

      <div v-if="listLoading" class="loading-wrap">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty v-else-if="workers.length === 0" :description="t('workerPool.empty')" :image-size="88" />

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
		            <span>{{ t('workerPool.deals') }} {{ row.dealCount }}</span>
		            <span>{{ t('workerPool.rating') }} {{ row.rating.toFixed(1) }}</span>
		            <span>{{ t('workerPool.estimatedQuote') }} {{ formatMoney(getEstimatedAmount(row)) }}</span>
		          </div>
	          <div v-if="isPreferredWorker(row)" class="preferred-tip">
	            {{ t('workerPool.preferredWorkerTag') }}
	          </div>

	          <div class="action-row">
	            <el-button
	              type="primary"
	              :disabled="!demandId || !isDemandOwner || !canCreateOrderWithWorker(row)"
	              :loading="Boolean(createLoadingMap[row.workerId || row.id])"
	              @click="handleCreateOrder(row)"
		            >
		              {{ t('workerPool.createOrder') }}
		            </el-button>
	            <el-button @click="goPublishDemandWithWorker(row)">
	              {{ t('workerPool.bindAndPublishDemand') }}
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
import { useI18n } from 'vue-i18n';
import { getWorkerListApi } from '@/api/worker';
import { getDemandDetailApi } from '@/api/demand';
import { createOrderApi } from '@/api/order';
import { getCountryDictApi } from '@/api/dict';
import { CATEGORY_OPTIONS, CATEGORY_PRESETS } from '@/dicts';
import { formatMoney } from '@/utils/format';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const { t, locale } = useI18n();

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

const CATEGORY_I18N_KEY_MAP = {
  '翻译本地化': 'categoryLabel.translationLocalization',
  '远程助理': 'categoryLabel.remoteAssistant',
  '视频剪辑': 'categoryLabel.videoEditing',
  '海外投放': 'categoryLabel.overseasAds',
  '客服支持': 'categoryLabel.customerSupport',
  '平面设计': 'categoryLabel.graphicDesign',
  'UI/UX设计': 'categoryLabel.uiuxDesign',
  '文案策划': 'categoryLabel.copywriting',
  '社媒运营': 'categoryLabel.socialMediaOps',
  'SEO优化': 'categoryLabel.seoOptimization',
  '网红/KOL合作': 'categoryLabel.kolCollab',
  '网站开发': 'categoryLabel.webDevelopment',
  '电商代运营': 'categoryLabel.ecommerceOps',
  '跨境物流': 'categoryLabel.crossBorderLogistics',
  '财税服务': 'categoryLabel.financeTax',
  '海外公司注册': 'categoryLabel.companyRegistration',
  '法律咨询': 'categoryLabel.legalConsulting',
  'UI设计': 'categoryLabel.uiDesign',
  '翻译': 'categoryLabel.translation',
  'AI服务': 'categoryLabel.aiService',
};

const categoryOptions = computed(() => {
  const fromDict = Array.isArray(CATEGORY_OPTIONS) ? CATEGORY_OPTIONS : [];
  if (fromDict.length > 0) {
    return fromDict
      .map((item) => ({
        code: String(item?.code || item?.label || '').trim(),
        label: localizeCategoryLabel(String(item?.label || item?.code || '').trim()),
        sort: Number(item?.sort || 0),
        hot: Boolean(item?.hot),
      }))
      .filter((item) => item.code && item.label)
      .sort((a, b) => a.sort - b.sort);
  }
  return (Array.isArray(CATEGORY_PRESETS) ? CATEGORY_PRESETS : [])
    .map((label, index) => ({
      code: String(label),
      label: localizeCategoryLabel(String(label)),
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

const currentUserId = computed(() => Number(userStore.userInfo?.id || 0));
const demandOwnerId = computed(() => Number(demandInfo.value?.userId || 0));
const preferredWorkerProfileId = computed(() => Number(demandInfo.value?.preferredWorkerProfileId || 0));
const isDemandOwner = computed(() => {
  if (!demandId.value) return false;
  if (!demandOwnerId.value) return true;
  return currentUserId.value > 0 && currentUserId.value === demandOwnerId.value;
});

const demandInfoAlertTitle = computed(() => {
  if (!demandInfo.value) return '';
  return t('workerPool.alertCurrentDemand', {
    id: demandInfo.value.id,
    country: getCountryLabel(demandInfo.value.targetCountry) || demandInfo.value.targetCountry || '—',
    category: resolveDemandCategoryLabel(demandInfo.value.category) || '—',
  });
});
const preferredWorkerAlertTitle = computed(() => {
  if (!demandInfo.value || !preferredWorkerProfileId.value) return '';
  const preferredName = String(demandInfo.value?.preferredWorkerNameSnapshot || '').trim();
  return t('workerPool.alertPreferredWorkerBound', {
    id: preferredWorkerProfileId.value,
    name: preferredName || `#${preferredWorkerProfileId.value}`,
  });
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

function localizeCountryLabel(code, fallbackLabel) {
  const regionCode = String(code || '').trim().toUpperCase();
  const fallback = String(fallbackLabel || '').trim();
  if (!regionCode) return fallback;
  try {
    const displayNames = new Intl.DisplayNames([locale.value], { type: 'region' });
    return displayNames.of(regionCode) || fallback || regionCode;
  } catch {
    return fallback || regionCode;
  }
}

function localizeCategoryLabel(label) {
  const text = String(label || '').trim();
  if (!text) return '';
  const i18nKey = CATEGORY_I18N_KEY_MAP[text];
  return i18nKey ? t(i18nKey) : text;
}

async function loadCountries() {
  countryLoading.value = true;
  try {
    const data = await getCountryDictApi();
    const list = Array.isArray(data) ? data : [];
    const mapped = list
      .map((item, index) => {
        const code = String(item?.dict_code || item?.dictCode || '').trim().toUpperCase();
        const rawLabel = String(item?.dict_label || item?.dictLabel || '').trim();
        const label = localizeCountryLabel(code, rawLabel);
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
    ElMessage.error(t('workerPool.countryLoadFailed'));
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
    demandLoadError.value = t('workerPool.demandLoadFailed');
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

function getWorkerProfileId(worker) {
  return Number(worker?.workerId || worker?.id || 0);
}

function isPreferredWorker(worker) {
  if (!preferredWorkerProfileId.value) return false;
  return getWorkerProfileId(worker) === preferredWorkerProfileId.value;
}

function canCreateOrderWithWorker(worker) {
  if (!preferredWorkerProfileId.value) return true;
  return isPreferredWorker(worker);
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
  if (!source) return [t('workerPool.unlabeled')];
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

function goPublishDemandWithWorker(worker) {
  const workerProfileId = getWorkerProfileId(worker);
  if (!workerProfileId) {
    ElMessage.error(t('workerPool.invalidWorkerId'));
    return;
  }
  router.push({
    path: '/publish-demand',
    query: {
      preferredWorkerProfileId: String(workerProfileId),
      preferredWorkerName: String(worker?.name || ''),
      preferredWorkerCountry: String(worker?.country || ''),
      preferredWorkerCategory: String(worker?.category || ''),
    },
  });
}

async function handleCreateOrder(worker) {
  if (!demandId.value) {
    ElMessage.warning(t('workerPool.createNeedDemandId'));
    return;
  }
  if (!isDemandOwner.value) {
    ElMessage.warning(t('workerPool.createOnlyOwner'));
    return;
  }
  const workerId = getWorkerProfileId(worker);
  if (!workerId) {
    ElMessage.error(t('workerPool.invalidWorkerId'));
    return;
  }
  if (!canCreateOrderWithWorker(worker)) {
    ElMessage.warning(t('workerPool.createOnlyPreferredWorker'));
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
      throw new Error(t('workerPool.orderIdMissing'));
    }
    ElMessage.success(t('workerPool.createSuccess'));
    router.push(`/order/${orderId}`);
  } catch (error) {
    const message = String(error?.message || '').trim();
    if (message.toLowerCase().includes('only demand owner can create order')) {
      ElMessage.error(t('workerPool.createOnlyOwner'));
      return;
    }
    if (message.toLowerCase().includes('bound to a preferred worker')) {
      ElMessage.error(t('workerPool.createOnlyPreferredWorker'));
      return;
    }
    ElMessage.error(message || t('workerPool.createFailed'));
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
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preferred-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #0f766e;
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
