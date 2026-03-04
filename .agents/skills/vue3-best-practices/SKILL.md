---
name: vue3-best-practices
description: 现代 Vue 3 前端代码规范与最佳实践。当用户或任务表明当前开发目录为 `frontend/`，或涉及 `.vue` 文件、Vite 构建时必须触发。包含使用 `<script setup>`、组合式 API (Composition API)、组件状态拆分的高级准则。如果当前目录或任务明确为 `backend/` 或纯 Java 领域，请**绝对禁止**套用此包。
---

# Vue 3 Best Practices & Component Architecture

## 隔离防御 ⚠️
**核心规则：**
- 此技能**仅在前端领域生效**（如在 `frontend/` 文件夹下，或处理 `.vue` / `.ts` / vite 相关文件时）。
- 绝不要用本技能的逻辑去指导后端的 Java 代码或后端架构设计。

## Vue 3 核心编码规范
1. **强制使用 `<script setup>` 语法糖**
   - 所有的 Vue 组件必须使用 `<script setup>` 而非旧版的 `export default { setup() {} }` 或 Options API。
   - 使用 TS 泛型支持，即 `<script setup lang="ts">`。
2. **响应式最佳实践**
   - 基础类型使用 `ref()`，复杂对象使用 `reactive()`。但不建议将大型数据结构全部包装入单个 `reactive`，避免解构失去响应式。
   - 大量使用 `computed()` 衍生状态，保持 `template` 渲染层极其薄脆不含任何复杂计算逻辑。
3. **宏与生命周期**
   - 统一使用 `defineProps` 和 `defineEmits`，优先采用基于类型的声明（TS 类型标注）。
   - 生命周期钩子（如 `onMounted`, `onUnmounted`）内逻辑尽量抽离为独立的 hooks 函数。

## 组合式函数 (Composables) 抽取
- 当一个组件的逻辑超过 150 行，或某个逻辑具备复用性时，必须将其剥离为独立的 Composable 函数（命名 `useXxx.ts`）。
- Composable 中应当同时包含响应式状态和相关的副作用操作，并且返回一个对象。

## 构建与性能 (Vite)
- 利用 Vite 的动态导入机制（`defineAsyncComponent`）拆分巨无霸组件，优化首屏体积。
- CSS 如果无特别必要，强烈建议使用 `<style scoped>` 以确保样式不污染全局。
