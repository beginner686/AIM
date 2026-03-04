import { createRouter, createWebHistory } from 'vue-router';
import pinia from '@/store';
import { useUserStore } from '@/store/modules/user';
import { USER_ROLE } from '@/dicts';

const routes = [
  // -------------------------
  // 公开前台页面
  // -------------------------
  {
    path: '/',
    component: () => import('@/layout/PublicLayout.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        name: 'Landing',
        component: () => import('@/views/public/Landing.vue'),
        meta: { title: '全球跨境人力服务平台', public: true },
      },
      {
        path: 'explore/demands',
        name: 'PublicDemandHall',
        component: () => import('@/views/public/DemandHall.vue'),
        meta: { title: '需求大厅', public: true },
      },
      {
        path: 'explore/workers',
        name: 'PublicWorkerShowcase',
        component: () => import('@/views/public/WorkerShowcase.vue'),
        meta: { title: '发现执行者', public: true },
      },
      {
        path: 'explore/categories',
        name: 'PublicCategoryPage',
        component: () => import('@/views/public/CategoryPage.vue'),
        meta: { title: '服务分类', public: true },
      },
    ],
  },

  // -------------------------
  // 登录/注册
  // -------------------------
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录',
      public: true,
    },
  },

  // -------------------------
  // 登录后工作台（BasicLayout）
  // -------------------------
  {
    path: '/_app', // 占位路径，子路由使用绝对路径
    component: () => import('@/layout/BasicLayout.vue'),
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
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/publish-demand',
        name: 'PublishDemand',
        component: () => import('@/views/PublishDemand.vue'),
        meta: {
          title: '发布需求',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/worker-pool',
        name: 'WorkerPool',
        component: () => import('@/views/WorkerPool.vue'),
        meta: {
          title: '执行者资源池',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/demand-applications',
        name: 'DemandApplications',
        component: () => import('@/views/DemandApplications.vue'),
        meta: {
          title: '需求申请管理',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/orders',
        name: 'OrderList',
        component: () => import('@/views/OrderList.vue'),
        meta: {
          title: '我的订单',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/order/checkout/:id',
        name: 'OrderCheckout',
        component: () => import('@/views/OrderCheckout.vue'),
        meta: {
          title: '订单结账',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/order/match/:id',
        name: 'OrderMatch',
        component: () => import('@/views/OrderMatch.vue'),
        meta: {
          title: '撮合解锁',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: {
          title: '订单详情',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/user-center',
        name: 'UserCenter',
        component: () => import('@/views/UserCenter.vue'),
        meta: {
          title: '用户中心',
          requiresAuth: true,
          disallowRoles: [USER_ROLE.ADMIN],
        },
      },
      {
        path: '/admin',
        name: 'AdminDashboard',
        component: () => import('@/views/AdminDashboard.vue'),
        meta: {
          title: '管理后台',
          requiresAuth: true,
          roles: [USER_ROLE.ADMIN],
        },
      },
    ],
  },

  // 兜底路由
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  }
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
  const role = userStore.userInfo?.role || '';
  const isAdmin = role === USER_ROLE.ADMIN;

  if (to.path === '/login' && hasToken) {
    next(isAdmin ? '/admin' : '/home');
    return;
  }

  // 针对原来 / 重定向到 /home 的兼容
  // 由于我们现在 / 指向了 Landing，不需要主动 redirect / 到 /home，
  // 但如果业务逻辑需要在登录后直接进 /，且用户已登录，我们可以考虑跳 /home。
  // 不过为了让已登录用户也能看主页，我们不需要拦截 /。

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
    if (!to.meta.roles.includes(role)) {
      next(isAdmin ? '/admin' : '/home');
      return;
    }
  }

  if (to.meta.disallowRoles?.length) {
    if (to.meta.disallowRoles.includes(role)) {
      next(isAdmin ? '/admin' : '/home');
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
