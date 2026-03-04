---
name: vue-ecosystem-patterns
description: 现代 Vue 状态管理与路由设计模式。针对 Pinia 状态树和 Vue Router 4 的最佳组织实践。当涉及全局状态、跨页面传参、路由权限守卫以及前端项目目录规划时必须参考。同样：仅在前端上下文(`frontend/`, `.vue`等)生效。
---

# Vue Ecosystem Patterns: Pinia & Vue Router 4

## 隔离防御 ⚠️
此规定**只对前端项目结构有效**。绝对不要混入后端的任何业务控制流中。

## 目录分层架构
推荐在前端项目（`frontend/src/`）采用如下分层：
- `/views` 或 `/pages`: 页面级路由组件。
- `/components`: 全局通用及业务复用组件。
- `/store`: Pinia 状态管理模块。
- `/router`: 路由注册与拦截器防腐层。
- `/composables`: 纯逻辑 hooks 的堆放处。
- `/api`: 与后端（Java）交互的纯函数与模型定义层。

## Pinia 状态管理规范
1. **抛弃 Vuex 概念**
   - 坚决不要写 `mutations`，Pinia 仅需使用 `state`, `getters`, `actions`。
2. **Setup Store 风格**
   - 强烈推荐使用 Setup Store 风格定义 Store：
     ```typescript
     export const useUserStore = defineStore('user', () => {
       const userInfo = ref(null);
       const isLoggedIn = computed(() => !!userInfo.value);
       function login() { /* ... */ }
       return { userInfo, isLoggedIn, login };
     });
     ```
3. **扁平化设计**
   - 不要试图设计出包含极深层级的一颗巨型 Store 树。按业务（User / Cart / Settings）拆分多个 Store 模块。

## Vue Router 4 高级实践
1. **路由懒加载强约束**
   - 所有的 View 页面引包必须是 `() => import('@/views/xxx.vue')`，禁止在顶部发生同步全量引入。
2. **路由元数据 (Meta)**
   - 需要权限校验的路由必须注入 `meta: { requiresAuth: true, role: 'ADMIN' }`，方便拦截器全局统一收口权限判断。
3. **导航守卫防腐层**
   - 在 `router.beforeEach` 中对进入页面的 Token、请求权限和非法重定向做统一处理。
