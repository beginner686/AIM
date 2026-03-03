<template>
  <div class="public-layout">
    <header class="pub-header" :class="{ scrolled: isScrolled }">
      <div class="pub-header-inner">
        <RouterLink to="/" class="pub-brand">
          <div class="brand-logo">
            <img :src="brandLogoUrl" alt="AI-Link" />
          </div>
          <span class="brand-name">AI-Link</span>
        </RouterLink>

        <nav class="pub-nav">
          <RouterLink to="/" class="nav-item" exact-active-class="active">{{ $t('nav.home') }}</RouterLink>
          <RouterLink to="/explore/demands" class="nav-item" active-class="active">{{ $t('nav.demands') }}</RouterLink>
          <RouterLink to="/explore/workers" class="nav-item" active-class="active">{{ $t('nav.workers') }}</RouterLink>
          <RouterLink to="/explore/categories" class="nav-item" active-class="active">{{ $t('nav.categories') }}</RouterLink>
        </nav>

        <div class="pub-actions">
          <LanguageSwitcher />
          <template v-if="isLogin">
            <RouterLink to="/home" class="action-btn action-primary">
              <span>{{ $t('action.workspace') }}</span>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M6 3L11 8L6 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </RouterLink>
          </template>
          <template v-else>
            <button class="action-btn action-ghost" type="button" @click="handleOpenAuth('login')">{{ $t('action.login') }}</button>
            <button class="action-btn action-primary" type="button" @click="handleOpenAuth('register')">
              <span>{{ $t('action.register') }}</span>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M6 3L11 8L6 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </template>
        </div>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="pub-footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand-col">
            <div class="footer-brand">
              <div class="brand-logo small">
                <img :src="brandLogoUrl" alt="AI-Link" />
              </div>
              <span>AI-Link</span>
            </div>
            <p class="footer-tagline">{{ $t('footer.tagline') }}</p>
          </div>
          <div class="footer-links-col">
            <div class="footer-group">
              <h4>{{ $t('footer.platform') }}</h4>
              <RouterLink to="/explore/demands">{{ $t('nav.demands') }}</RouterLink>
              <RouterLink to="/explore/workers">{{ $t('nav.workers') }}</RouterLink>
              <RouterLink to="/explore/categories">{{ $t('nav.categories') }}</RouterLink>
            </div>
            <div class="footer-group">
              <h4>{{ $t('footer.join') }}</h4>
              <button type="button" class="footer-link-btn" @click="handleOpenAuth('register')">{{ $t('action.registerEmployer') }}</button>
              <button type="button" class="footer-link-btn" @click="handleOpenAuth('register')">{{ $t('action.registerWorker') }}</button>
              <button type="button" class="footer-link-btn" @click="handleOpenAuth('login')">{{ $t('action.loginAccount') }}</button>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <span>© {{ new Date().getFullYear() }} {{ $t('footer.copyright') }}</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useUserStore } from '@/store/modules/user';
import { openAuthModal } from '@/composables/useAuthModal';
import LanguageSwitcher from '@/components/LanguageSwitcher.vue';

const userStore = useUserStore();
const isLogin = computed(() => userStore.isLogin);
const isScrolled = ref(false);
const brandLogoUrl = '/brand-logo.png';

function handleScroll() {
  isScrolled.value = window.scrollY > 10;
}

function handleOpenAuth(tab) {
  openAuthModal(tab);
}

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }));
onUnmounted(() => window.removeEventListener('scroll', handleScroll));
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* ====== HEADER ====== */
.pub-header {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 200;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent;
}
.pub-header.scrolled {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.pub-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 140px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  gap: 40px;
}

.pub-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-logo {
  width: 128px;
  height: 128px;
  border-radius: 10px;
  overflow: hidden;
}
.brand-logo.small {
  width: 128px;
  height: 128px;
}
.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.brand-name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.5px;
}
.pub-header:not(.scrolled) .brand-name {
  color: #fff;
}

.pub-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.nav-item {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
}
.pub-header.scrolled .nav-item {
  color: #64748b;
}
.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}
.pub-header.scrolled .nav-item:hover {
  color: #0f172a;
  background: #f1f5f9;
}
.nav-item.active {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.15);
}
.pub-header.scrolled .nav-item.active {
  color: #0ea5e9 !important;
  background: #f0f9ff;
}

.pub-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: none;
}
.action-ghost {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
}
.pub-header.scrolled .action-ghost {
  color: #475569;
  background: transparent;
  border: 1px solid #e2e8f0;
}
.action-ghost:hover { background: rgba(255, 255, 255, 0.18); }
.pub-header.scrolled .action-ghost:hover { background: #f8fafc; border-color: #cbd5e1; }

.action-primary {
  color: #fff;
  background: linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%);
  box-shadow: 0 2px 12px rgba(14, 165, 233, 0.35);
}
.action-primary:hover {
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.5);
  transform: translateY(-1px);
}

/* ====== FOOTER ====== */
.pub-footer {
  background: #0b1120;
  padding: 64px 32px 32px;
  margin-top: auto;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.footer-top {
  display: flex;
  justify-content: space-between;
  gap: 64px;
  padding-bottom: 40px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.footer-brand-col { max-width: 360px; }
.footer-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
  margin-bottom: 16px;
}
.footer-tagline {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}
.footer-links-col {
  display: flex;
  gap: 80px;
}
.footer-group h4 {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 20px;
}
.footer-group a {
  display: block;
  color: #64748b;
  text-decoration: none;
  font-size: 14px;
  padding: 6px 0;
  transition: color 0.2s;
}
.footer-group a:hover { color: #e2e8f0; }
.footer-link-btn {
  display: block;
  width: 100%;
  text-align: left;
  color: #64748b;
  background: transparent;
  border: none;
  font-size: 14px;
  padding: 6px 0;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s;
}
.footer-link-btn:hover { color: #e2e8f0; }

.footer-bottom {
  padding-top: 32px;
  text-align: center;
}
.footer-bottom span {
  font-size: 13px;
  color: #475569;
}

main { flex: 1; padding-top: 0; }

@media (max-width: 768px) {
  .pub-header-inner { padding: 0 16px; gap: 16px; height: 64px; }
  .pub-nav { display: none; }
  .pub-footer { padding: 40px 16px 24px; }
  .footer-top { flex-direction: column; gap: 40px; }
  .footer-links-col { gap: 40px; }
}
</style>
