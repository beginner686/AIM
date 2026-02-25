<template>
  <div class="layout">
    <header class="header">
      <div class="brand">AI-Link</div>
      <nav class="nav">
        <RouterLink to="/home">首页</RouterLink>
        <RouterLink to="/publish-demand">发布需求</RouterLink>
        <RouterLink to="/worker-pool">执行者资源池</RouterLink>
        <RouterLink to="/user-center">用户中心</RouterLink>
        <RouterLink to="/admin">管理后台</RouterLink>
      </nav>
      <div class="right">
        <span class="user">{{ userStore.userInfo?.username || '未登录' }}</span>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const userStore = useUserStore();

function logout() {
  userStore.clearAuth();
  router.push('/login');
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.brand {
  font-weight: 700;
}

.nav {
  display: flex;
  gap: 12px;
  font-size: 14px;
}

.right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user {
  font-size: 12px;
  color: #6b7280;
}

.content {
  padding: 16px;
}
</style>
