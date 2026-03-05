<template>
  <div class="publish-page">
    <section class="hero-strip reveal-up">
      <div class="orb orb-a" />
      <div class="orb orb-b" />
      <div class="hero-content">
        <p class="kicker">{{ t('publishDemand.kicker') }}</p>
        <h1>
          <span class="gradient-text">{{ t('publishDemand.poolPageTitle') }}</span>
        </h1>
        <p class="sub">{{ t('publishDemand.poolPageSubtitle') }}</p>
      </div>
      <div class="hero-actions reveal-up delay-1">
        <el-button class="hero-btn-primary" @click="openCreateDialog">{{ t('publishDemand.iHaveDemand') }}</el-button>
      </div>
    </section>

    <el-alert
      v-if="hasPreferredWorker"
      class="preferred-worker-alert"
      type="success"
      :closable="false"
      show-icon
    >
      <template #title>
        {{ t('publishDemand.preferredWorkerTitle', { name: preferredWorkerDisplayName }) }}
      </template>
      <div class="preferred-worker-body">
        <span>{{ t('publishDemand.preferredWorkerHint', { id: preferredWorkerProfileId }) }}</span>
        <span v-if="preferredWorkerCountry">{{ t('publishDemand.preferredWorkerCountry', { country: preferredWorkerCountry }) }}</span>
        <span v-if="preferredWorkerCategory">{{ t('publishDemand.preferredWorkerCategory', { category: preferredWorkerCategory }) }}</span>
        <el-button link type="primary" @click="clearPreferredWorker">{{ t('publishDemand.clearPreferredWorker') }}</el-button>
      </div>
    </el-alert>

    <section class="metric-grid reveal-up delay-1">
      <article class="metric">
        <p class="label">{{ t('publishDemand.poolMetricTotal') }}</p>
        <p class="value">{{ demandPool.length }}</p>
      </article>
      <article class="metric">
        <p class="label">{{ t('publishDemand.poolMetricOpen') }}</p>
        <p class="value">{{ openDemandCount }}</p>
      </article>
      <article class="metric">
        <p class="label">{{ t('publishDemand.poolMetricActive') }}</p>
        <p class="value">{{ activeDemandCount }}</p>
      </article>
      <article class="metric">
        <p class="label">{{ t('publishDemand.poolMetricShown') }}</p>
        <p class="value">{{ filteredDemandPool.length }}</p>
      </article>
    </section>

    <el-card class="filter-card reveal-up delay-1" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('publishDemand.poolFilterTitle') }}</h2>
          <span class="section-tip">{{ t('publishDemand.poolFilterHint') }}</span>
        </div>
      </template>
      <div class="pool-filter-row">
        <el-input v-model.trim="poolKeyword" clearable :placeholder="t('publishDemand.poolSearchPlaceholder')" class="pool-filter-item" />
        <el-select v-model="poolCountry" clearable filterable :placeholder="t('publishDemand.fieldCountry')" class="pool-filter-item">
          <el-option v-for="item in countryOptions" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
        <el-select v-model="poolStatus" clearable :placeholder="t('publishDemand.poolStatusPlaceholder')" class="pool-filter-item">
          <el-option :label="t('publishDemand.poolAllStatus')" value="" />
          <el-option v-for="item in demandStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button @click="clearPoolFilters">{{ t('publishDemand.reset') }}</el-button>
        <el-button type="primary" @click="openCreateDialog">{{ t('publishDemand.iHaveDemand') }}</el-button>
      </div>
    </el-card>

    <el-card class="list-card reveal-up delay-1" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('publishDemand.poolListTitle') }}</h2>
          <span class="section-tip">{{ t('publishDemand.poolListCount', { count: filteredDemandPool.length }) }}</span>
        </div>
      </template>
      <div v-if="poolLoading" class="loading-wrap">
        <el-skeleton :rows="5" animated />
      </div>
      <el-empty v-else-if="filteredDemandPool.length === 0" :description="t('publishDemand.poolEmpty')" :image-size="88" />
      <div v-else class="demand-grid">
        <article v-for="item in filteredDemandPool" :key="item.id" class="demand-card">
          <div class="demand-top">
            <h3>{{ item.title || resolveDemandCategoryLabel(item.category) || `#${item.id}` }}</h3>
            <el-tag :type="demandStatusType(item.status)">{{ demandStatusText(item.status) }}</el-tag>
          </div>
          <p class="demand-desc">{{ item.description || t('publishDemand.noDescription') }}</p>
          <div class="demand-meta">
            <span>{{ t('publishDemand.poolMetaId') }} {{ item.id }}</span>
            <span>{{ t('publishDemand.poolMetaCountry') }} {{ getCountryLabel(item.targetCountry || item.country) || '—' }}</span>
            <span>{{ t('publishDemand.poolMetaCategory') }} {{ resolveDemandCategoryLabel(item.category) || '—' }}</span>
            <span>{{ t('publishDemand.poolMetaBudget') }} {{ formatMoney(item.budget) }}</span>
          </div>
          <div class="demand-actions">
            <el-button size="small" @click="goWorkerPool(item.id)">{{ t('publishDemand.poolMatchWorker') }}</el-button>
          </div>
        </article>
      </div>
    </el-card>

    <el-dialog
      v-model="createDialogVisible"
      :title="t('publishDemand.dialogTitle')"
      width="1180px"
      class="publish-create-dialog"
      destroy-on-close
    >
      <section class="content-grid">
        <el-card class="form-card" shadow="never">
          <template #header>
            <div class="section-head">
              <h2>{{ t('publishDemand.sectionInfo') }}</h2>
              <span class="section-tip">{{ t('publishDemand.requiredTip') }}</span>
            </div>
          </template>

          <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
            <div class="two-col">
              <el-form-item :label="t('publishDemand.fieldTitle')" prop="title">
                <el-input v-model="formData.title" maxlength="50" :placeholder="t('publishDemand.placeholderTitle')" />
              </el-form-item>

              <el-form-item :label="t('publishDemand.fieldCategory')" prop="category">
                <el-autocomplete
                  v-model="formData.category"
                  class="category-autocomplete"
                  popper-class="category-autocomplete-popper"
                  :fetch-suggestions="queryCategorySuggestions"
                  :trigger-on-focus="true"
                  :highlight-first-item="true"
                  maxlength="50"
                  clearable
                  :placeholder="t('publishDemand.placeholderCategory')"
                  @input="handleCategoryInput"
                  @select="handleCategorySelect"
                >
                  <template #default="{ item }">
                    <div class="category-suggestion" :class="{ 'is-fallback': item.isFallback }">
                      <span v-if="item.isFallback">{{ item.label }}</span>
                      <span v-else v-html="highlightKeyword(item.label, latestCategoryKeyword)"></span>
                    </div>
                  </template>
                </el-autocomplete>
              </el-form-item>
            </div>

            <div class="preset-row hot-row">
              <span class="preset-label">{{ t('publishDemand.hotCategories') }}</span>
              <el-tag
                v-for="item in hotCategoryOptions"
                :key="item.code"
                class="hot-tag"
                :class="{ 'is-active': isHotCategoryActive(item) }"
                :type="isHotCategoryActive(item) ? 'success' : 'info'"
                :effect="isHotCategoryActive(item) ? 'dark' : 'plain'"
                @click="handleHotCategoryClick(item)"
              >
                {{ item.label }}
              </el-tag>
            </div>

            <div class="two-col">
              <el-form-item :label="t('publishDemand.fieldBudget')" prop="budget">
                <el-input-number
                  v-model="formData.budget"
                  :min="0.01"
                  :step="50"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>

              <el-form-item :label="t('publishDemand.fieldCountry')" prop="country">
                <el-select
                  v-model="formData.country"
                  class="country-select"
                  filterable
                  clearable
                  :placeholder="t('publishDemand.placeholderCountry')"
                  :loading="countryLoading"
                >
                  <el-option-group
                    v-for="group in groupedCountryOptions"
                    :key="group.key"
                    :label="group.label"
                  >
                    <el-option
                      v-for="item in group.options"
                      :key="item.code"
                      :value="item.code"
                      :label="item.label"
                    >
                      <div class="country-option-item">
                        <span class="country-option-label">{{ item.label }}</span>
                        <span v-if="item.hot" class="country-option-hot">{{ t('publishDemand.recommended') }}</span>
                      </div>
                    </el-option>
                  </el-option-group>
                </el-select>
                <div v-if="!countryLoading && groupedCountryOptions.length === 0" class="country-empty-tip">
                  {{ t('publishDemand.countryEmptyTip') }}
                </div>
              </el-form-item>
            </div>

            <el-form-item :label="t('publishDemand.fieldDescription')" prop="description">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="6"
                maxlength="500"
                show-word-limit
                :placeholder="t('publishDemand.placeholderDescription')"
              />
            </el-form-item>

            <div class="action-row">
              <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ t('publishDemand.submit') }}</el-button>
              <el-button @click="resetForm">{{ t('publishDemand.reset') }}</el-button>
            </div>
          </el-form>
        </el-card>

        <div class="right-stack">
          <el-card class="side-card" shadow="never">
            <template #header>
              <div class="section-head">
                <h2>{{ t('publishDemand.splitTitle') }}</h2>
              </div>
            </template>

            <div class="split-item">
              <span>{{ t('publishDemand.splitOrderBudget') }}</span>
              <strong>{{ formatMoney(budgetAmount) }}</strong>
            </div>
            <div class="split-item">
              <span>{{ t('publishDemand.splitPlatformFee', { rate: formatRatePercent(platformFeeRate) }) }}</span>
              <strong>{{ formatMoney(platformFeeAmount) }}</strong>
            </div>
            <div class="split-item">
              <span>{{ t('publishDemand.splitEscrowFee', { rate: formatRatePercent(escrowFeeRate) }) }}</span>
              <strong>{{ formatMoney(escrowFeeAmount) }}</strong>
            </div>
            <div class="split-item highlight">
              <span>{{ t('publishDemand.splitWorkerIncome') }}</span>
              <strong>{{ formatMoney(workerIncomeAmount) }}</strong>
            </div>
          </el-card>

          <el-card class="side-card" shadow="never">
            <template #header>
              <div class="section-head">
                <h2>{{ t('publishDemand.tipsTitle') }}</h2>
              </div>
            </template>
            <ul class="tips">
              <li>{{ t('publishDemand.tip1') }}</li>
              <li>{{ t('publishDemand.tip2') }}</li>
              <li>{{ t('publishDemand.tip3') }}</li>
              <li>{{ t('publishDemand.tip4') }}</li>
            </ul>
          </el-card>
        </div>
      </section>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { createDemandApi, getMyDemandListApi } from '@/api/demand';
