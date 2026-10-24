# AI Interview Assistant

基于 Spring Boot 和 Spring AI 的本地 AI 面试助手。

## 当前阶段

- F01 环境预检：已验收。
- F02 Spring Boot 骨架：已验收；测试和健康检查已通过。

当前只提供健康检查，还没有 DeepSeek、MySQL 业务表、文件上传、RAG 或 Vue 页面。

## 环境

- Java 21
- Maven 3.6.3
- Spring Boot 3.5.15
- 项目端口：8081

## 启动

```powershell
mvn spring-boot:run
```

## 测试

```powershell
mvn test
```

## 检查地址

- `http://127.0.0.1:8081/api/health`
- `http://127.0.0.1:8081/actuator/health`

## 本地配置

复制 `.env.example` 的字段到本地 `.env`，填写实际值。`.env` 已被 `.gitignore` 排除，禁止提交。
