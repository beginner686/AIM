import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import pinia from '@/store';
import { useUserStore } from '@/store/modules/user';

const service = axios.create({
  baseURL: import.meta.env.VITE_BASE_API || '/api',
  timeout: 15000,
});

function jumpToLogin() {
  const userStore = useUserStore(pinia);
  userStore.clearAuth();
  const currentRoute = router.currentRoute.value;
  if (currentRoute.path === '/login') {
    return;
  }
  router.replace({
    path: '/login',
    query: {
      redirect: currentRoute.fullPath,
    },
  });
}

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore(pinia);
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

service.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code === 0) {
        return payload.data;
      }
      if (payload.code === 40100) {
        ElMessage.error('登录已失效，请重新登录');
        jumpToLogin();
      } else {
        ElMessage.error(payload.message || '请求失败');
      }
      return Promise.reject(new Error(payload.message || 'Request failed'));
    }
    return payload;
  },
  (error) => {
    const status = error?.response?.status;
    const apiMessage = error?.response?.data?.message;
    if (status === 401) {
      ElMessage.error('登录已失效，请重新登录');
      jumpToLogin();
    } else {
      ElMessage.error(apiMessage || error.message || '网络异常');
    }
    return Promise.reject(error);
  },
);

export default service;
