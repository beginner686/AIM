<template>
  <div class="worker-page">
    <section class="hero-strip reveal-up">
      <div class="orb orb-a" />
      <div class="orb orb-b" />
      
      <div class="hero-left hero-content">
        <p class="kicker">{{ t('workerPool.kicker') }}</p>
        <h1>
          <span class="gradient-text">{{ t('workerPool.title') }}</span>
        </h1>
        <p class="sub">{{ t('workerPool.subtitle') }}</p>
        <div class="hero-tags reveal-up delay-1">
          <span class="chip">{{ t('workerPool.onlineResources') }} {{ displayedWorkers.length }}</span>
          <span class="chip">{{ t('workerPool.avgRating') }} {{ avgRating.toFixed(1) }}</span>
          <span class="chip">{{ t('workerPool.demandId') }} {{ demandId || t('workerPool.notProvided') }}</span>
        </div>
      </div>

      <div class="hero-right hero-content reveal-up delay-1">
        <el-button class="hero-btn-primary" @click="$router.push('/publish-demand')">{{ t('workerPool.publishNewDemand') }}</el-button>
        <el-button class="hero-btn-ghost" @click="resetFilters">{{ t('workerPool.clearFilters') }}</el-button>
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

    <el-card class="filter-card reveal-up delay-1" shadow="never">
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

    <section class="metric-grid reveal-up delay-1">
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

    <el-card class="list-card reveal-up delay-1" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('workerPool.listTitle') }}</h2>
          <div class="list-head-actions">
            <el-input
              v-model.trim="workerKeyword"
              clearable
              :placeholder="t('workerPool.searchWorkerPlaceholder')"
              class="worker-search-input"
            />
            <span class="count">{{ t('workerPool.totalWorkers', { count: displayedWorkers.length }) }}</span>
          </div>
        </div>
      </template>

      <div v-if="listLoading" class="loading-wrap">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty
        v-else-if="displayedWorkers.length === 0"
        :description="workerKeyword ? t('workerPool.searchEmpty') : t('workerPool.empty')"
        :image-size="88"
      />

      <div v-else class="worker-grid">
        <article v-for="row in displayedWorkers" :key="row.workerId || row.id" class="worker-card reveal-up">
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
	            <el-button class="list-btn" @click="goPublishDemandWithWorker(row)">
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
const workerKeyword = ref('');
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

const displayedWorkers = computed(() => {
  const keyword = String(workerKeyword.value || '').trim().toLowerCase();
  if (!keyword) return workers.value;
  return workers.value.filter((item) => {
    const workerId = String(getWorkerProfileId(item));
    const workerName = String(item?.name || '').toLowerCase();
    return workerName.includes(keyword) || workerId.includes(keyword);
  });
});

const avgRating = computed(() => {
  if (displayedWorkers.value.length === 0) return 0;
  const total = displayedWorkers.value.reduce((sum, item) => sum + Number(item.rating || 0), 0);
  return total / displayedWorkers.value.length;
});

const avgQuote = computed(() => {
  if (displayedWorkers.value.length === 0) return 0;
  const total = displayedWorkers.value.reduce((sum, item) => sum + getEstimatedAmount(item), 0);
  return total / displayedWorkers.value.length;
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
    router.push(`/order/checkout/${orderId}`);
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
  justify-content: space-between;
  align-items: center;
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

.kicker {
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

.sub {
  margin: 10px 0 0;
  color: rgba(229, 238, 255, 0.9);
  font-size: 14.5px;
  line-height: 1.6;
}

.hero-tags {
  margin-top: 14px;
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
  gap: 12px;
  align-items: center;
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

.filter-card,
.list-card {
  border-radius: 18px;
  border: 1px solid var(--border-light);
  background: var(--surface);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(27, 49, 90, 0.04);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.section-head h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #102449;
  letter-spacing: -0.01em;
}

.count {
  color: #65819e;
  font-size: 13.5px;
}

.list-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.worker-search-input {
  width: 280px;
  max-width: 100%;
}

.filter-form {
  margin-bottom: 8px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 16px;
  align-items: end;
}

.filter-actions {
  display: flex;
  gap: 12px;
  padding-bottom: 2px;
}

.filter-actions :deep(.el-button) {
  border-radius: 999px;
  padding: 10px 22px;
  font-weight: 600;
}

.filter-actions :deep(.el-button--primary) {
  background: linear-gradient(120deg, #1f4f99, #183e7a);
  border: none;
  box-shadow: 0 8px 16px rgba(31, 79, 153, 0.2);
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
  gap: 16px;
}

.metric {
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 16px 18px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 24px rgba(27, 49, 90, 0.03);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.metric:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(27, 49, 90, 0.05);
}

.metric .label {
  margin: 0;
  color: #6a8396;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.metric .value {
  margin: 6px 0 0;
  color: #102c42;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.loading-wrap {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.worker-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.worker-card {
  border: 1px solid rgba(216, 228, 246, 0.85);
  border-radius: 16px;
  padding: 16px 18px;
  background: #fdfdff;
  box-shadow: 0 12px 24px rgba(31, 57, 107, 0.03);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.worker-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 36px rgba(31, 57, 107, 0.08);
  border-color: #bad2f9;
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
  margin-top: 10px;
  font-size: 12.5px;
  color: #0d9488;
  font-weight: 500;
}

.list-btn {
  border-radius: 999px;
  font-weight: 600;
  padding: 8px 18px;
}

:deep(.el-button--primary) {
  border-radius: 999px;
  background: linear-gradient(120deg, #1f4f99, #183e7a);
  border: none;
  box-shadow: 0 4px 10px rgba(31, 79, 153, 0.2);
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
  .hero-strip h1 {
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
