<template>
  <div class="publish-page">
    <section class="hero-strip">
      <div>
        <p class="kicker">Demand Creation Workspace</p>
        <h1>发布跨国需求</h1>
        <p class="sub">
          一次提交，进入平台匹配、托管交易、履约结算的完整链路。需求写得越清晰，成交效率越高。
        </p>
      </div>
      <div class="hero-badges">
        <span class="badge">托管保障</span>
        <span class="badge">风控审计</span>
        <span class="badge">跨国履约</span>
      </div>
    </section>

    <section class="content-grid">
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="section-head">
            <h2>需求信息</h2>
            <span class="section-tip">* 为必填项</span>
          </div>
        </template>

        <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
          <div class="two-col">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" maxlength="50" placeholder="例如：新加坡市场产品文案本地化" />
            </el-form-item>

            <el-form-item label="类目" prop="category">
              <el-autocomplete
                v-model="formData.category"
                class="category-autocomplete"
                popper-class="category-autocomplete-popper"
                :fetch-suggestions="queryCategorySuggestions"
                :trigger-on-focus="true"
                :highlight-first-item="true"
                maxlength="50"
                clearable
                placeholder="搜索或输入类目，例如：网站开发 / 翻译 / 视频剪辑"
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
            <span class="preset-label">热门推荐：</span>
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
            <el-form-item label="预算（USD）" prop="budget">
              <el-input-number
                v-model="formData.budget"
                :min="0.01"
                :step="50"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="国家" prop="country">
              <el-select
                v-model="formData.country"
                class="country-select"
                filterable
                clearable
                placeholder="请选择国家"
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
                      <span v-if="item.hot" class="country-option-hot">推荐</span>
                    </div>
                  </el-option>
                </el-option-group>
              </el-select>
              <div v-if="!countryLoading && groupedCountryOptions.length === 0" class="country-empty-tip">
                暂无可选国家，请联系管理员配置国家字典。
              </div>
            </el-form-item>
          </div>

          <el-form-item label="描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="6"
              maxlength="500"
              show-word-limit
              placeholder="请写清交付内容、时效、验收标准和协作方式。"
            />
          </el-form-item>

          <div class="action-row">
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">发布需求</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-form>
      </el-card>

      <div class="right-stack">
        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="section-head">
              <h2>金额拆分预估</h2>
            </div>
          </template>

          <div class="split-item">
            <span>订单预算</span>
            <strong>{{ formatMoney(budgetAmount) }}</strong>
          </div>
          <div class="split-item">
            <span>平台抽成 (6%)</span>
            <strong>{{ formatMoney(platformFeeAmount) }}</strong>
          </div>
          <div class="split-item">
            <span>托管手续费 (0.5%)</span>
            <strong>{{ formatMoney(escrowFeeAmount) }}</strong>
          </div>
          <div class="split-item highlight">
            <span>执行者预估到手</span>
            <strong>{{ formatMoney(workerIncomeAmount) }}</strong>
          </div>
        </el-card>

        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="section-head">
              <h2>成交建议</h2>
            </div>
          </template>
          <ul class="tips">
            <li>标题建议包含国家 + 类目 + 交付目标，提升匹配速度。</li>
            <li>预算越明确，执行者报价越稳定，减少议价时间。</li>
            <li>验收标准写进描述，能显著降低争议率。</li>
            <li>使用平台托管交易，避免线下支付风险。</li>
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
import { createDemandApi } from '@/api/demand';
import { getCountryDictApi } from '@/api/dict';
import { CATEGORY_OPTIONS, CATEGORY_PRESETS } from '@/dicts';
import { formatMoney } from '@/utils/format';

const router = useRouter();
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
const FALLBACK_OTHER_LABEL = '定制需求';
const FALLBACK_GUIDE_TEXT = '未找到？发布为【定制需求】';
const latestCategoryKeyword = ref('');
const selectedCategoryCode = ref('');

const countryOptions = ref([]);
const countryLoading = ref(false);

const REGION_ORDER = ['ASIA', 'EUROPE', 'OTHER'];
const REGION_LABEL_MAP = {
  ASIA: 'Asia',
  EUROPE: 'Europe',
  OTHER: 'Other',
};

const categoryOptionList = computed(() => {
  const fromOptions = Array.isArray(CATEGORY_OPTIONS) ? CATEGORY_OPTIONS : [];
  if (fromOptions.length > 0) {
    return fromOptions
      .map((item) => ({
        code: String(item?.code || item?.label || '').trim(),
        label: String(item?.label || item?.code || '').trim(),
        sort: Number(item?.sort || 0),
        hot: Boolean(item?.hot),
        isFallback: false,
      }))
      .filter((item) => item.code && item.label);
  }

  return (Array.isArray(categoryPresets) ? categoryPresets : [])
    .map((label, index) => ({
      code: String(label),
      label: String(label),
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
      label: '热门国家',
      options: hotOptions,
    });
  }

  const remain = hotOptions.length > 0 ? list.filter((item) => !item.hot) : list;
  REGION_ORDER.forEach((region) => {
    const options = remain.filter((item) => item.region === region);
    if (options.length > 0) {
      groups.push({
        key: `region_${region}`,
        label: REGION_LABEL_MAP[region],
        options,
      });
    }
  });

  return groups;
});

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请输入类目', trigger: 'blur' }],
  budget: [
    { required: true, message: '请输入预算', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined || Number(value) <= 0) {
          callback(new Error('预算必须为正数'));
          return;
        }
        callback();
      },
      trigger: 'change',
    },
  ],
  country: [{ required: true, message: '请选择国家', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
};

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
      label: FALLBACK_GUIDE_TEXT,
      value: FALLBACK_GUIDE_TEXT,
      isFallback: true,
    },
  ]);
}

function handleCategorySelect(item) {
  if (!item) return;
  if (item.isFallback || item.code === FALLBACK_OTHER_CODE) {
    selectedCategoryCode.value = FALLBACK_OTHER_CODE;
    formData.category = FALLBACK_OTHER_LABEL;
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
  if (text === FALLBACK_OTHER_LABEL) {
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

function normalizeCountryOption(row, index) {
  const code = String(row?.dict_code || row?.dictCode || '').trim().toUpperCase();
  const label = String(row?.dict_label || row?.dictLabel || '').trim();
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
    ElMessage.error('国家字典加载失败，请稍后重试');
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
      // 国家提交 dict_code（如 SG/US）
      country: String(formData.country || '').trim().toUpperCase(),
    };
    const data = await createDemandApi(payload);
    const demandId = data?.demandId || data?.id || data || '';
    ElMessage.success('需求发布成功');
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
