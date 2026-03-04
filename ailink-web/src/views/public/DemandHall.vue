<template>
  <div class="demand-hall-page">
    <section class="hero">
      <div class="glow glow-a" />
      <div class="glow glow-b" />
      <div class="hero-inner reveal-up">
        <p class="eyebrow">DEMAND EXPLORER</p>
        <h1>{{ t('demandHall.title') }}</h1>
        <p>{{ t('demandHall.subtitle') }}</p>
      </div>
    </section>

    <section class="filter-shell">
      <div class="filter-card reveal-up delay-1">
        <div class="filter-row">
          <el-input
            v-model="queryParams.keyword"
            :placeholder="t('demandHall.searchPlaceholder')"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.category" :placeholder="t('demandHall.categoryPlaceholder')" clearable class="select-input" @change="handleSearch">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
          <el-select v-model="queryParams.country" :placeholder="t('demandHall.countryPlaceholder')" clearable class="select-input" @change="handleSearch">
            <el-option v-for="country in countries" :key="country.value" :label="country.label" :value="country.value" />
          </el-select>
          <el-button type="primary" class="search-btn" @click="handleSearch">{{ t('demandHall.search') }}</el-button>
        </div>

        <div class="quick-row">
          <span>{{ t('demandHall.hotCategories') }}</span>
          <button
            v-for="item in quickCategories"
            :key="item"
            class="quick-chip"
            :class="{ active: queryParams.category === item }"
            @click="pickCategory(item)"
          >
            {{ displayCategory(item) }}
          </button>
        </div>
      </div>
    </section>

    <section class="content">
      <header class="result-head reveal-up">
        <div>
          <h2>{{ t('demandHall.resultTitle') }}</h2>
          <p>{{ t('demandHall.resultCount', { count: demandList.length }) }}</p>
        </div>
        <el-button text @click="clearFilters">{{ t('demandHall.clearFilters') }}</el-button>
      </header>

      <div v-if="loading" class="loading-grid">
        <div v-for="n in 6" :key="n" class="skeleton-card">
          <el-skeleton :rows="4" animated />
        </div>
      </div>

      <el-empty
        v-else-if="demandList.length === 0"
        class="empty-box"
        :description="t('demandHall.empty')"
        :image-size="120"
      />

      <div v-else class="demand-grid">
        <article
          v-for="(item, idx) in demandList"
          :key="item.id"
          class="demand-card reveal-up"
          :style="{ animationDelay: `${idx * 0.05}s` }"
          @click="handleCardClick"
        >
          <div class="card-top">
            <span class="category">{{ displayCategory(item.category) || t('demandHall.uncategorized') }}</span>
            <span class="budget">¥{{ formatMoney(item.budget) }}</span>
          </div>
          <h3>{{ formatTitle(item.description) }}</h3>
          <p>{{ formatDesc(item.description) }}</p>
          <div class="card-foot">
            <span><el-icon><Location /></el-icon>{{ displayCountry(item.targetCountry) || 'Global' }}</span>
            <span><el-icon><Clock /></el-icon>{{ formatDate(item.createdTime) }}</span>
          </div>
          <div class="card-actions">
            <button
              v-if="isLogin && canApplyDemand"
              type="button"
              class="apply-btn"
              :disabled="Boolean(applyLoadingMap[item.id])"
              @click.stop="handleApply(item)"
            >
              {{ applyLoadingMap[item.id] ? t('demandHall.submitting') : t('demandHall.applyNow') }}
            </button>
            <button
              v-else-if="!isLogin"
              type="button"
              class="apply-btn secondary"
              @click.stop="handleCardClick"
            >
              {{ t('demandHall.applyAfterLogin') }}
            </button>
            <span v-else class="login-hint">{{ t('demandHall.requireWorkerApproved') }}</span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Clock, Location, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { getPublicDemandsApi } from '@/api/public';
import { CATEGORY_PRESETS, COUNTRY_PRESETS } from '@/dicts';
import { openAuthModal } from '@/composables/useAuthModal';
import { useUserStore } from '@/store/modules/user';
import { submitDemandApplyApi } from '@/api/demandApply';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { t, locale } = useI18n();
const loading = ref(false);
const demandList = ref([]);
const applyLoadingMap = reactive({});

