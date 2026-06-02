# MindPulse - AI 增强型学生个人生产力工具

项目定位：面向大学生群体的轻量化 AI 生产力工具后端服务，提供标准化 REST API + WebSocket 实时推送，供前端 / 智能体 / Apifox 联调。

核心模块状态：
- [x] AI 任务解析服务（含语义缓存去重）
- [x] 笔记摘要异步处理链路（RabbitMQ + AI 摘要 + WebSocket 推送）
- [x] 智能提醒与分布式并发控制（Redis 分布式锁 + 动态调度）

---

## 1. 技术栈

| 组件 | 版本/说明 |
|------|-----------|
| Spring Boot | 3.1.5 |
| Java | 21 |
| ORM | MyBatis 3.0.2 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.0 (Lettuce 连接池) |
| 消息队列 | RabbitMQ 3.x (spring-boot-starter-amqp) |
| 实时通信 | WebSocket STOMP (SockJS) |
| 认证 | JWT (jjwt 0.11.5, HS256) |
| AI 集成 | Python Agent (LangChain + Ollama qwen2.5:1.5b) |
| 接口文档 | SpringDoc OpenAPI 2.2.0 |
| 构建 | Maven |
| 测试框架 | spring-boot-starter-test |

---

## 2. 项目结构（实际）

```
mindpulse-system/
├── src/main/java/com/mindpulse/backend/
│   ├── MindPulseBackendApplication.java
│   ├── config/            # SecurityConfig, RabbitMQConfig, RedisConfig, WebSocketConfig, ScheduledTasksConfig, CacheConfig
│   ├── controller/        # AuthController, TaskController, NoteController, ReminderController
│   ├── service/           # 业务逻辑（含 service/ai/ 子包: SemanticCacheService, TaskValidationService）
│   │   ├── AiAgentClient.java        # 调用 Python AI 服务
│   │   ├── NoteSummaryProducer.java  # RabbitMQ 消息生产者
│   │   ├── NoteSummaryConsumer.java  # RabbitMQ 消息消费者（@RabbitListener）
│   │   ├── TaskService.java          # 含分布式锁状态更新
│   │   ├── NoteService.java          # 含异步上传 createNoteAsync
│   │   └── ReminderService.java
│   ├── mapper/            # MyBatis 接口（User/Note/Task/ReminderMapper）
│   ├── entity/            # User, Task, Note, Reminder
│   ├── dto/               # Record/POJO: TaskDto, NoteDto, ReminderDto, NoteSummaryMessage 等
│   ├── security/          # JwtUtil, JwtFilter, CustomUserDetails
│   ├── websocket/         # ReminderWebSocketHandler, NoteWebSocketHandler
│   ├── util/              # DistributedLock（Redis SETNX + Lua 安全解锁）
│   └── exception/         # GlobalExceptionHandler, ResourceNotFoundException
├── src/main/resources/
│   ├── application.yml
│   └── mapper/            # UserMapper.xml, TaskMapper.xml, NoteMapper.xml, ReminderMapper.xml
├── sql/                   # init-tasks.sql, init-notes.sql, init-reminders.sql
├── prompts/               # 模块开发提示词
├── interface.md           # 完整 API 接口文档（Markdown）
├── README.md              # 项目说明
├── CLAUDE.md              # 本文件
├── Dockerfile
└── pom.xml
```

---

## 3. 快速启动中间件

```bash
# Redis
docker run -d -p 6379:6379 --name redis redis:7

# RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:3-management

# Ollama
ollama run qwen2.5:1.5b
```

数据库初始化：
```bash
mysql -u root -p mindpulse-database < sql/init-tasks.sql
mysql -u root -p mindpulse-database < sql/init-notes.sql
mysql -u root -p mindpulse-database < sql/init-reminders.sql
```

启动服务：`mvn spring-boot:run`

关键地址：
- API 文档（Swagger UI）：http://localhost:8090/doc.html
- OpenAPI JSON（导入 Apifox）：http://localhost:8090/v3/api-docs
- RabbitMQ 管理后台：http://localhost:15672 (guest/guest)

---

## 4. 编码规范（强制遵守）

### 命名
- 驼峰命名法：类名大驼峰，方法/变量小驼峰
- 数据库字段下划线 → MyBatis map-underscore-to-camel-case 自动转换

### 注释
- 核心方法加单行功能注释，复杂逻辑加行内注释
- 不要写多行 docstring 或叙述性注释块——代码应自解释

