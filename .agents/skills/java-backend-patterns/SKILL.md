---
name: java-backend-patterns
description: Java 后端微服务最佳实践。针对 Java、Spring Boot 3、Spring Cloud、JPA/MyBatis 架构，处理 Controller-Service-Repository 经典分层、DTO/VO 转换、统一异常处理、RESTful 设计、微服务调用、以及强类型约束等最佳实践。只要涉及到 Java 后端业务开发或脚手架搭建，就必须强制触发并遵循此包规范！
---

# Java Backend Patterns (Spring Boot & Cloud)

## 适用场景
当你使用 Java 并且涉及 Spring Boot 3、Spring Cloud 或数据持久化（Spring Data JPA / MyBatis）时，必须遵循本规范。不要随意打破分层。

## 架构分层规约
1. **Controller (API) 层**
   - 仅负责控制层逻辑：接收 HTTP 请求、参数基本校验（使用 `@Valid` 或 `@Validated`）、调用 Service、构造返回结果并统一封装结构（如 `Result<T>`）。
   - **严禁** 在 Controller 层写真正的业务逻辑判断。
2. **Service (业务层)**
   - 核心业务逻辑所在之地。若需要组合调用多个不同的业务，在此处完成聚合。
   - 对于涉及写操作的方法，在此层增加 `@Transactional` 注解保证事务一致性。
3. **Repository / DAO (持久层)**
   - 只负责与数据库的直接读写交互，不做其他复杂业务。
   - 如果使用 Spring Data JPA，使用规范接口名；如果使用 MyBatis，规范编写 Mapper。
4. **DTO/VO 模型隔离**
   - **严禁** 将查询得到的 Entity/DO（数据库直接映射体）直接通过 Controller 返回给前端。必须转换为 VO (Value Object)。
   - 接口接收入参必须使用专用的 DTO (Data Transfer Object) 类，不要使用 Map 或者 Entity 接收。

## Spring Boot 3 专有实践
- 基于 Java 17+ 特性，推荐使用 `record` 来声明简洁的 DTO/VO。
- 推荐使用文本块（Text Blocks `"""`）来编写复杂的内联 SQL 或 JSON 字符串，提高可读性。
- 代码生成优先采用依赖注入的构造器注入模式（配合 lombok `@RequiredArgsConstructor`），而非字段注入 `@Autowired`。

## RESTful & 全局规范
- API 路径应符合 REST 规范：使用复数名词、连字符（kebab-case），用正确的 HTTP Method 表达操作意图（GET/POST/PUT/DELETE）。
- 必须具备**全局异常处理机制**（`@RestControllerAdvice`），将系统内各种异常统一转换为标准的 JSON 告警代码和描述。

## 微服务交互
- 在业务线切分时，模块化处理，RPC 调用（如 OpenFeign / Dubbo）必须包含超时设置。
- 高可用和容错机制：推荐配套如 Resilience4j 的 fallback 退路。