const queryParams = reactive({
  keyword: String(route.query.keyword || ''),
  category: String(route.query.category || ''),
  country: String(route.query.country || ''),
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
const categories = CATEGORY_PRESETS.map((label) => ({ label: localizeCategoryLabel(label), value: label }));
const countries = COUNTRY_PRESETS.map((label) => ({ label: localizeCountryLabel(label), value: label }));
const quickCategories = CATEGORY_PRESETS.slice(0, 8);
const isLogin = computed(() => userStore.isLogin);
const canApplyDemand = computed(() => {
  const role = String(userStore.userInfo?.role || '').toUpperCase();
  const applyStatus = String(userStore.userInfo?.workerApplyStatus || '').toUpperCase();
  return role === 'WORKER' || applyStatus === 'APPROVED';
});

async function fetchDemands() {
  loading.value = true;
  try {
    const data = await getPublicDemandsApi({
      keyword: queryParams.keyword,
      category: queryParams.category,
      country: queryParams.country,
    });
    demandList.value = Array.isArray(data) ? data : [];
  } catch {
    demandList.value = [];
  } finally {
    loading.value = false;
  }
}

function syncQuery() {
  const query = {};
  if (queryParams.keyword) query.keyword = queryParams.keyword;
  if (queryParams.category) query.category = queryParams.category;
  if (queryParams.country) query.country = queryParams.country;
  router.replace({ path: route.path, query });
}

function handleSearch() {
  syncQuery();
  fetchDemands();
}

function pickCategory(category) {
  queryParams.category = queryParams.category === category ? '' : category;
  handleSearch();
}

function clearFilters() {
  queryParams.keyword = '';
  queryParams.category = '';
  queryParams.country = '';
  handleSearch();
}

function handleCardClick() {
  if (!isLogin.value) {
    openAuthModal('login', { redirect: route.fullPath });
  }
}

function localizeCategoryLabel(label) {
  const text = String(label || '').trim();
  if (!text) return '';
  const i18nKey = CATEGORY_I18N_KEY_MAP[text];
  return i18nKey ? t(i18nKey) : text;
}

function localizeCountryLabel(country) {
  const text = String(country || '').trim();
  const code = text.toUpperCase();
  if (/^[A-Z]{2}$/.test(code)) {
    try {
      const displayNames = new Intl.DisplayNames([locale.value], { type: 'region' });
      return displayNames.of(code) || text;
    } catch {
      return text;
    }
  }
  return text;
}

function displayCategory(value) {
  return localizeCategoryLabel(value);
}

function displayCountry(value) {
  return localizeCountryLabel(value);
}

async function handleApply(item) {
  if (!isLogin.value) {
    openAuthModal('login', { redirect: route.fullPath });
    return;
  }
  if (!canApplyDemand.value) {
    ElMessage.warning(t('demandHall.needWorkerApproved'));
    return;
  }
  const demandId = Number(item?.id || 0);
  if (!demandId || applyLoadingMap[demandId]) return;

  try {
    const defaultQuote = Number(item?.budget || 0);
    const { value } = await ElMessageBox.prompt(t('demandHall.applyPromptMessage'), t('demandHall.applyPromptTitle'), {
      confirmButtonText: t('demandHall.applyConfirm'),
      cancelButtonText: t('demandHall.cancel'),
      inputPlaceholder: t('demandHall.applyPlaceholder'),
      inputValue: defaultQuote > 0 ? String(defaultQuote) : '',
      inputPattern: /^(?:[1-9]\d*|0)(?:\.\d{1,2})?$/,
      inputErrorMessage: t('demandHall.applyAmountInvalid'),
    });
    const quoteAmount = Number(String(value || '').trim());
    if (!Number.isFinite(quoteAmount) || quoteAmount <= 0) {
      ElMessage.warning(t('demandHall.applyAmountGtZero'));
      return;
    }
    applyLoadingMap[demandId] = true;
    await submitDemandApplyApi(demandId, {
      quoteAmount,
      applyNote: '',
    });
    ElMessage.success(t('demandHall.applySuccess'));
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || t('demandHall.applyFailed'));
    }
  } finally {
    applyLoadingMap[demandId] = false;
  }
}

function formatTitle(desc) {
  if (!desc) return t('demandHall.untitled');
  const match = desc.match(/^\[(.*?)\]/);
  return match?.[1] || t('demandHall.detailTitle');
}

function formatDesc(desc) {
  if (!desc) return t('demandHall.noDesc');
  const match = desc.match(/^\[.*?\]\s*(.*)/);
  const text = (match?.[1] || desc).trim();
  if (text.length <= 120) return text;
  return `${text.slice(0, 120)}...`;
}

