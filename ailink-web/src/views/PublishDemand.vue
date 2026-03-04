<template>
  <div class="publish-page">
    <section class="hero-strip">
      <div>
        <p class="kicker">{{ t('publishDemand.kicker') }}</p>
        <h1>{{ t('publishDemand.title') }}</h1>
        <p class="sub">{{ t('publishDemand.subtitle') }}</p>
      </div>
      <div class="hero-badges">
        <span class="badge">{{ t('publishDemand.badgeEscrow') }}</span>
        <span class="badge">{{ t('publishDemand.badgeRisk') }}</span>
        <span class="badge">{{ t('publishDemand.badgeCrossBorder') }}</span>
      </div>
    </section>

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
            <span>{{ t('publishDemand.splitPlatformFee') }}</span>
            <strong>{{ formatMoney(platformFeeAmount) }}</strong>
          </div>
          <div class="split-item">
            <span>{{ t('publishDemand.splitEscrowFee') }}</span>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { createDemandApi } from '@/api/demand';
import { getCountryDictApi } from '@/api/dict';
import { CATEGORY_OPTIONS, CATEGORY_PRESETS } from '@/dicts';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const { t, locale } = useI18n();
const formRef = ref(null);
const submitLoading = ref(false);

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
const platformFeeAmount = computed(() => round2(budgetAmount.value * 0.06));
const escrowFeeAmount = computed(() => round2(budgetAmount.value * 0.005));
const workerIncomeAmount = computed(() => round2(Math.max(0, budgetAmount.value - platformFeeAmount.value - escrowFeeAmount.value)));

function round2(value) {
  return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
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
    };
    const data = await createDemandApi(payload);
    const demandId = data?.demandId || data?.id || data || '';
    ElMessage.success(t('publishDemand.submitSuccess'));
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

onMounted(() => {
  loadCountryOptions();
});
</script>

<style scoped>
.publish-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-strip {
  border-radius: 16px;
  padding: 20px 22px;
  background:
    radial-gradient(circle at 20% 0%, rgba(67, 156, 211, 0.18), transparent 35%),
    linear-gradient(120deg, #0a3a56 0%, #0f4f63 60%, #0f6b63 100%);
  color: #e9f4ff;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.kicker {
  margin: 0 0 6px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #9dd6ff;
}

.hero-strip h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  color: #f7fcff;
}

.sub {
  margin: 8px 0 0;
  color: #c5deef;
  font-size: 14px;
  max-width: 760px;
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  grid-template-columns: 1.7fr 1fr;
  gap: 16px;
}

.form-card,
.side-card {
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

.section-tip {
  font-size: 12px;
  color: #7a90a2;
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
  gap: 10px;
}

.right-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.split-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 10px 0;
  border-bottom: 1px dashed #e1ebf3;
  color: #3d5a71;
  font-size: 13px;
}

.split-item strong {
  color: #102b40;
  font-size: 16px;
}

.split-item.highlight {
  border-bottom: none;
}

.split-item.highlight strong {
  color: #0f766e;
}

.tips {
  margin: 0;
  padding-left: 18px;
  color: #3c596f;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 1100px) {
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
