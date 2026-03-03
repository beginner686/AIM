import { createI18n } from 'vue-i18n';
import zhCN from './zh-CN.json';
import enUS from './en-US.json';

const messages = {
    'zh-CN': zhCN,
    'en-US': enUS,
};

const getInitialLocale = () => {
    const storedLocale = localStorage.getItem('app-locale');
    if (storedLocale && messages[storedLocale]) {
        return storedLocale;
    }
    const browserLang = navigator.language || navigator.userLanguage;
    return browserLang.includes('zh') ? 'zh-CN' : 'en-US';
};

const i18n = createI18n({
    legacy: false, // 启用 Composition API
    locale: getInitialLocale(),
    fallbackLocale: 'en-US',
    messages,
    globalInjection: true,
});

export default i18n;