### 异常处理
- 统一由 GlobalExceptionHandler 处理，返回标准化 ApiResponse JSON
- 业务异常抛 ResourceNotFoundException 或 RuntimeException

### 日志
- 使用 SLF4J（`LoggerFactory.getLogger`），不用 `System.out.println`
- 关键节点：入参、出参、异常
- 日志级别：`com.mindpulse` = DEBUG

### 安全
- 禁止硬编码密钥/密码，敏感信息通过环境变量注入
- JWT 密钥：`${JWT_SECRET}`，默认值仅开发环境使用
- 密码加密：BCryptPasswordEncoder

### 测试
- 核心方法写单元测试，覆盖率 ≥ 60%
- 测试类位置：`src/test/java/com/mindpulse/backend/`

### API 文档
- 所有 Controller 必须加 `@Tag`，方法加 `@Operation`
- DTO 字段加 `@Schema(description = "...")`
- 每个接口至少标注 `@ApiResponse(responseCode = "200/201/400/500")`

---

## 5. 核心架构模式

### 异步处理链路（笔记摘要）
```
POST /api/notes/async
→ NoteService.createNoteAsync() 保存 (status=processing) 并立即返回
→ NoteSummaryProducer 投递 RabbitMQ 消息
→ NoteSummaryConsumer @RabbitListener 消费
→ AiAgentClient.generateSummary() 调用 Python AI
→ NoteMapper.updateSummaryAndTags() 写入摘要/标签/分类
→ SimpMessagingTemplate 推送 NoteSummaryResult 到 /user/{}/queue/note-summary
```

### 分布式锁（任务状态更新）
```
PUT /api/tasks/{id}/status
→ DistributedLock.tryLock("task:{id}", 10s) → SETNX + TTL
→ 获取成功 → 读 DB → 校验权限 → 写 DB
→ Lua 脚本安全解锁（仅 value 匹配时才 del）
→ 锁竞争失败 → HTTP 409 响应
```

### 动态提醒调度
```
@Scheduled 每分钟扫描 reminders 表
→ shouldFire() 匹配 remindType (ONCE/DAILY/WEEKLY/CUSTOM) + 时间/日期/星期
→ DistributedLock.tryLock("reminder:{id}:{HH}:{mm}", 50s) 防多实例重复
→ SimpMessagingTemplate.convertAndSendToUser() 推送 /queue/reminders
```

### 语义缓存（AI 解析去重）
```
POST /api/tasks/parse
→ SemanticCacheService.normalize() 归一化（小写 + 去标点 + 同义词替换）
→ SHA-256 哈希
→ Redis 查缓存 → 命中直接返回（45ms）
→ 未命中 → 调用 AI → 校验 → 入库 → 写缓存（980ms）
```

---

## 6. 开发工作流

### 开发新模块
1. 先读 `prompts/` 目录下对应提示词文件，禁止未读就开发
2. 设计 → Entity → DTO → Mapper + XML → Service → Controller
3. 每个 Controller 写完立即加 OpenAPI 注解
4. 同步生成 SQL 脚本放到 `sql/` 目录
5. 编译验证：`mvn compile`
6. 运行测试：`mvn test`
7. 更新 `interface.md` 接口文档

### 修改现有模块
- 先 `git diff` 了解当前分支变更
- 遵循现有代码风格和命名模式
- 不要引入不必要的抽象或 "为未来设计"
- 修改完成后编译 + 测试

### 接口文档
- 项目根目录 `interface.md` 包含所有接口的完整 Markdown 文档
- 前后端联调以 Swagger UI (`/doc.html`) 和 Apifox 导入 (`/v3/api-docs`) 为准

---

## 7. 核心规则

1. **仅开发后端**：不生成任何前端 HTML/CSS/JS、智能体代码
2. **所有接口需认证**：除 `/api/auth/**`、`/ws/**`、文档路径外，其他需 JWT
3. **接口文档必须完整**：在线 Swagger + `interface.md` 双向保障
4. **SQL 脚本同步**：涉及表变更必须产出 SQL 文件
5. **可编译运行**：代码无语法错误，`mvn compile` 通过
6. **一个模块完成再下一个**：不要同时改多个模块
7. **默认中文回复**
8. **不要写无意义的注释**：代码本身说明 WHAT，注释只解释 WHY

---

## 8. 常用命令

```bash
mvn spring-boot:run          # 启动服务 (:8090)
mvn compile                  # 编译
mvn test                     # 运行测试
mvn clean package -DskipTests # 打包
mvn dependency:resolve       # 解析依赖
```
