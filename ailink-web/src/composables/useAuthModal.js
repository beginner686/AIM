import { reactive } from 'vue';

const DEFAULT_REDIRECT = '';

export const authModalState = reactive({
  visible: false,
  tab: 'login',
  redirect: DEFAULT_REDIRECT,
});

export function openAuthModal(tab = 'login', options = {}) {
  const safeTab = tab === 'register' ? 'register' : 'login';
  authModalState.tab = safeTab;
  authModalState.redirect = String(options?.redirect || DEFAULT_REDIRECT);
  authModalState.visible = true;
}

export function closeAuthModal() {
  authModalState.visible = false;
}
