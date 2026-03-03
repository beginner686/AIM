import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from '@/router';
import pinia from '@/store';
import i18n from '@/locales';
import { initDictCenter } from '@/dicts';
import { formatMoney } from '@/utils/format';
import '@/styles/index.css';

async function bootstrap() {
  await initDictCenter();

  const app = createApp(App);

  app.use(pinia);
  app.use(router);
  app.use(i18n);
  app.use(ElementPlus);

  app.config.globalProperties.$formatMoney = formatMoney;
  app.provide('formatMoney', formatMoney);

  app.mount('#app');
}

bootstrap();
