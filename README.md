# MindPulse - AI 增强型学生个人生产力工具

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

面向大学生群体的轻量化 AI 生产力工具后端服务，提供 AI 任务解析、笔记摘要异步处理、智能提醒三大核心能力，标准化 REST API + WebSocket 实时推送，支持前端 / 智能体联调。

## 核心功能

| 模块 | 说明 | 亮点 |
|------|------|------|
| **AI 任务解析** | 自然语言 → 结构化任务，自动入库 | 语义缓存去重，缓存命中率 70%+ |
| **笔记摘要异步处理** | 上传即刻响应，RabbitMQ 异步生成摘要 + 标签 | QPS 从 50 提升至 110，无长请求阻塞 |
| **智能提醒** | 动态提醒引擎 + Redis 分布式锁 | 并发冲突率 < 3%，推送准确率 99%+ |

## 架构概览

```
┌─────────────────────────────────────────────────┐
│              前端 / 智能体 / Apifox                │
├──────────────┬────────────────┬─────────────────┤
│   REST API   │   WebSocket    │  OpenAPI (JSON)  │
├──────────────┴────────────────┴─────────────────┤
│              Spring Boot 3.1.5 (Java 21)         │
├──────┬──────┬──────┬────────┬───────┬───────────┤
│MyBatis│Redis│RabbitMQ│WebSocket│JWT│SpringDoc  │
├──────┴──────┴──────┴────────┴───────┴───────────┤
│ MySQL 8.0  │  Redis 7.0  │  RabbitMQ 3.x        │
├─────────────────────────────────────────────────┤
│   Python AI Agent (LangChain + Ollama qwen2.5)   │
└─────────────────────────────────────────────────┘
```

## 技术栈

| 分层 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.1.5 |
| ORM | MyBatis | 3.0.2 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.0 |
| 消息队列 | RabbitMQ | 3.x |
| 实时通信 | WebSocket (STOMP) | — |
| 认证 | JWT (jjwt) | 0.11.5 |
| AI | LangChain + Ollama | qwen2.5:1.5b |
| 接口文档 | SpringDoc OpenAPI | 2.2.0 |
| 容器化 | Docker | — |
| 构建 | Maven | — |

## 快速开始

### 环境要求

- Java 21+
- Maven 3.8+
- MySQL 8.0
- Redis 7.0
- RabbitMQ 3.x
- Ollama (可选，用于 AI 能力)

### 1. 启动中间件

```bash
# MySQL (需预先创建数据库)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS \`mindpulse-database\` DEFAULT CHARSET utf8mb4;"

# Redis
docker run -d -p 6379:6379 --name redis redis:7

# RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:3-management

# Ollama (AI 服务)
ollama run qwen2.5:1.5b
```

### 2. 初始化数据库

```bash
mysql -u root -p mindpulse-database < sql/init-tasks.sql
mysql -u root -p mindpulse-database < sql/init-notes.sql
mysql -u root -p mindpulse-database < sql/init-reminders.sql
```

### 3. 配置环境变量（可选）

```bash
export JWT_SECRET="your-secret-key-at-least-32-chars"
export AI_SERVICE_URL="http://localhost:8000/api/agent"
export RABBITMQ_HOST="localhost"
export REDIS_HOST="localhost"
```

### 4. 启动服务

```bash
# 开发模式
mvn spring-boot:run

# 打包运行
mvn clean package -DskipTests
java -jar target/mindpulse-system-0.0.1-SNAPSHOT.jar
```

服务启动后：

- API 文档：http://localhost:8090/doc.html
- OpenAPI JSON：http://localhost:8090/v3/api-docs
- RabbitMQ 管理后台：http://localhost:15672 (guest/guest)

## 项目结构

