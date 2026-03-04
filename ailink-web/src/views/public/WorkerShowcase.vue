<template>
  <div class="worker-showcase-page">
    <section class="hero">
      <div class="halo halo-a" />
      <div class="halo halo-b" />
      <div class="hero-inner reveal-up">
        <p class="eyebrow">GLOBAL TALENT POOL</p>
        <h1>{{ t('workerShowcase.title') }}</h1>
        <p>{{ t('workerShowcase.subtitle') }}</p>
      </div>
    </section>

    <section class="toolbar-wrap">
      <div class="toolbar reveal-up delay-1">
        <div class="search-row">
          <el-input
            v-model="queryParams.skillKeyword"
            clearable
            :placeholder="t('workerShowcase.searchPlaceholder')"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="queryParams.country" clearable :placeholder="t('workerShowcase.countryPlaceholder')" class="select-input" @change="handleSearch">
            <el-option v-for="country in countries" :key="country.value" :label="country.label" :value="country.value" />
          </el-select>
          <el-button type="primary" class="search-btn" @click="handleSearch">{{ t('workerShowcase.search') }}</el-button>
        </div>

        <div class="chips-row">
          <span>{{ t('workerShowcase.hotCountries') }}</span>
          <button
            v-for="country in quickCountries"
            :key="country"
            class="quick-chip"
            :class="{ active: queryParams.country === country }"
            @click="pickCountry(country)"
          >
            {{ localizeCountryLabel(country) }}
          </button>
        </div>
      </div>
    </section>

    <section class="content">
      <header class="result-head reveal-up">
        <div>
          <h2>{{ t('workerShowcase.resultTitle') }}</h2>
          <p>{{ t('workerShowcase.resultCount', { count: workerList.length }) }}</p>
        </div>
        <el-button text @click="clearFilters">{{ t('workerShowcase.clearFilters') }}</el-button>
      </header>

      <div v-if="loading" class="loading-grid">
        <div v-for="n in 6" :key="n" class="skeleton-card"><el-skeleton :rows="4" animated /></div>
      </div>

      <el-empty
        v-else-if="workerList.length === 0"
        class="empty-box"
        :description="t('workerShowcase.empty')"
        :image-size="118"
      />

      <div v-else class="worker-grid">
        <article
          v-for="(item, idx) in workerList"
          :key="item.workerProfileId"
          class="worker-card reveal-up"
          :style="{ animationDelay: `${idx * 0.05}s` }"
          @click="handleCardClick"
        >
          <div class="card-head">
            <div class="avatar">{{ getInitial(item.displayName) }}</div>
            <div class="identity">
              <h3>
                {{ item.displayName || t('workerShowcase.executor') }}
                <el-icon v-if="item.verified === 1" class="verified"><CircleCheckFilled /></el-icon>
              </h3>
              <p><el-icon><Location /></el-icon>{{ localizeCountryLabel(item.country) || t('workerShowcase.unknownRegion') }}</p>
            </div>
            <div class="rating">
              <el-icon><StarFilled /></el-icon>
              <span>{{ formatRating(item.rating) }}</span>
            </div>
          </div>

          <div class="skills">
            <span v-for="tag in parseTags(item.skillTags)" :key="tag" class="skill-tag">{{ tag }}</span>
            <span v-if="parseTags(item.skillTags).length === 0" class="skill-tag">{{ t('workerShowcase.unlabeledSkills') }}</span>
          </div>

          <div class="meta">
            <div class="price">
              <p>{{ t('workerShowcase.referenceQuote') }}</p>
              <strong>¥{{ formatMoney(item.priceMin) }} - ¥{{ formatMoney(item.priceMax) }}</strong>
            </div>
            <button class="contact-btn" @click.stop="handleCardClick">{{ t('workerShowcase.contactAfterLogin') }}</button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { CircleCheckFilled, Location, Search, StarFilled } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { getPublicWorkersApi } from '@/api/public';
import { COUNTRY_PRESETS } from '@/dicts';
import { openAuthModal } from '@/composables/useAuthModal';

const route = useRoute();
const router = useRouter();
const { t, locale } = useI18n();
const loading = ref(false);
const workerList = ref([]);

const queryParams = reactive({
  skillKeyword: String(route.query.skillKeyword || ''),
  country: String(route.query.country || ''),
});

const countries = COUNTRY_PRESETS.map((label) => ({ label: localizeCountryLabel(label), value: label }));
const quickCountries = COUNTRY_PRESETS.slice(0, 8);

async function fetchWorkers() {
  loading.value = true;
  try {
    const data = await getPublicWorkersApi({
      skillKeyword: queryParams.skillKeyword,
      country: queryParams.country,
    });
    workerList.value = Array.isArray(data) ? data : [];
  } catch {
    workerList.value = [];
  } finally {
    loading.value = false;
  }
}

function syncQuery() {
  const query = {};
  if (queryParams.skillKeyword) query.skillKeyword = queryParams.skillKeyword;
  if (queryParams.country) query.country = queryParams.country;
  router.replace({ path: route.path, query });
}

function handleSearch() {
  syncQuery();
  fetchWorkers();
}

function pickCountry(country) {
  queryParams.country = queryParams.country === country ? '' : country;
  handleSearch();
}

function clearFilters() {
  queryParams.skillKeyword = '';
  queryParams.country = '';
  handleSearch();
}

