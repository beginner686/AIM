import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import pinia from '@/store';
import { useUserStore } from '@/store/modules/user';
import { openAuthModal } from '@/composables/useAuthModal';

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
  if (currentRoute.meta?.public) {
    openAuthModal('login');
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
    const requestUrl = String(response?.config?.url || '');
    const isAuthLoginRequest = requestUrl.includes('/auth/login');
    const silentBusinessError = Boolean(response?.config?.silentBusinessError);
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code === 0) {
        return payload.data;
      }
      if (payload.code === 40100) {
        if (isAuthLoginRequest) {
          if (!silentBusinessError) {
            ElMessage.error(payload.message || '用户名或密码错误');
          }
        } else if (!silentBusinessError) {
          ElMessage.error('登录已失效，请重新登录');
          jumpToLogin();
        }
      } else if (!silentBusinessError) {
        ElMessage.error(payload.message || '请求失败');
      }
      const bizError = new Error(payload.message || 'Request failed');
      bizError.bizCode = payload.code;
      bizError.bizPayload = payload;
      return Promise.reject(bizError);
    }
    return payload;
  },
  (error) => {
    const status = error?.response?.status;
    const apiMessage = error?.response?.data?.message;
    const requestUrl = String(error?.config?.url || '');
    const isAuthLoginRequest = requestUrl.includes('/auth/login');
    const ignoredStatuses = error?.config?.ignoreErrorStatuses;
    const isIgnoredStatus = Array.isArray(ignoredStatuses) && ignoredStatuses.includes(status);
    const silentHttpError = Boolean(error?.config?.silentHttpError);
    if (status === 401 && !isAuthLoginRequest && !silentHttpError) {
      ElMessage.error('登录已失效，请重新登录');
      jumpToLogin();
    } else if (!isIgnoredStatus && !silentHttpError) {
      ElMessage.error(apiMessage || error.message || '网络异常');
    }
    return Promise.reject(error);
  },
);

export default service;