import { getOrderFeeConfigApi } from '@/api/order';
import { getCountryDictApi } from '@/api/dict';
import {
  CATEGORY_OPTIONS,
  CATEGORY_PRESETS,
  DEMAND_ACTIVE_STATUSES,
  DEMAND_OPEN_STATUSES,
  DEMAND_STATUS_DICT,
  getDemandStatusLabel,
  getDemandStatusTag,
  toSelectOptions,
} from '@/dicts';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const route = useRoute();
const { t, locale } = useI18n();
const formRef = ref(null);
const submitLoading = ref(false);
const createDialogVisible = ref(false);
const preferredWorkerProfileId = ref(0);
const preferredWorkerName = ref('');
const preferredWorkerCountry = ref('');
const preferredWorkerCategory = ref('');
const poolLoading = ref(false);
const demandPool = ref([]);
const poolKeyword = ref('');
const poolCountry = ref('');
const poolStatus = ref('');
const platformFeeRate = ref(0);
const escrowFeeRate = ref(0);

const formData = reactive({
  title: '',
  category: '',
  budget: null,
  country: '',
  description: '',
});

const categoryPresets = CATEGORY_PRESETS;
const FALLBACK_OTHER_CODE = 'other';
const REGION_ORDER = ['ASIA', 'EUROPE', 'OTHER'];
const REGION_KEY_MAP = {
  ASIA: 'asia',
  EUROPE: 'europe',
  OTHER: 'other',
};
const fallbackOtherLabel = computed(() => t('publishDemand.fallbackOtherLabel'));
const fallbackGuideText = computed(() => t('publishDemand.fallbackGuideText'));
const latestCategoryKeyword = ref('');
const selectedCategoryCode = ref('');

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

