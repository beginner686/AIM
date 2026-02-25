import { createRouter, createWebHistory } from 'vue-router';
import pinia from '@/store';
import { useUserStore } from '@/store/modules/user';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录',
      public: true,
    },
  },
  {
    path: '/',
    component: () => import('@/layout/BasicLayout.vue'),
    redirect: '/home',
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: {
          title: '首页',
          requiresAuth: true,
        },
      },
      {
        path: '/publish-demand',
        name: 'PublishDemand',
        component: () => import('@/views/PublishDemand.vue'),
        meta: {
          title: '发布需求',
          requiresAuth: true,
        },
      },
      {
        path: '/worker-pool',
        name: 'WorkerPool',
        component: () => import('@/views/WorkerPool.vue'),
        meta: {
          title: '执行者资源池',
          requiresAuth: true,
        },
      },
      {
        path: '/orders',
        name: 'OrderList',
        component: () => import('@/views/OrderList.vue'),
        meta: {
          title: '我的订单',
          requiresAuth: true,
        },
      },
      {
        path: '/order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: {
          title: '订单详情',
          requiresAuth: true,
        },
      },
      {
        path: '/user-center',
        name: 'UserCenter',
        component: () => import('@/views/UserCenter.vue'),
        meta: {
          title: '用户中心',
          requiresAuth: true,
        },
      },
      {
        path: '/admin',
        name: 'AdminDashboard',
        component: () => import('@/views/AdminDashboard.vue'),
        meta: {
          title: '管理后台',
          requiresAuth: true,
          roles: ['ADMIN'],
        },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const userStore = useUserStore(pinia);
  const isPublic = Boolean(to.meta.public);
  const requiresAuth = Boolean(to.meta.requiresAuth);
  const hasToken = userStore.isLogin;

  if (to.path === '/login' && hasToken) {
    next('/home');
    return;
  }

  if (!isPublic && requiresAuth && !hasToken) {
    next({
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    });
    return;
  }

  if (to.meta.roles?.length) {
    const role = userStore.userInfo?.role || '';
    if (!to.meta.roles.includes(role)) {
      next('/home');
      return;
    }
  }

  next();
});

router.afterEach((to) => {
  const title = to.meta?.title ? `AI-Link | ${to.meta.title}` : 'AI-Link';
  document.title = title;
});

export default router;
