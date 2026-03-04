---
name: vue-ui-styling
description: Vue 前端界面、样式与 UI/UX 规范指导。适用于处理 Vue 应用的页面绘制、响应式布局、Tailwind CSS/UnoCSS 或 Element Plus UI 组件库深度定制开发时。包含了微动画和设计系统统一认知。仅在处理前端样式展示层时触发。
---

# Vue UI/UX, Design Systems & Styling

## 隔离防御 ⚠️
**只操作前端！**不要干涉任何后端的响应 JSON 格式或者 Java Spring 设定的鉴权模型，本包仅负责最终的象素呈现。

## 主流方案组合
在当前的主流全栈应用中，要求界面有“极高质量的商业设计感”：
- **微组件体系**：如果你使用 Element Plus / Ant Design Vue，不要直接裸用原生颜色。需要覆盖它的预设 CSS 变量，定制带有圆角、极简边框以及适合主题色的设计（Glassmorphism 或现代化扁平）。
- **原子 CSS 工具**：如果你配合 Tailwind CSS 或 UnoCSS：
  - 用尽它们的 Utility class，不要轻易去内联写普通 CSS。
  - 不做简单的 `bg-blue-500`，请搭配色彩过渡：`hover:bg-blue-600 transition-colors duration-200` 等微小动画。

## UX 与动画体验规则
1. **绝不“闪现”**
   - 任何涉及列表增删、弹窗显示隐藏的 Vue 代码，强制要求外包 `<Transition>` 或 `<TransitionGroup>` 组件。
   - `v-if` 的切换应当自带优雅的 Opacity 或 Slide 渐变。
2. **占位与反馈**
   - 当调用后端的接口并在等待（例如加载数据或提单）时，必须提供局部或全局的 `Loading` / `Skeleton` 骨架屏骨架占位状态。决不允许存在因为网络慢导致的“白页挂起死亡凝视”。

## 结构与样式的解耦
- 尽量把纯粹的纯展示（Dumb/Presentational 组件）跟拥有副作用的页级（Smart/Container 组件）抽离开。展示层只接收 Props 和 Emit 事件。
