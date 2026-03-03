<template>
  <div class="landing-page">
    <section class="hero">
      <div class="orb orb-a" />
      <div class="orb orb-b" />
      <div class="orb orb-c" />

      <div class="hero-inner">
        <article class="hero-copy reveal-up">
          <p class="eyebrow">GLOBAL TASK MARKETPLACE</p>
          <h1>
            连接全球执行者
            <span class="gradient-text">让跨境履约稳定发生</span>
          </h1>
          <p class="lead">
            AI-Link 为出海团队提供可托管、可验收、可追踪的跨境任务撮合与履约协作。
            从发布需求到验收结算，全链路透明可控。
          </p>

          <div class="hero-actions">
            <button type="button" class="btn btn-primary" @click="openRegister">免费开始</button>
            <RouterLink to="/explore/demands" class="btn btn-ghost">浏览需求大厅</RouterLink>
          </div>

          <div class="hero-meta">
            <span>托管机制保障交易安全</span>
            <span>执行者接单后才解锁联系方式</span>
          </div>
        </article>

        <aside class="hero-panel reveal-up delay-1">
          <div class="panel-head">
            <p>平台实时概览</p>
            <span>Live</span>
          </div>
          <div class="kpi-grid">
            <div class="kpi-card">
              <p>开放需求</p>
              <strong>{{ formatNumber(stats?.demandCount, '1,200+') }}</strong>
            </div>
            <div class="kpi-card">
              <p>认证执行者</p>
              <strong>{{ formatNumber(stats?.workerCount, '500+') }}</strong>
            </div>
            <div class="kpi-card">
              <p>覆盖国家</p>
              <strong>{{ formatNumber(stats?.countryCount, '30+') }}</strong>
            </div>
          </div>

          <div class="pulse-list">
            <div v-for="(item, idx) in liveSignals" :key="item.title" class="pulse-row" :style="{ animationDelay: `${idx * 0.12}s` }">
              <span class="dot" />
              <div>
                <p>{{ item.title }}</p>
                <small>{{ item.desc }}</small>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <section class="trust-strip reveal-up">
      <p>被跨境电商、SaaS 团队、独立品牌和服务机构采用</p>
      <div class="trust-logos">
        <span>DTC Studio</span>
        <span>NovaGrowth</span>
        <span>BlueHarbor</span>
        <span>Atlas Commerce</span>
      </div>
    </section>

    <section class="value-section">
      <header class="section-head reveal-up">
        <p>平台能力</p>
        <h2>商业级履约协作体验</h2>
      </header>
      <div class="value-grid">
        <article v-for="(item, idx) in valueCards" :key="item.title" class="value-card reveal-up" :style="{ animationDelay: `${idx * 0.08}s` }">
          <div class="icon"><img :src="item.icon" :alt="item.title" /></div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <section class="category-section">
      <header class="section-head reveal-up">
        <p>热门赛道</p>
        <h2>高频跨境服务场景</h2>
      </header>
      <div class="category-grid">
        <RouterLink
          v-for="(item, idx) in topCategories"
          :key="item.name"
          :to="`/explore/demands?category=${encodeURIComponent(item.name)}`"
          class="category-card reveal-up"
          :style="{ animationDelay: `${idx * 0.06}s` }"
        >
          <div class="top">
            <span class="emoji"><img :src="item.icon" :alt="item.name" /></span>
            <span class="badge">{{ item.tag }}</span>
          </div>
          <h3>{{ item.name }}</h3>
          <p>{{ item.desc }}</p>
        </RouterLink>
      </div>
      <div class="category-action reveal-up delay-1">
        <RouterLink to="/explore/categories" class="inline-link">查看完整服务目录</RouterLink>
      </div>
    </section>

    <section class="process-section">
      <header class="section-head reveal-up">
        <p>交易流程</p>
        <h2>从发布到履约，路径清晰</h2>
      </header>
      <div class="timeline">
        <article v-for="(step, idx) in processSteps" :key="step.title" class="timeline-card reveal-up" :style="{ animationDelay: `${idx * 0.1}s` }">
          <span class="index">0{{ idx + 1 }}</span>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </article>
      </div>
    </section>

    <section class="cta-section reveal-up">
      <div class="cta-bg" />
      <div class="cta-inner">
        <h2>现在开始你的跨境任务协作</h2>
        <p>发布需求、发现执行者、推进订单状态，构建可持续的全球化执行能力。</p>
        <div class="cta-actions">
          <button type="button" class="btn btn-primary" @click="openRegister">我是雇主，发布任务</button>
          <button type="button" class="btn btn-ghost" @click="openRegister">我是执行者，申请入驻</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getPublicStatsApi } from '@/api/public';
