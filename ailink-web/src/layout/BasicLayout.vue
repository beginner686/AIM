<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <!-- 品牌 -->
        <RouterLink to="/home" class="brand">
          <span class="brand-icon">◆</span>
          <span class="brand-text">AI-Link</span>
        </RouterLink>

        <!-- 导航 -->
        <nav class="nav">
          <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-link">
            {{ item.label }}
          </RouterLink>
        </nav>

        <!-- 右侧 -->
        <div class="header-right">
          <div class="user-chip">
            <span class="user-avatar">{{ avatarLetter }}</span>
            <span class="user-name">{{ userStore.userInfo?.username || '未登录' }}</span>
          </div>
          <button class="logout-btn" @click="logout">退出</button>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const userStore = useUserStore();

const avatarLetter = computed(() => {
  const name = userStore.userInfo?.username || '';
  return name.charAt(0).toUpperCase() || 'U';
});

const navItems = [
  { to: '/home', label: '首页' },
  { to: '/publish-demand', label: '发布需求' },
  { to: '/worker-pool', label: '执行者' },
  { to: '/orders', label: '我的订单' },
  { to: '/user-center', label: '个人中心' },
  { to: '/admin', label: '管理' },
];

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
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.header-inner {
  max-width: 1320px;
  margin: 0 auto;
  height: 60px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 品牌 */
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0b4b6f, #0f766e);
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
}
.brand-text {
  font-size: 18px;
  font-weight: 800;
  color: var(--color-text);
  letter-spacing: -0.3px;
}

/* 导航链接 */
.nav {
  display: flex;
  align-items: center;
  gap: 4px;
}
.nav-link {
  position: relative;
  padding: 6px 14px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border-radius: var(--radius-sm);
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
  gap: 12px;
  flex-shrink: 0;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  background: var(--color-bg);
}
.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0b4b6f, #0f766e);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}
.logout-btn {
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
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