const countryOptions = ref([]);
const countryLoading = ref(false);
const demandStatusOptions = computed(() => toSelectOptions(DEMAND_STATUS_DICT));
const countryLabelMap = computed(() => {
  const map = new Map();
  countryOptions.value.forEach((item) => {
    const code = String(item?.code || '').trim().toUpperCase();
    const label = String(item?.label || '').trim();
    if (code) map.set(code, label);
    if (label) map.set(label, label);
  });
  return map;
});

const categoryOptionList = computed(() => {
  const fromOptions = Array.isArray(CATEGORY_OPTIONS) ? CATEGORY_OPTIONS : [];
  if (fromOptions.length > 0) {
    return fromOptions
      .map((item) => ({
        code: String(item?.code || item?.label || '').trim(),
        label: localizeCategoryLabel(String(item?.label || item?.code || '').trim()),
        sort: Number(item?.sort || 0),
        hot: Boolean(item?.hot),
        isFallback: false,
      }))
      .filter((item) => item.code && item.label);
  }

  return (Array.isArray(categoryPresets) ? categoryPresets : [])
    .map((label, index) => ({
      code: String(label),
      label: localizeCategoryLabel(String(label)),
      sort: (index + 1) * 10,
      hot: index < 6,
      isFallback: false,
    }))
    .filter((item) => item.code && item.label);
});