import { openAuthModal } from '@/composables/useAuthModal';

import iconEscrow from '@/assets/icons/icon-escrow.png';
import iconMatching from '@/assets/icons/icon-matching.png';
import iconCollaboration from '@/assets/icons/icon-collaboration.png';
import iconScalable from '@/assets/icons/icon-scalable.png';
import iconTranslation from '@/assets/icons/icon-translation.png';
import iconAds from '@/assets/icons/icon-ads.png';
import iconVideo from '@/assets/icons/icon-video.png';
import iconSupport from '@/assets/icons/icon-support.png';
import iconSocial from '@/assets/icons/icon-social.png';
import iconAssistant from '@/assets/icons/icon-assistant.png';

const stats = ref(null);

const liveSignals = [
  { title: '托管状态可追踪', desc: '订单关键节点自动记录' },
  { title: '接单前隐私保护', desc: '未接单前不暴露联系方式' },
  { title: '跨国协作可视化', desc: '进度与争议处理有据可查' },
];

const valueCards = [
  { icon: iconEscrow, title: '托管与验收', desc: '需求方付款后进入平台托管，按状态推进与验收，降低交易不确定性。' },
  { icon: iconMatching, title: '智能匹配', desc: '按国家、技能、价格区间快速筛选候选执行者，提高撮合效率。' },
  { icon: iconCollaboration, title: '跨境协作', desc: '面向多时区团队设计，统一订单状态、消息与留痕，减少沟通损耗。' },
  { icon: iconScalable, title: '商业可扩展', desc: '支持从单次任务到长期合作，逐步沉淀稳定履约网络。' },
];

const topCategories = [
  { name: '翻译本地化', icon: iconTranslation, tag: 'High Demand', desc: '文案、页面、客服脚本等内容的本地语言适配。' },
  { name: '海外投放', icon: iconAds, tag: 'Performance', desc: '多渠道投放优化与本地素材测试。' },
  { name: '视频剪辑', icon: iconVideo, tag: 'Creative', desc: '短视频与广告素材迭代，适配不同市场偏好。' },
  { name: '客服支持', icon: iconSupport, tag: 'Operation', desc: '跨语言售前售后响应，提升客户体验。' },
  { name: '社媒运营', icon: iconSocial, tag: 'Brand', desc: '账号内容发布与日常互动运营。' },
  { name: '远程助理', icon: iconAssistant, tag: 'Execution', desc: '流程执行、资料整理与协同支持。' },
];

const processSteps = [
  { title: '发布需求', desc: '明确国家、类目、预算与交付标准，进入公开需求池。' },
  { title: '选择执行者', desc: '基于能力画像进行筛选，创建订单并锁定协作关系。' },
  { title: '支付与接单', desc: '服务费支付后进入待接单，执行者确认后解锁联系与履约。' },
  { title: '验收结算', desc: '任务完成后验收与评价，形成长期可信合作关系。' },
];

function openRegister() {
  openAuthModal('register');
}

function formatNumber(value, fallback = '-') {
  if (typeof value !== 'number' || Number.isNaN(value)) return fallback;
  return value.toLocaleString('zh-CN');
}

onMounted(async () => {
  try {
    const data = await getPublicStatsApi();
    stats.value = data || null;
  } catch {
    stats.value = null;
  }
});
</script>

