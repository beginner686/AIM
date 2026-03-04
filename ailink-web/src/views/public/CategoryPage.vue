<template>
  <div class="category-page">
    <section class="hero">
      <div class="light light-a" />
      <div class="light light-b" />
      <div class="hero-inner reveal-up">
        <p class="eyebrow">SERVICE DIRECTORY</p>
        <h1>{{ t('categoryPage.title') }}</h1>
        <p>{{ t('categoryPage.subtitle') }}</p>
      </div>
    </section>

    <section class="toolbar-wrap">
      <div class="toolbar reveal-up delay-1">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="t('categoryPage.searchPlaceholder')"
          class="search-input"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <p>{{ t('categoryPage.count', { count: filteredCategories.length }) }}</p>
      </div>
    </section>

    <section class="content">
      <div class="category-grid">
        <RouterLink
          v-for="(item, idx) in filteredCategories"
          :key="item.value"
          :to="`/explore/demands?category=${encodeURIComponent(item.value)}`"
          class="category-card reveal-up"
          :style="{ animationDelay: `${idx * 0.035}s` }"
        >
          <div class="top">
            <span class="icon">{{ getIcon(item.value) }}</span>
            <span class="arrow">→</span>
          </div>
          <h3>{{ item.label }}</h3>
          <p>{{ t('categoryPage.cardDesc') }}</p>
        </RouterLink>
      </div>

      <el-empty
        v-if="filteredCategories.length === 0"
        class="empty-box"
        :description="t('categoryPage.empty')"
        :image-size="120"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { CATEGORY_PRESETS } from '@/dicts';

const { t } = useI18n();
const keyword = ref('');
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
const categories = CATEGORY_PRESETS.map((value) => ({ label: localizeCategoryLabel(value), value }));

const iconMap = {
  翻译本地化: '🌐',
  远程助理: '🧩',
  视频剪辑: '🎬',
  海外投放: '📈',
  客服支持: '🎧',
  平面设计: '🎨',
  社媒运营: '📱',
  SEO优化: '🔎',
  网站开发: '💻',
  电商代运营: '🛒',
  跨境物流: '🚚',
  法律咨询: '⚖',
};

const defaultIcons = ['💡', '📊', '🛰', '🧠', '🧭', '🛠'];

const filteredCategories = computed(() => {
  const key = String(keyword.value || '').trim().toLowerCase();
  if (!key) return categories;
  return categories.filter((item) => String(item.label || '').toLowerCase().includes(key));
});

function getIcon(label) {
  if (iconMap[label]) return iconMap[label];
  const index = Math.abs(hashCode(label || 'x')) % defaultIcons.length;
  return defaultIcons[index];
}

function localizeCategoryLabel(label) {
  const text = String(label || '').trim();
  if (!text) return '';
  const i18nKey = CATEGORY_I18N_KEY_MAP[text];
  return i18nKey ? t(i18nKey) : text;
}

function hashCode(value) {
  let h = 0;
  for (let i = 0; i < value.length; i += 1) {
    h = (h << 5) - h + value.charCodeAt(i);
    h |= 0;
  }
  return h;
}
</script>

<style scoped>
.category-page {
  min-height: calc(100vh - 190px);
  background:
    radial-gradient(circle at 0% 10%, rgba(102, 126, 255, 0.14), transparent 34%),
    radial-gradient(circle at 100% 0%, rgba(70, 212, 240, 0.14), transparent 34%),
    linear-gradient(180deg, #eef4ff 0%, #f8fbff 45%, #fbfcff 100%);
}

.hero {
  position: relative;
  overflow: hidden;
  padding: 94px 20px 54px;
  background:
    radial-gradient(circle at 86% 14%, rgba(75, 236, 214, 0.32), transparent 32%),
    linear-gradient(132deg, #071a41 0%, #113068 52%, #19498f 100%);
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}

.light {
  position: absolute;
  border-radius: 50%;
  filter: blur(9px);
}

.light-a {
  width: 200px;
  height: 200px;
  left: -52px;
  top: -34px;
  background: rgba(105, 149, 255, 0.4);
}

.light-b {
  width: 150px;
  height: 150px;
  right: 8%;
  top: 18%;
  background: rgba(96, 235, 211, 0.3);
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
  letter-spacing: 0.14em;
  font-size: 12px;
  color: #bfd4ff;
}

.hero-inner h1 {
  margin: 10px 0 0;
  font-size: clamp(32px, 4vw, 52px);
}

.hero-inner p {
  margin: 14px auto 0;
  max-width: 760px;
  line-height: 1.75;
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
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.toolbar p {
  margin: 0;
  font-size: 13px;
  color: #4f688d;
}

.content {
  max-width: 1100px;
  margin: 22px auto 0;
  padding: 0 20px 28px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.category-card {
  text-decoration: none;
  color: inherit;
  border-radius: 18px;
  border: 1px solid rgba(218, 231, 255, 0.94);
  background: linear-gradient(168deg, rgba(255, 255, 255, 0.94), rgba(244, 250, 255, 0.84));
  box-shadow: 0 16px 25px rgba(30, 68, 136, 0.1);
  padding: 16px;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 34px rgba(24, 58, 117, 0.17);
  border-color: #bad5ff;
}

.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 22px;
  border: 1px solid #cae3ff;
  background: linear-gradient(135deg, #e6f2ff, #e1fff3);
}

.arrow {
  color: #3666a8;
  font-size: 18px;
}

.category-card h3 {
  margin: 14px 0 8px;
  font-size: 18px;
  color: #10254a;
}

.category-card p {
  margin: 0;
  color: #536b8f;
  line-height: 1.62;
  font-size: 14px;
}

.empty-box {
  margin-top: 24px;
  border-radius: 20px;
  border: 1px dashed #ceddf9;
  background: rgba(255, 255, 255, 0.6);
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
  .category-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .hero {
    padding-top: 86px;
  }

  .toolbar-wrap {
    position: static;
    margin-top: -20px;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }

  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .category-grid {
    grid-template-columns: 1fr;
  }
}
</style>
