<template>
  <div class="demand-hall-page">
    <section class="hero">
      <div class="glow glow-a" />
      <div class="glow glow-b" />
      <div class="hero-inner reveal-up">
        <p class="eyebrow">DEMAND EXPLORER</p>
        <h1>全球需求大厅</h1>
        <p>
          快速浏览公开需求，按国家、类目和关键词筛选。登录后可进入工作台完成选人、下单与履约流程。
        </p>
      </div>
    </section>

    <section class="filter-shell">
      <div class="filter-card reveal-up delay-1">
        <div class="filter-row">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索需求关键词（如：本地化、广告投放）"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.category" placeholder="服务类目" clearable class="select-input" @change="handleSearch">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
          <el-select v-model="queryParams.country" placeholder="国家/地区" clearable class="select-input" @change="handleSearch">
            <el-option v-for="country in countries" :key="country.value" :label="country.label" :value="country.value" />
          </el-select>
          <el-button type="primary" class="search-btn" @click="handleSearch">搜索</el-button>
        </div>

        <div class="quick-row">
          <span>热门类目:</span>
          <button
            v-for="item in quickCategories"
            :key="item"
            class="quick-chip"
            :class="{ active: queryParams.category === item }"
            @click="pickCategory(item)"
          >
            {{ item }}
          </button>
        </div>
      </div>
    </section>

    <section class="content">
      <header class="result-head reveal-up">
        <div>
          <h2>公开需求列表</h2>
          <p>共 {{ demandList.length }} 条结果</p>
        </div>
        <el-button text @click="clearFilters">清空筛选</el-button>
      </header>

      <div v-if="loading" class="loading-grid">
        <div v-for="n in 6" :key="n" class="skeleton-card">
          <el-skeleton :rows="4" animated />
        </div>
      </div>

      <el-empty
        v-else-if="demandList.length === 0"
        class="empty-box"
        description="没有找到符合筛选条件的需求"
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
            <span class="category">{{ item.category || '未分类' }}</span>
            <span class="budget">¥{{ formatMoney(item.budget) }}</span>
          </div>
          <h3>{{ formatTitle(item.description) }}</h3>
          <p>{{ formatDesc(item.description) }}</p>
          <div class="card-foot">
            <span><el-icon><Location /></el-icon>{{ item.targetCountry || 'Global' }}</span>
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
              {{ applyLoadingMap[item.id] ? '提交中...' : '我来申请' }}
            </button>
            <button
              v-else-if="!isLogin"
              type="button"
              class="apply-btn secondary"
              @click.stop="handleCardClick"
            >
              登录后申请
            </button>
            <span v-else class="login-hint">通过执行者审核后可主动申请</span>
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
import { getPublicDemandsApi } from '@/api/public';
import { CATEGORY_PRESETS, COUNTRY_PRESETS } from '@/dicts';
import { openAuthModal } from '@/composables/useAuthModal';
import { useUserStore } from '@/store/modules/user';
import { submitDemandApplyApi } from '@/api/demandApply';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const demandList = ref([]);
const applyLoadingMap = reactive({});

const queryParams = reactive({
  keyword: String(route.query.keyword || ''),
  category: String(route.query.category || ''),
  country: String(route.query.country || ''),
});

const categories = CATEGORY_PRESETS.map((label) => ({ label, value: label }));
const countries = COUNTRY_PRESETS.map((label) => ({ label, value: label }));
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

async function handleApply(item) {
  if (!isLogin.value) {
    openAuthModal('login', { redirect: route.fullPath });
    return;
  }
  if (!canApplyDemand.value) {
    ElMessage.warning('请先在个人中心完成执行者审核');
    return;
  }
  const demandId = Number(item?.id || 0);
  if (!demandId || applyLoadingMap[demandId]) return;

  try {
    const defaultQuote = Number(item?.budget || 0);
    const { value } = await ElMessageBox.prompt('请输入你的报价（最多两位小数）', '申请该需求', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：1200',
      inputValue: defaultQuote > 0 ? String(defaultQuote) : '',
      inputPattern: /^(?:[1-9]\d*|0)(?:\.\d{1,2})?$/,
      inputErrorMessage: '请输入合法金额',
    });
    const quoteAmount = Number(String(value || '').trim());
    if (!Number.isFinite(quoteAmount) || quoteAmount <= 0) {
      ElMessage.warning('报价必须大于 0');
      return;
    }
    applyLoadingMap[demandId] = true;
    await submitDemandApplyApi(demandId, {
      quoteAmount,
      applyNote: '',
    });
    ElMessage.success('申请已提交，请等待需求方处理');
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '申请提交失败');
    }
  } finally {
    applyLoadingMap[demandId] = false;
  }
}

function formatTitle(desc) {
  if (!desc) return '未命名需求';
  const match = desc.match(/^\[(.*?)\]/);
  return match?.[1] || '需求详情';
}

function formatDesc(desc) {
  if (!desc) return '暂无描述';
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
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
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