const hotCategoryOptions = computed(() => {
  const list = categoryOptionList.value;
  const hotList = list.filter((item) => item.hot);
  if (hotList.length > 0) {
    return hotList
      .slice()
      .sort((a, b) => a.sort - b.sort)
      .slice(0, 6);
  }
  return list
    .slice()
    .sort((a, b) => a.sort - b.sort)
    .slice(0, 6);
});

const groupedCountryOptions = computed(() => {
  const list = countryOptions.value;
  const groups = [];

  const hotOptions = list.filter((item) => item.hot);
  if (hotOptions.length > 0) {
    groups.push({
      key: 'hot',
      label: t('publishDemand.countryGroupHot'),
      options: hotOptions,
    });
  }

  const remain = hotOptions.length > 0 ? list.filter((item) => !item.hot) : list;
  REGION_ORDER.forEach((region) => {
    const options = remain.filter((item) => item.region === region);
    if (options.length > 0) {
      const regionKey = REGION_KEY_MAP[region] || 'other';
      groups.push({
        key: `region_${region}`,
        label: t(`publishDemand.region.${regionKey}`),
        options,
      });
    }
  });

  return groups;
});

const rules = computed(() => ({
  title: [{ required: true, message: t('publishDemand.ruleTitleRequired'), trigger: 'blur' }],
  category: [{ required: true, message: t('publishDemand.ruleCategoryRequired'), trigger: 'blur' }],
  budget: [
    { required: true, message: t('publishDemand.ruleBudgetRequired'), trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined || Number(value) <= 0) {
          callback(new Error(t('publishDemand.ruleBudgetPositive')));
          return;
        }
        callback();
      },
      trigger: 'change',
    },
  ],
  country: [{ required: true, message: t('publishDemand.ruleCountryRequired'), trigger: 'change' }],
  description: [{ required: true, message: t('publishDemand.ruleDescriptionRequired'), trigger: 'blur' }],
}));

