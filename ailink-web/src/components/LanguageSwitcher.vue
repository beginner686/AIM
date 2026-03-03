<template>
  <div class="lang-switcher">
    <div class="lang-selector" @click="toggleDropdown" ref="selectorRef">
      <div class="current-lang">
        <svg v-if="currentLanguage === 'zh-CN'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/></svg>
        <span>{{ currentLanguage === 'zh-CN' ? '简体中文' : 'English' }}</span>
        <svg class="chevron" :class="{ open: isOpen }" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
      </div>
    </div>
    <div v-show="isOpen" class="lang-dropdown">
      <div 
        class="lang-option" 
        :class="{ active: currentLanguage === 'zh-CN' }"
        @click="changeLanguage('zh-CN')"
      >
        简体中文
      </div>
      <div 
        class="lang-option" 
        :class="{ active: currentLanguage === 'en-US' }"
        @click="changeLanguage('en-US')"
      >
        English
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { locale } = useI18n();
const isOpen = ref(false);
const selectorRef = ref(null);

const currentLanguage = computed(() => locale.value);

const toggleDropdown = () => {
  isOpen.value = !isOpen.value;
};

const changeLanguage = (lang) => {
  locale.value = lang;
  localStorage.setItem('app-locale', lang);
  isOpen.value = false;
};

// 点击外部关闭下拉框
const handleClickOutside = (event) => {
  if (selectorRef.value && !selectorRef.value.contains(event.target)) {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.lang-switcher {
  position: relative;
  display: inline-block;
}

.lang-selector {
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.1);
}
.lang-selector:hover {
  background: rgba(255, 255, 255, 0.2);
}

.current-lang {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: inherit;
  font-weight: 500;
}

.chevron {
  transition: transform 0.2s ease;
}
.chevron.open {
  transform: rotate(180deg);
}

.lang-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 140px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 8px;
  z-index: 1000;
  border: 1px solid rgba(0,0,0,0.05);
}

.lang-option {
  padding: 8px 12px;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.lang-option:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.lang-option.active {
  background: #e0f2fe;
  color: #0ea5e9;
  font-weight: 500;
}

/* 适配白底导航栏的状态 */
:deep(.scrolled) .lang-selector {
  background: rgba(0, 0, 0, 0.05);
  color: #475569;
}
:deep(.scrolled) .lang-selector:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #0f172a;
}
</style>
