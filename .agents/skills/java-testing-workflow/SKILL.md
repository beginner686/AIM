---
name: java-testing-workflow
description: Java 单元测试与 TDD 工作流。当进行 Java 代码编写、Bug 修复、特性增加，或者用户提及 JUnit 5、Mockito、Mock 测试、单元测试或 TDD (测试驱动开发) 时，必须触发。旨在强制推行红-绿-重构循环，保障高代码覆盖率与稳定的用例。
---

# Java Testing Workflow & TDD

## 工具栈要求
代码项目级的单元测试强制采用 **JUnit 5 (Jupiter)** 和 **Mockito** 进行编写。请抛弃旧版的 JUnit 4（如 `@RunWith`, `org.junit.Test` 等）。

## 测试驱动开发 (TDD) 流程核心
务必引导代理或者在自己自主写代码时，遵守以下循环：
1. **红 (Red)**：在没有写生产代码前，先写一个失败的单元测试来表达预期目标。
2. **绿 (Green)**：仅编写足以让该测试通过的最简化的生产代码，不过度设计。
3. **重构 (Refactor)**：保证功能正确的前提下，抽取代码、提升设计模式、消除重复代码，确保用例依然全是绿色。

## Java 编排范式
1. **AAA 模式切分**  
   任何测试方法都必须清晰地分为三大段：
   - `Arrange` / `Given`（准备测试数据、构建 Mock 环境）
   - `Act` / `When`（调用目标测试方法）
   - `Assert` / `Then`（使用 AssertJ, Hamcrest 或 JUnit Assertions 验证返回值及其对 Mock 的副作用交互）
   
2. **完美的隔离性**  
   单元测试绝不应该连接真实的数据库或调用真实的外部 API 网络。所有位于被测类之外的依赖服务都必须使用 `@Mock` 模拟其行为。使用 `@InjectMocks` 注解快速让 Spring 或 Mockito 将依赖注入到你的被测对象里。

3. **命名规约**  
   使用具有业务描述性的测试命名风格，例如：
   - `should_ReturnSuccessStatus_When_CredentialsAreValid()`
   - 或遵循 BDD 命名 `given_when_then`。

4. **关于 Spring Context**  
   单元测试不应使用 `@SpringBootTest` 启动整个大容器。它仅仅测试单个组件逻辑。对于专门的“集成测试”才启用全量上下文环境。