const budgetAmount = computed(() => Number(formData.budget || 0));
const platformFeeAmount = computed(() => round2(budgetAmount.value * Number(platformFeeRate.value || 0)));
const escrowFeeAmount = computed(() => round2(budgetAmount.value * Number(escrowFeeRate.value || 0)));
const workerIncomeAmount = computed(() => round2(Math.max(0, budgetAmount.value - platformFeeAmount.value - escrowFeeAmount.value)));
const hasPreferredWorker = computed(() => preferredWorkerProfileId.value > 0);
const filteredDemandPool = computed(() => {
  const keyword = String(poolKeyword.value || '').trim().toLowerCase();
  const country = String(poolCountry.value || '').trim().toUpperCase();
  const status = String(poolStatus.value || '').trim().toUpperCase();
  return demandPool.value.filter((item) => {
    const itemCountry = String(item?.targetCountry || item?.country || '').trim().toUpperCase();
    const itemStatus = normalizeDemandStatus(item?.status);
    const title = String(item?.title || '').toLowerCase();
    const category = String(item?.category || '').toLowerCase();
    const description = String(item?.description || '').toLowerCase();
    const idText = String(item?.id || '');
    const keywordMatch = !keyword
      || title.includes(keyword)
      || category.includes(keyword)
      || description.includes(keyword)
      || idText.includes(keyword);
    const countryMatch = !country || itemCountry === country;
    const statusMatch = !status || itemStatus === status;
    return keywordMatch && countryMatch && statusMatch;
  });
});
const openDemandCount = computed(() => demandPool.value.filter((item) => isOpenDemand(item)).length);
const activeDemandCount = computed(() => demandPool.value.filter((item) => isActiveDemand(item)).length);
const preferredWorkerDisplayName = computed(() => {
  const name = String(preferredWorkerName.value || '').trim();
  if (name) return name;
  return `#${preferredWorkerProfileId.value}`;
});

function round2(value) {
  return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
}

function formatRatePercent(rate) {
  const value = Number(rate || 0);
  if (!Number.isFinite(value) || value <= 0) {
    return '0';
  }
  return (value * 100).toFixed(2);
}

function queryCategorySuggestions(queryString, cb) {
  const keyword = String(queryString || '').trim();
  latestCategoryKeyword.value = keyword;
  const normalized = keyword.toLowerCase();

  const filtered = categoryOptionList.value
    .filter((item) => {
      if (!normalized) return true;
      return String(item.label).toLowerCase().includes(normalized);
    })
    .slice(0, 8);

  if (filtered.length > 0) {
    cb(filtered);
    return;
  }

  cb([
    {
      code: FALLBACK_OTHER_CODE,
      label: fallbackGuideText.value,
      value: fallbackGuideText.value,
      isFallback: true,
    },
  ]);
}

function handleCategorySelect(item) {
  if (!item) return;
  if (item.isFallback || item.code === FALLBACK_OTHER_CODE) {
    selectedCategoryCode.value = FALLBACK_OTHER_CODE;
    formData.category = fallbackOtherLabel.value;
    return;
  }
  selectedCategoryCode.value = item.code || '';
  formData.category = item.label || '';
}

function handleCategoryInput(value) {
  const text = String(value || '').trim();
  if (!text) {
    selectedCategoryCode.value = '';
    return;
  }
  if (text === fallbackOtherLabel.value) {
    selectedCategoryCode.value = FALLBACK_OTHER_CODE;
    return;
  }
  const matched = categoryOptionList.value.find((item) => item.label === text);
  selectedCategoryCode.value = matched?.code || '';
}

function handleHotCategoryClick(item) {
  selectedCategoryCode.value = item?.code || '';
  formData.category = item?.label || '';
}

function isHotCategoryActive(item) {
  return String(formData.category || '').trim() === String(item?.label || '').trim();
}