function handleCardClick() {
  openAuthModal('login');
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

function parseTags(source) {
  return String(source || '')
    .split(/[，,]/)
    .map((i) => i.trim())
    .filter(Boolean)
    .slice(0, 5);
}

function getInitial(name) {
  const text = String(name || '').trim();
  return text ? text.charAt(0).toUpperCase() : 'W';
}

function formatRating(value) {
  const n = Number(value);
  if (Number.isNaN(n) || n <= 0) return '5.0';
  return n.toFixed(1);
}

function formatMoney(value) {
  const n = Number(value || 0);
  return n.toLocaleString(locale.value, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

onMounted(() => {
  fetchWorkers();
});
</script>

<style scoped>
.worker-showcase-page {
  min-height: calc(100vh - 190px);
  background:
    radial-gradient(circle at 100% 0%, rgba(48, 209, 228, 0.14), transparent 34%),
    radial-gradient(circle at 0% 16%, rgba(88, 127, 255, 0.14), transparent 36%),
    linear-gradient(180deg, #edf3ff 0%, #f8fbff 42%, #fbfcff 100%);
}

.hero {
  position: relative;
  overflow: hidden;
  padding: 92px 20px 56px;
  background:
    radial-gradient(circle at 86% 10%, rgba(52, 217, 220, 0.34), transparent 30%),
    linear-gradient(136deg, #071a40 0%, #103067 50%, #1a4685 100%);
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}

.halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
}

.halo-a {
  width: 200px;
  height: 200px;
  left: -54px;
  top: -34px;
  background: rgba(96, 150, 255, 0.4);
}

.halo-b {
  width: 160px;
  height: 160px;
  right: 8%;
  top: 20%;
  background: rgba(101, 235, 205, 0.3);
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 980px;
  margin: 0 auto;
  text-align: center;
  color: #edf5ff;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #bdd4ff;
}

.hero-inner h1 {
  margin: 10px 0 0;
  font-size: clamp(32px, 4vw, 52px);
}

.hero-inner p {
  margin: 14px auto 0;
  max-width: 720px;
  line-height: 1.74;
  color: rgba(216, 231, 255, 0.92);
}

.toolbar-wrap {
  max-width: 1100px;
  margin: -24px auto 0;
  padding: 0 20px;
  position: sticky;
  top: 72px;
  z-index: 30;
}

.toolbar {
  border-radius: 20px;
  border: 1px solid rgba(227, 237, 255, 0.95);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 30px rgba(31, 74, 145, 0.12);
  padding: 14px;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 0.8fr) auto;
  gap: 10px;
}

.search-btn {
  border-radius: 12px;
  padding: 0 20px;
}

.chips-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.chips-row span {
  font-size: 12px;
  color: #4f688d;
}

.quick-chip {
  border: 1px solid #d4e4ff;
  background: #f5f9ff;
  color: #2a5597;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-chip:hover,
.quick-chip.active {
  background: #ddebff;
  border-color: #bed7ff;
}

.content {
  max-width: 1100px;
  margin: 22px auto 0;
  padding: 0 20px 28px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.skeleton-card {
  border-radius: 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid #e3edff;
}

.empty-box {
  margin-top: 24px;
  border-radius: 20px;
  border: 1px dashed #ceddf9;
  background: rgba(255, 255, 255, 0.6);
}

.worker-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.worker-card {
  border-radius: 18px;
  border: 1px solid rgba(218, 231, 255, 0.94);
  background: linear-gradient(168deg, rgba(255, 255, 255, 0.94), rgba(244, 250, 255, 0.85));
  box-shadow: 0 16px 25px rgba(28, 64, 129, 0.1);
  padding: 16px;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  cursor: pointer;
}

.worker-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 34px rgba(22, 55, 115, 0.18);
  border-color: #b8d4ff;
}

.card-head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 800;
  color: #0e326d;
  background: linear-gradient(135deg, #dff1ff, #dbffef);
  border: 1px solid #c9e8ff;
}

.identity h3 {
  margin: 0;
  font-size: 16px;
  color: #11284d;
  display: flex;
  align-items: center;
  gap: 4px;
}

.identity p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #5a7194;
  display: flex;
  align-items: center;
  gap: 5px;
}

.verified {
  color: #17b57f;
}

.rating {
  border-radius: 10px;
  border: 1px solid #f9e7b0;
  background: #fff7df;
  color: #b07719;
  padding: 4px 8px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 700;
}

.skills {
  margin-top: 12px;
  min-height: 30px;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.skill-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid #d6e6ff;
  background: #eef5ff;
  color: #305a98;
  padding: 5px 9px;
  font-size: 12px;
}

.meta {
  margin-top: 12px;
  border-top: 1px dashed #d8e5fb;
  padding-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 12px;
}

.price p {
  margin: 0;
  font-size: 11px;
  color: #5d7498;
}

.price strong {
  margin-top: 4px;
  display: block;
  color: #d06430;
  font-size: 16px;
}

.contact-btn {
  border: 1px solid #b9d3ff;
  background: #e8f2ff;
  color: #1f4f93;
  border-radius: 10px;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.contact-btn:hover {
  background: #dbeaff;
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

@media (max-width: 1080px) {
  .worker-grid,
  .loading-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero {
    padding-top: 86px;
  }

  .toolbar-wrap {
    position: static;
    margin-top: -20px;
  }

  .search-row {
    grid-template-columns: 1fr;
  }

  .worker-grid,
  .loading-grid {
    grid-template-columns: 1fr;
  }
}
</style>
