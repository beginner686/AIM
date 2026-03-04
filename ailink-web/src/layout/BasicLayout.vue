<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <!-- 品牌 -->
        <RouterLink :to="homePath" class="brand">
          <span class="brand-logo">
            <img :src="brandLogoUrl" alt="AI-Link" />
          </span>
          <span class="brand-text">AI-Link</span>
        </RouterLink>

        <!-- 导航 -->
        <nav class="nav">
          <RouterLink v-for="item in navItems" :key="item.key" :to="item.to" class="nav-link">
            {{ item.label }}
          </RouterLink>
        </nav>

        <!-- 右侧 -->
        <div class="header-right">
          <div class="user-chip">
            <span class="user-avatar">{{ avatarLetter }}</span>
            <span class="user-name">{{ userStore.userInfo?.username || $t('layout.notLoggedIn') }}</span>
          </div>
          <button class="logout-btn" @click="logout">{{ $t('layout.logout') }}</button>
        </div>
      </div>
      <div class="header-rgb-border"></div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/store/modules/user';
import { USER_ROLE } from '@/dicts';

const { t } = useI18n();

const router = useRouter();
const userStore = useUserStore();
const brandLogoUrl = '/brand-logo.png';
const isAdmin = computed(() => userStore.userInfo?.role === USER_ROLE.ADMIN);
const homePath = computed(() => (isAdmin.value ? '/admin' : '/home'));

const avatarLetter = computed(() => {
  const name = userStore.userInfo?.username || '';
  return name.charAt(0).toUpperCase() || 'U';
});

const adminNavItems = computed(() => [
  { key: 'admin-overview', to: '/admin', label: t('layout.adminOverview') },
  { key: 'admin-worker-apply', to: { path: '/admin', query: { tab: 'workerApply' } }, label: t('layout.workerApply') },
  { key: 'admin-fees', to: { path: '/admin', query: { tab: 'fees' } }, label: t('layout.feeConfig') },
]);

const userNavItems = computed(() => [
  { key: 'home', to: '/home', label: t('layout.home') },
  { key: 'publish-demand', to: '/publish-demand', label: t('layout.publishDemand') },
  { key: 'worker-pool', to: '/worker-pool', label: t('layout.executor') },
  { key: 'demand-applications', to: '/demand-applications', label: t('layout.demandApplications') },
  { key: 'orders', to: '/orders', label: t('layout.myOrders') },
  { key: 'user-center', to: '/user-center', label: t('layout.userCenter') },
]);

const navItems = computed(() => (isAdmin.value ? adminNavItems.value : userNavItems.value));

function logout() {
  userStore.clearAuth();
  router.push('/login');
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部导航栏 ===== */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

.header-rgb-border {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(
    90deg,
    #3b82f6,
    #8b5cf6,
    #ec4899,
    #3b82f6,
    #06b6d4,
    #3b82f6
  );
  background-size: 200% 100%;
  animation: rgbFlow 4s linear infinite;
  opacity: 0.8;
}

@keyframes rgbFlow {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}


.header-inner {
  max-width: 1320px;
  margin: 0 auto;
  height: 140px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 品牌 */
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-logo {
  width: 128px;
  height: 128px;
  border-radius: 10px;
  overflow: hidden;
}
.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.brand-text {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.5px;
  line-height: 1;
}

/* 导航链接 */
.nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-link {
  position: relative;
  padding: 8px 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-radius: 10px;
  transition: color 0.2s, background 0.2s;
}
.nav-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}
.nav-link.router-link-active {
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-soft);
}

/* 右侧用户区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  border-radius: 26px;
  background: var(--color-bg);
}
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0b4b6f, #0f766e);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.logout-btn {
  padding: 6px 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.logout-btn:hover {
  color: #ef4444;
  border-color: #fca5a5;
  background: rgba(239, 68, 68, 0.06);
}

/* ===== 主内容区 ===== */
.main-content {
  flex: 1;
  max-width: 1320px;
  width: 100%;
  margin: 0 auto;
  padding: 28px;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .header-inner {
    padding: 0 16px;
  }
  .nav {
    gap: 2px;
  }
  .nav-link {
    padding: 6px 10px;
    font-size: 13px;
  }
  .main-content {
    padding: 20px 16px;
  }
}
</style>
