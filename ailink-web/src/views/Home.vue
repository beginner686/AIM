<template>
  <el-card shadow="never">
    <template #header>首页</template>
    <p>第一章已完成：请求封装、路由、Pinia、登录态与401处理。</p>
    <el-space>
      <el-button type="primary" @click="loadProfile">请求 /api/user/me</el-button>
      <span v-if="profile">当前用户：{{ profile.username }}（{{ profile.role }}）</span>
    </el-space>
  </el-card>
</template>

<script setup>
import { ref } from 'vue';
import { getCurrentUserApi } from '@/api/user';
import { useUserStore } from '@/store/modules/user';

const userStore = useUserStore();
const profile = ref(null);

async function loadProfile() {
  const data = await getCurrentUserApi();
  profile.value = data;
  userStore.setUserInfo(data);
}
</script>