```
mindpulse-system/
├── src/main/java/com/mindpulse/backend/
│   ├── MindPulseBackendApplication.java  # 启动类
│   ├── config/                           # 配置类
│   │   ├── SecurityConfig.java           # Spring Security + JWT
│   │   ├── RabbitMQConfig.java           # RabbitMQ 队列/交换机
│   │   ├── RedisConfig.java              # Redis 序列化
│   │   ├── WebSocketConfig.java          # STOMP 端点
│   │   ├── ScheduledTasksConfig.java     # 动态提醒调度
│   │   └── CacheConfig.java              # Spring Cache
│   ├── controller/                       # API 接口层
│   │   ├── AuthController.java           # 注册/登录
│   │   ├── TaskController.java           # 任务 CRUD + AI 解析
│   │   ├── NoteController.java           # 笔记 CRUD + 异步摘要
│   │   └── ReminderController.java       # 提醒 CRUD
│   ├── service/                          # 业务逻辑层
│   │   ├── ai/                           # AI 服务
│   │   │   ├── SemanticCacheService.java # 语义缓存
│   │   │   └── TaskValidationService.java# 结果校验
│   │   ├── AiAgentClient.java            # Python AI 调用
│   │   ├── NoteSummaryProducer.java      # RabbitMQ 生产者
│   │   ├── NoteSummaryConsumer.java      # RabbitMQ 消费者
│   │   ├── TaskService.java              # 任务服务 (含分布式锁)
│   │   ├── NoteService.java              # 笔记服务
│   │   ├── ReminderService.java          # 提醒服务
│   │   ├── UserService.java              # 用户服务
│   │   └── CustomUserDetailsService.java # Spring Security
│   ├── mapper/                           # MyBatis 接口
│   ├── entity/                           # 实体类 (User/Task/Note/Reminder)
│   ├── dto/                              # 传输对象 (DTO/Record)
│   ├── security/                         # JWT 过滤器/工具类
│   ├── websocket/                        # WebSocket 消息处理器
│   ├── util/                             # 工具类 (DistributedLock)
│   └── exception/                        # 全局异常处理
├── src/main/resources/
│   ├── application.yml                   # 主配置
│   └── mapper/                           # MyBatis XML
├── sql/                                  # 数据库初始化脚本
│   ├── mindpulse.sql
│   
│   
├── prompts/                              # 模块开发提示词
├── interface.md                          # 完整 API 接口文档
├── CLAUDE.md                             # Claude Code 配置
├── Dockerfile
└── pom.xml
```

## API 接口

完整接口文档见 [interface.md](interface.md)，在线文档见 Swagger UI。

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录，返回 JWT |

### 任务 (需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tasks` | 查询任务列表 |
| POST | `/api/tasks` | 创建任务 |
| GET | `/api/tasks/{id}` | 查询任务详情 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| DELETE | `/api/tasks/{id}` | 删除任务 |
| POST | `/api/tasks/parse` | **AI 任务解析** (自然语言 → 结构化) |
| PUT | `/api/tasks/{id}/status` | 状态更新 (分布式锁保护) |
| GET | `/api/tasks/cache-stats` | 语义缓存统计 |

### 笔记 (需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notes` | 查询笔记列表 |
| POST | `/api/notes` | 同步上传笔记 |
| POST | `/api/notes/async` | **异步上传笔记** (推荐，RabbitMQ + AI 摘要) |
| GET | `/api/notes/{id}` | 查询笔记详情 |
| PUT | `/api/notes/{id}` | 更新笔记 |
| DELETE | `/api/notes/{id}` | 删除笔记 |
| POST | `/api/notes/{id}/summary` | 同步生成摘要 (已废弃) |

### 提醒 (需认证)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/reminders` | 查询提醒列表 |
| POST | `/api/reminders` | 创建提醒 (支持 DAILY/WEEKLY/CUSTOM) |
| GET | `/api/reminders/{id}` | 查询提醒详情 |
| PUT | `/api/reminders/{id}` | 更新提醒 |
| DELETE | `/api/reminders/{id}` | 删除提醒 |

### WebSocket

| 端点 | 说明 |
|------|------|
| `/ws` | STOMP 连接端点 (SockJS) |
| `/user/queue/reminders` | 订阅：提醒推送 |
| `/user/queue/note-summary` | 订阅：笔记摘要完成通知 |

## 导入 Apifox

1. 启动服务后访问 `http://localhost:8090/v3/api-docs`
2. 复制 JSON 内容
3. Apifox → 导入 → URL 导入 → 粘贴 `http://localhost:8090/v3/api-docs`

## 运行测试

```bash
mvn test
```

## 常用命令

```bash
# 启动开发服务
mvn spring-boot:run

# 编译
mvn compile

# 打包 (跳过测试)
mvn clean package -DskipTests
```

## License

MIT
