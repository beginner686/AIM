import { defineStore } from 'pinia';
import { clearToken, getToken, setToken } from '@/utils/auth';

const USER_INFO_KEY = 'ailink_user_info';

function readUserInfo() {
  const cached = localStorage.getItem(USER_INFO_KEY);
  if (!cached) {
    return null;
  }
  try {
    return JSON.parse(cached);
  } catch {
    return null;
  }
}

function writeUserInfo(userInfo) {
  if (!userInfo) {
    localStorage.removeItem(USER_INFO_KEY);
    return;
  }
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo));
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: readUserInfo(),
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
  },
  actions: {
    setLogin(payload) {
      const token = payload?.token || '';
      const user = payload?.user || null;
      this.token = token;
      this.userInfo = user;
      setToken(token);
      writeUserInfo(user);
    },
    setUserInfo(user) {
      this.userInfo = user || null;
      writeUserInfo(this.userInfo);
    },
    clearAuth() {
      this.token = '';
      this.userInfo = null;
      clearToken();
      writeUserInfo(null);
    },
  },
});