<style scoped>
.landing-page {
  --surface: rgba(255, 255, 255, 0.7);
  --surface-dark: rgba(8, 16, 40, 0.58);
  --border-light: rgba(255, 255, 255, 0.34);
  --ink-1: #f6f9ff;
  --ink-2: #cad5f3;
  --ink-dark: #0e1b34;
  --brand-a: #36c9f7;
  --brand-b: #73f2ba;
  --brand-c: #7aa2ff;
  display: flex;
  flex-direction: column;
  gap: 26px;
  padding-bottom: 28px;
  background:
    radial-gradient(circle at 0% 0%, rgba(50, 96, 255, 0.15), transparent 34%),
    radial-gradient(circle at 100% 20%, rgba(34, 211, 238, 0.13), transparent 34%),
    linear-gradient(180deg, #ecf2ff 0%, #f6f9ff 36%, #f9fbff 100%);
  color: var(--ink-dark);
}

.hero {
  position: relative;
  overflow: hidden;
  padding: 116px 22px 86px;
  background:
    radial-gradient(circle at 18% -6%, rgba(68, 125, 255, 0.58), transparent 33%),
    radial-gradient(circle at 88% 10%, rgba(61, 225, 226, 0.4), transparent 29%),
    linear-gradient(135deg, #08132f 0%, #0f2451 46%, #11386e 100%);
  border-bottom-left-radius: 28px;
  border-bottom-right-radius: 28px;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.orb-a {
  width: 280px;
  height: 280px;
  left: -70px;
  top: -60px;
  background: rgba(86, 140, 255, 0.5);
  animation: floatY 8s ease-in-out infinite;
}

.orb-b {
  width: 220px;
  height: 220px;
  right: 12%;
  top: 8%;
  background: rgba(65, 234, 203, 0.34);
  animation: floatY 10s ease-in-out infinite reverse;
}

.orb-c {
  width: 180px;
  height: 180px;
  right: -40px;
  bottom: 20px;
  background: rgba(137, 174, 255, 0.45);
  animation: floatY 7s ease-in-out infinite;
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: 24px;
  align-items: stretch;
}

.hero-copy {
  color: var(--ink-1);
}

.eyebrow {
  margin: 0 0 14px;
  letter-spacing: 0.14em;
  font-size: 12px;
  color: var(--ink-2);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4.1vw, 56px);
  line-height: 1.06;
  letter-spacing: -0.02em;
  font-weight: 800;
}

.gradient-text {
  display: block;
  margin-top: 8px;
  background: linear-gradient(92deg, var(--brand-a), var(--brand-b));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.lead {
  margin: 20px 0 0;
  max-width: 650px;
  font-size: 16px;
  line-height: 1.75;
  color: rgba(229, 238, 255, 0.9);
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn {
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  font-family: inherit;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  padding: 12px 20px;
  transition: transform 0.2s ease, box-shadow 0.25s ease, background 0.25s ease, color 0.25s ease;
}

.btn:hover {
  transform: translateY(-2px);
}

.btn-primary {
  color: #052241;
  background: linear-gradient(120deg, #75e9ff, #8fffb7);
  box-shadow: 0 12px 30px rgba(31, 227, 208, 0.34);
}

.btn-ghost {
  color: #ecf6ff;
  border: 1px solid rgba(212, 230, 255, 0.45);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.hero-meta {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: rgba(201, 218, 244, 0.9);
  font-size: 12px;
}

.hero-panel {
  position: relative;
  border-radius: 22px;
  padding: 16px;
  background: var(--surface-dark);
  border: 1px solid var(--border-light);
  backdrop-filter: blur(14px);
  box-shadow: 0 30px 56px rgba(2, 11, 34, 0.42);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--ink-1);
}

.panel-head p {
  margin: 0;
  font-size: 14px;
}

.panel-head span {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  color: #06273f;
  background: linear-gradient(90deg, #84f4ff, #9dffc8);
}

.kpi-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.kpi-card {
  border-radius: 14px;
  padding: 12px;
  border: 1px solid rgba(238, 246, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: var(--ink-1);
}

.kpi-card p {
  margin: 0;
  font-size: 11px;
  color: rgba(220, 232, 255, 0.82);
}

.kpi-card strong {
  margin-top: 8px;
  display: block;
  font-size: 22px;
  line-height: 1.1;
}

.pulse-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pulse-row {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(236, 246, 255, 0.16);
  background: rgba(255, 255, 255, 0.06);
  animation: fadeInUp 0.45s ease both;
}

.dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: #8af6e2;
  box-shadow: 0 0 0 6px rgba(138, 246, 226, 0.16);
}

.pulse-row p {
  margin: 0;
  font-size: 13px;
  color: #edf6ff;
}

.pulse-row small {
  color: rgba(201, 218, 244, 0.9);
}

.trust-strip {
  max-width: 1180px;
  margin: -22px auto 0;
  width: calc(100% - 44px);
  border-radius: 20px;
  padding: 14px 20px;
  background: var(--surface);
  border: 1px solid rgba(228, 238, 255, 0.9);
  backdrop-filter: blur(8px);
  box-shadow: 0 16px 26px rgba(45, 83, 165, 0.1);
}

.trust-strip p {
  margin: 0;
  font-size: 13px;
  color: #3a527a;
}

.trust-logos {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.trust-logos span {
  font-size: 13px;
  padding: 7px 12px;
  border-radius: 999px;
  color: #204a84;
  background: #f2f7ff;
  border: 1px solid #dbe8ff;
}

.value-section,
.category-section,
.process-section {
  max-width: 1180px;
  width: calc(100% - 44px);
  margin: 0 auto;
}

.section-head p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #4e6f9f;
}

.section-head h2 {
  margin: 8px 0 0;
  font-size: clamp(28px, 3.1vw, 40px);
  color: #0f1f3f;
  line-height: 1.14;
  letter-spacing: -0.01em;
}

.value-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.value-card {
  border-radius: 18px;
  padding: 18px;
  background: var(--surface);
  border: 1px solid rgba(231, 240, 255, 0.96);
  box-shadow: 0 18px 26px rgba(42, 76, 146, 0.08);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.value-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 24px 36px rgba(42, 76, 146, 0.14);
}

.icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, #eff7ff, #ddf8f4);
  border: 1px solid #d7e9ff;
}

.icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 14px;
}

.value-card h3 {
  margin: 14px 0 8px;
  font-size: 18px;
  color: #102347;
}

.value-card p {
  margin: 0;
  color: #4c6288;
  line-height: 1.65;
  font-size: 14px;
}

.category-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.category-card {
  text-decoration: none;
  color: inherit;
  border-radius: 18px;
  padding: 16px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.84), rgba(239, 247, 255, 0.72));
  border: 1px solid rgba(218, 230, 255, 0.92);
  box-shadow: 0 16px 26px rgba(30, 57, 112, 0.09);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 22px 36px rgba(30, 57, 112, 0.14);
  border-color: #bfd7ff;
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.emoji {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #eff7ff, #ddf8f4);
  border: 1px solid #d7e9ff;
}

.emoji img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.badge {
  font-size: 11px;
  color: #224f8d;
  background: #e8f2ff;
  border: 1px solid #cfe3ff;
  border-radius: 999px;
  padding: 5px 9px;
}

.category-card h3 {
  margin: 14px 0 8px;
  font-size: 18px;
  color: #0f2449;
}

.category-card p {
  margin: 0;
  line-height: 1.65;
  font-size: 14px;
  color: #4f678d;
}

.category-action {
  margin-top: 12px;
}

.inline-link {
  color: #1f4f99;
  text-decoration: none;
  font-weight: 700;
  border-bottom: 1px solid rgba(31, 79, 153, 0.32);
}

.timeline {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.timeline-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(216, 228, 250, 0.9);
  background: #fff;
  box-shadow: 0 14px 24px rgba(21, 53, 119, 0.07);
}

.index {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 24px;
  border-radius: 999px;
  font-size: 11px;
  color: #1b4b8c;
  background: #ecf4ff;
  border: 1px solid #d0e4ff;
}

.timeline-card h3 {
  margin: 12px 0 8px;
  font-size: 17px;
  color: #102449;
}

.timeline-card p {
  margin: 0;
  font-size: 14px;
  color: #4f678d;
  line-height: 1.65;
}

.cta-section {
  position: relative;
  overflow: hidden;
  max-width: 1180px;
  width: calc(100% - 44px);
  margin: 0 auto;
  border-radius: 24px;
  border: 1px solid rgba(204, 222, 255, 0.85);
}

.cta-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 30%, rgba(89, 194, 255, 0.34), transparent 34%),
    radial-gradient(circle at 88% 78%, rgba(83, 248, 183, 0.32), transparent 36%),
    linear-gradient(120deg, #082148 0%, #0e3272 46%, #175093 100%);
}

.cta-inner {
  position: relative;
  z-index: 1;
  padding: 42px 30px;
  text-align: center;
  color: #f2f8ff;
}

.cta-inner h2 {
  margin: 0;
  font-size: clamp(26px, 3vw, 40px);
  letter-spacing: -0.01em;
}

.cta-inner p {
  margin: 12px auto 0;
  max-width: 680px;
  color: rgba(215, 230, 255, 0.9);
  line-height: 1.7;
}

.cta-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.reveal-up {
  animation: fadeInUp 0.55s ease both;
}

.delay-1 {
  animation-delay: 0.12s;
}

@keyframes floatY {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-14px);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1080px) {
  .hero-inner {
    grid-template-columns: 1fr;
  }

  .value-grid,
  .timeline {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero {
    padding-top: 96px;
    border-bottom-left-radius: 20px;
    border-bottom-right-radius: 20px;
  }

  .landing-page {
    gap: 20px;
  }

  .value-grid,
  .category-grid,
  .timeline {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions,
  .cta-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