function formatDate(value) {
  if (!value) return '--';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '--';
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatMoney(value) {
  const n = Number(value || 0);
  return n.toLocaleString(locale.value, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

onMounted(() => {
  fetchDemands();
});
</script>

<style scoped>
.demand-hall-page {
  min-height: calc(100vh - 190px);
  background:
    radial-gradient(circle at 0% 0%, rgba(69, 119, 255, 0.14), transparent 34%),
    radial-gradient(circle at 100% 12%, rgba(56, 211, 224, 0.13), transparent 32%),
    linear-gradient(180deg, #eff4ff 0%, #f8faff 44%, #f9fbff 100%);
}

.hero {
  position: relative;
  overflow: hidden;
  padding: 94px 20px 58px;
  background:
    radial-gradient(circle at 14% -12%, rgba(91, 154, 255, 0.48), transparent 36%),
    linear-gradient(130deg, #0a1c44 0%, #112d63 52%, #18407f 100%);
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
}

.glow-a {
  width: 220px;
  height: 220px;
  left: -50px;
  top: -30px;
  background: rgba(80, 154, 255, 0.42);
}

.glow-b {
  width: 180px;
  height: 180px;
  right: 5%;
  top: 12%;
  background: rgba(76, 238, 215, 0.34);
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1020px;
  margin: 0 auto;
  color: #edf5ff;
  text-align: center;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #bfd4ff;
}

.hero-inner h1 {
  margin: 10px 0 0;
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1.05;
}

.hero-inner p {
  margin: 14px auto 0;
  max-width: 760px;
  color: rgba(221, 234, 255, 0.92);
  line-height: 1.75;
}

.filter-shell {
  max-width: 1100px;
  margin: -26px auto 0;
  padding: 0 20px;
  position: sticky;
  top: 72px;
  z-index: 30;
}

.filter-card {
  border-radius: 20px;
  border: 1px solid rgba(227, 236, 255, 0.96);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 30px rgba(33, 73, 146, 0.13);
  padding: 14px;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 0.8fr) minmax(0, 0.8fr) auto;
  gap: 10px;
}

.search-btn {
  border-radius: 12px;
  padding: 0 20px;
}

.quick-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.quick-row span {
  font-size: 12px;
  color: #4f6892;
}

.quick-chip {
  border: 1px solid #d6e5ff;
  background: #f4f8ff;
  color: #285394;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-chip:hover,
.quick-chip.active {
  background: #ddecff;
  border-color: #bdd6ff;
}

.content {
  max-width: 1100px;
  margin: 22px auto 0;
  padding: 0 20px 28px;
}

.result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.result-head h2 {
  margin: 0;
  font-size: 28px;
  color: #12264b;
}

.result-head p {
  margin: 6px 0 0;
  color: #5a7094;
}

.loading-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.skeleton-card {
  border-radius: 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid #e6eeff;
}

.empty-box {
  margin-top: 26px;
  border-radius: 20px;
  border: 1px dashed #ceddf9;
  background: rgba(255, 255, 255, 0.62);
  padding: 20px 0;
}

.demand-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.demand-card {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(219, 231, 255, 0.94);
  background: linear-gradient(170deg, rgba(255, 255, 255, 0.93), rgba(245, 250, 255, 0.84));
  box-shadow: 0 16px 25px rgba(30, 68, 136, 0.1);
  padding: 16px;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.demand-card::after {
  content: '';
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  right: -56px;
  top: -70px;
  background: radial-gradient(circle, rgba(122, 194, 255, 0.24), transparent 70%);
  pointer-events: none;
}

.demand-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 34px rgba(24, 58, 117, 0.17);
  border-color: #bad5ff;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  color: #194e93;
  border: 1px solid #d0e3ff;
  background: #edf5ff;
}

.budget {
  color: #d9682e;
  font-weight: 800;
  font-size: 22px;
}

.demand-card h3 {
  margin: 12px 0 8px;
  font-size: 19px;
  color: #10254a;
}

.demand-card p {
  margin: 0;
  color: #536b8f;
  line-height: 1.66;
  font-size: 14px;
}

.card-foot {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #d6e4fb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #5f7698;
  font-size: 12px;
}

.card-foot span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.login-hint {
  font-size: 12px;
  color: #245290;
}

.card-actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.apply-btn {
  border: 1px solid #98c0f7;
  background: #e7f1ff;
  color: #1e4f91;
  border-radius: 10px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.apply-btn:hover {
  background: #d7e9ff;
}

.apply-btn.secondary {
  border-color: #c2d8fb;
  color: #395f98;
  background: #f2f7ff;
}

.apply-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reveal-up {
  animation: fadeInUp 0.5s ease both;
}

.delay-1 {
  animation-delay: 0.1s;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .filter-row {
    grid-template-columns: 1fr 1fr;
  }

  .demand-grid,
  .loading-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 740px) {
  .hero {
    padding-top: 86px;
  }

  .filter-shell {
    position: static;
    margin-top: -20px;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
