---
name: java-coding-standards
description: 阿里巴巴 Java 开发规约 / Google Java 代码质量检查包。用于任何 Java 开发、Review、重构。要求代理强制遵守 Alibaba / Google / SonarQube 最严格的命名、格式、并发、NPE检查等准则，如同安装了本地的规约插件并开启严苛审查。
---

# Java Coding Standards & Quality Assurance

当进行任何 Java 代码编写时，你就是一名严苛的高级架构师，你的产出代码必须默认吻合《阿里巴巴 Java 开发手册》中的最核心约束，并兼顾 Google Java Style 的格式。

## 高频代码质量核对项

### 1. 命名与格式
- 类名必须使用 `UpperCamelCase`，方法名与变量名必须使用 `lowerCamelCase`。
- 常量所有字母全大写，单词间用下划线隔开（如 `MAX_LIMIT_COUNT`）。
- **千万不要**包含无意义的单词（如 `info`, `data` 作为后缀时尽量具象化表达）。不要使用拼音。
- Google Style 缩进：缩进层级分明，空行和换行符合经典 Java 流水线。

### 2. NPE（NullPointerException） 防御
- 自动封箱拆箱的坑：防止 `Long/Integer` 出现 null 时拆箱引发 NPE。
- 返回值：永远不要在方法返回集合时返回 `null`，应使用 `Collections.emptyList()` 等空集合替代。
- 可选值交互：大量提倡 Java 8 的 `Optional<T>` 封装潜在为 null 的实体。
- 字符串比对：`"SUCCESS".equals(status)`，把确认非空常量或者对象放前面，避免空指针对象调用 `equals`。

### 3. 数据结构与集合处理
- 使用泛型：定义集合必须写明尖括号里的具体类型 `List<String> list = new ArrayList<>();`
- 集合清除：不可以在 foreach 循环里进行集合元素的 `remove/add` 操作。推荐使用 `Iterator.remove()` 或 Java 8 特性 `removeIf()`。
- 工具类的取舍：不要使用废弃的 `Hashtable` / `Vector`，理解 `ConcurrentHashMap` 的使用。

### 4. 并发与多线程
- **严禁魔法规制**：绝不允许使用硬编码的神仙数字，全部抽做常量或配置。
- 线程池规范：禁止使用 `Executors` 自带的方法创建线程池（规避 OOM 风险），必须强行通过 `ThreadPoolExecutor` 自定义参数，明确指定阻塞队列的大小。
- `ThreadLocal`：强行要求用完之后使用 `remove()` 清理，避免内存泄露以及线程复用拿错值。

### 5. 面向对象与修饰符
- 注释完备度：所有的公有类以及公有方法必须书写完整的 Javadoc 块 `/** ... */`，包括其作用、`@param`、`@return` 和 `@throws`。
- 不要给接口里面的方法冗余添加 `public abstract` 修饰符，接口里保持代码最简纯净。