function highlightKeyword(text, keyword) {
  const source = String(text || '');
  const key = String(keyword || '').trim();
  if (!key) {
    return escapeHtml(source);
  }
  const escapedKeyword = escapeRegExp(key);
  return escapeHtml(source).replace(
    new RegExp(`(${escapedKeyword})`, 'ig'),
    '<span class="match-highlight">$1</span>',
  );
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function escapeHtml(value) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function parseCountryExtra(extraJson) {
  if (!extraJson) return {};
  try {
    const parsed = JSON.parse(extraJson);
    if (parsed && typeof parsed === 'object') {
      return parsed;
    }
    return {};
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

function resolveDemandCategoryLabel(rawCategory) {
  const text = String(rawCategory || '').trim();
  if (!text) return '';
  if (text === FALLBACK_OTHER_CODE) {
    return fallbackOtherLabel.value;
  }
  const byCode = categoryOptionList.value.find((item) => String(item.code || '').trim() === text);
  if (byCode?.label) return byCode.label;
  return localizeCategoryLabel(text);
}

function normalizeDemandStatus(status) {
  return String(status || '').trim().toUpperCase();
}

function isOpenDemand(item) {
  const status = normalizeDemandStatus(item?.status);
  return DEMAND_OPEN_STATUSES.includes(status) || status === 'OPEN';
}

function isActiveDemand(item) {
  const status = normalizeDemandStatus(item?.status);
  return DEMAND_ACTIVE_STATUSES.includes(status) || status === 'MATCHED' || status === 'IN_PROGRESS';
}

function demandStatusText(status) {
  return getDemandStatusLabel(status);
}

function demandStatusType(status) {
  return getDemandStatusTag(status) || 'info';
}

function getCountryLabel(rawCountry) {
  const country = String(rawCountry || '').trim();
  if (!country) return '';
  if (/^[A-Za-z]{2}$/.test(country)) {
    const code = country.toUpperCase();
    return countryLabelMap.value.get(code) || localizeCountryLabel(code, code);
  }
  return countryLabelMap.value.get(country) || country;
}

function normalizeCountryOption(row, index) {
  const code = String(row?.dict_code || row?.dictCode || '').trim().toUpperCase();
  const rawLabel = String(row?.dict_label || row?.dictLabel || '').trim();
  const label = localizeCountryLabel(code, rawLabel);
  const sortNo = Number(row?.sort_no ?? row?.sortNo ?? (index + 1) * 10);
  const extraJson = row?.extra_json || row?.extraJson || '';
  const extra = parseCountryExtra(extraJson);
  const regionRaw = String(extra?.region || '').trim().toUpperCase();
  const region = regionRaw === 'ASIA' || regionRaw === 'EUROPE' ? regionRaw : 'OTHER';
  return {
    code,
    label,
    sortNo,
    hot: Boolean(extra?.hot),
    region,
  };
}

async function loadCountryOptions() {
  countryLoading.value = true;
  try {
    const data = await getCountryDictApi();
    const list = Array.isArray(data) ? data : [];
    const normalizedList = list
      .map(normalizeCountryOption)
      .filter((item) => /^[A-Z]{2}$/.test(item.code) && item.label);
    const dedupByCode = new Map();
    normalizedList.forEach((item) => {
      if (!dedupByCode.has(item.code)) {
        dedupByCode.set(item.code, item);
      }
    });
    countryOptions.value = Array.from(dedupByCode.values()).sort((a, b) => {
        if (a.hot !== b.hot) return a.hot ? -1 : 1;
        if (a.sortNo !== b.sortNo) return a.sortNo - b.sortNo;
        return a.code.localeCompare(b.code);
      });
  } catch {
    countryOptions.value = [];
    ElMessage.error(t('publishDemand.countryLoadFailed'));
  } finally {
    countryLoading.value = false;
  }
}

async function loadFeeConfig() {
  try {
    const data = await getOrderFeeConfigApi();
    platformFeeRate.value = Number(data?.platformFeeRate || 0);
    escrowFeeRate.value = Number(data?.escrowFeeRate || 0);
  } catch {
    platformFeeRate.value = 0;
    escrowFeeRate.value = 0;
    ElMessage.error(t('publishDemand.feeConfigLoadFailed'));
  }
}

async function loadDemandPool() {
  poolLoading.value = true;
  try {
    const data = await getMyDemandListApi();
    demandPool.value = Array.isArray(data) ? data : [];
  } catch {
    demandPool.value = [];
  } finally {
    poolLoading.value = false;
  }
}

function clearPoolFilters() {
  poolKeyword.value = '';
  poolCountry.value = '';
  poolStatus.value = '';
}

function openCreateDialog() {
  createDialogVisible.value = true;
}

function goWorkerPool(demandId) {
  if (!demandId) {
    router.push('/worker-pool');
    return;
  }
  router.push({
    path: '/worker-pool',
    query: { demandId: String(demandId) },
  });
}

async function handleSubmit() {
  if (!formRef.value || submitLoading.value) {
    return;
  }

  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const payload = {
      ...formData,
      category: selectedCategoryCode.value === FALLBACK_OTHER_CODE
        ? FALLBACK_OTHER_CODE
        : String(formData.category || '').trim(),
      // Submit dict_code country value (e.g. SG/US)
      country: String(formData.country || '').trim().toUpperCase(),
      preferredWorkerProfileId: preferredWorkerProfileId.value > 0 ? preferredWorkerProfileId.value : undefined,
    };
    const data = await createDemandApi(payload);
    const demandId = data?.demandId || data?.id || data || '';
    ElMessage.success(t('publishDemand.submitSuccess'));
    createDialogVisible.value = false;
    router.push({
      path: '/worker-pool',
      query: demandId ? { demandId: String(demandId) } : undefined,
    });
  } finally {
    submitLoading.value = false;
  }
}

function resetForm() {
  formRef.value?.resetFields();
  selectedCategoryCode.value = '';
  latestCategoryKeyword.value = '';
}

function syncPreferredWorkerFromRoute() {
  const nextProfileId = Number(route.query?.preferredWorkerProfileId || 0);
  preferredWorkerProfileId.value = Number.isFinite(nextProfileId) && nextProfileId > 0 ? nextProfileId : 0;
  preferredWorkerName.value = String(route.query?.preferredWorkerName || '').trim();
  preferredWorkerCountry.value = String(route.query?.preferredWorkerCountry || '').trim().toUpperCase();
  preferredWorkerCategory.value = String(route.query?.preferredWorkerCategory || '').trim();
  if (!formData.country && /^[A-Z]{2}$/.test(preferredWorkerCountry.value)) {
    formData.country = preferredWorkerCountry.value;
  }
  if (preferredWorkerProfileId.value > 0) {
    createDialogVisible.value = true;
  }
}

function clearPreferredWorker() {
  preferredWorkerProfileId.value = 0;
  preferredWorkerName.value = '';
  preferredWorkerCountry.value = '';
  preferredWorkerCategory.value = '';
  const nextQuery = { ...route.query };
  delete nextQuery.preferredWorkerProfileId;
  delete nextQuery.preferredWorkerName;
  delete nextQuery.preferredWorkerCountry;
  delete nextQuery.preferredWorkerCategory;
  router.replace({
    path: route.path,
    query: nextQuery,
  });
}

watch(
  () => route.query,
  () => {
    syncPreferredWorkerFromRoute();
  },
  { deep: true },
);

onMounted(() => {
  syncPreferredWorkerFromRoute();
  loadCountryOptions();
  loadDemandPool();
  loadFeeConfig();
});
</script>

<style scoped>
.publish-page {
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
  max-width: 600px;
}

.hero-actions {
  position: relative;
  z-index: 1;
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

.hero-badges {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-self: center;
}

.preferred-worker-alert {
  border-radius: 12px;
}

.preferred-worker-body {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  margin-top: 4px;
  align-items: center;
}

.badge {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.content-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 24px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.filter-card,
.list-card,
.form-card,
.side-card {
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

.section-tip {
  font-size: 12.5px;
  color: #65819e;
}

.pool-filter-row {
  display: grid;
  grid-template-columns: 1.25fr 0.9fr 0.9fr auto auto;
  gap: 12px;
  align-items: center;
}

.pool-filter-item {
  min-width: 180px;
}

.loading-wrap {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.demand-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.demand-card {
  border: 1px solid rgba(216, 228, 246, 0.85);
  border-radius: 16px;
  padding: 16px 18px;
  background: #fdfdff;
  box-shadow: 0 12px 24px rgba(31, 57, 107, 0.03);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.demand-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 36px rgba(31, 57, 107, 0.08);
  border-color: #bad2f9;
}

.demand-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.demand-top h3 {
  margin: 0;
  color: #11344d;
  font-size: 19px;
}

.demand-desc {
  margin: 10px 0 0;
  color: #557186;
  font-size: 13px;
  line-height: 1.7;
  min-height: 44px;
}

.demand-meta {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: #425c71;
  font-size: 13px;
}

.demand-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.demand-actions :deep(.el-button) {
  border-radius: 999px;
  font-weight: 600;
  padding: 8px 18px;
}

:deep(.publish-create-dialog .el-dialog) {
  border-radius: 18px;
}

:deep(.publish-create-dialog .el-dialog__body) {
  max-height: 74vh;
  overflow: auto;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.category-autocomplete,
.country-select {
  width: 100%;
}

.preset-row {
  margin: 0 0 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.preset-label {
  font-size: 12px;
  color: #6e8799;
}

.hot-row {
  margin-top: -2px;
}

.hot-tag {
  cursor: pointer;
  user-select: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  border-width: 1px;
}

.hot-tag:hover {
  transform: translateY(-1px);
}

:deep(.hot-tag.is-active) {
  box-shadow: 0 6px 14px rgba(16, 185, 129, 0.26);
}

.country-empty-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #8a9bae;
}

.country-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.country-option-label {
  color: #2a4154;
}

.country-option-hot {
  border: 1px solid #9ec5ac;
  color: #116149;
  background: #edf8f2;
  border-radius: 999px;
  font-size: 11px;
  line-height: 1;
  padding: 3px 8px;
  flex-shrink: 0;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.action-row :deep(.el-button) {
  border-radius: 999px;
  padding: 12px 24px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.action-row :deep(.el-button--primary) {
  background: linear-gradient(120deg, #1f4f99, #183e7a);
  border: none;
  box-shadow: 0 8px 16px rgba(31, 79, 153, 0.2);
}

.action-row :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(31, 79, 153, 0.3);
}

/* 深度定制表单输入框样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(216, 228, 246, 0.9) inset;
  background: #fdfeff;
  transition: all 0.25s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #a4c5f4 inset;
  background: #fff;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px #5a9cf8 inset !important;
  background: #fff;
}

.right-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.split-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed rgba(200, 216, 238, 0.7);
  color: #4b6a88;
  font-size: 14px;
}

.split-item strong {
  color: #0f2449;
  font-size: 16px;
  font-weight: 600;
}

.split-item.highlight {
  margin-top: 6px;
  padding: 14px 16px;
  border-bottom: none;
  background: linear-gradient(135deg, #f0fdf9, #e1fbf4);
  border-radius: 12px;
  border: 1px solid #c1f0de;
  color: #115e59;
}

.split-item.highlight strong {
  color: #0d9488;
  font-size: 22px;
  font-weight: 800;
}

.tips {
  margin: 0;
  padding: 0;
  color: #4b6a88;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13.5px;
  line-height: 1.7;
  list-style: none;
}
.tips li {
  position: relative;
  padding-left: 22px;
}
.tips li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  width: 14px;
  height: 14px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/200.svg' viewBox='0 0 20 20' fill='%2314b8a6'%3E%3Cpath fill-rule='evenodd' d='M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z' clip-rule='evenodd' /%3E%3C/svg%3E") no-repeat center center;
  background-size: contain;
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
  animation-delay: 0.12s;
}

@media (max-width: 1100px) {
  .metric-grid,
  .demand-grid {
    grid-template-columns: 1fr;
  }

  .pool-filter-row {
    grid-template-columns: 1fr;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .hero-strip h1 {
    font-size: 22px;
  }
}

:global(.category-autocomplete-popper .category-suggestion) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:global(.category-autocomplete-popper .category-suggestion.is-fallback) {
  color: #0b5c8a;
  font-weight: 600;
}

:global(.category-autocomplete-popper .match-highlight) {
  color: #0f766e;
  font-weight: 700;
}
</style>
