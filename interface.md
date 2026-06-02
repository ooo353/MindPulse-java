# MindPulse API 接口文档

> 基础地址：`http://localhost:8090`  
> 在线文档：`http://localhost:8090/doc.html`  
> OpenAPI JSON：`http://localhost:8090/v3/api-docs`

---

## 认证方式

所有接口（除认证接口和文档页面）需携带 JWT Token：

```
Authorization: Bearer <token>
```

---

## 一、认证模块

### 1.1 用户注册

**POST** `/api/auth/register`

- **请求体** (`application/json`)
  ```json
  {
    "username": "student01",
    "password": "123456",
    "email": "student01@example.com"
  }
  ```

- **响应**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "User registered successfully",
    "data": { "username": "student01", "email": "student01@example.com" }
  }
  ```

---

### 1.2 用户登录

**POST** `/api/auth/login`

- **请求体** (`application/json`)
  ```json
  {
    "username": "student01",
    "password": "123456"
  }
  ```

- **响应**
  ```json
  {
    "success": true,
    "code": 200,
    "message": "Login successful",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "username": "student01",
      "email": "student01@example.com"
    }
  }
  ```

  | 字段 | 说明 |
  |------|------|
  | token | JWT令牌，后续请求需携带 |

---

## 二、AI 任务解析模块

### 2.1 创建任务

**POST** `/api/tasks`

- **请求体** (`application/json`)
  ```json
  {
    "title": "复习高数第三章",
    "description": "重点复习微积分与极限",
    "dueDate": "2026-05-25T14:00:00",
    "priority": "high",
    "status": "pending",
    "category": "考试复习",
    "relatedNotes": "1,3"
  }
  ```

- **响应** `201 Created`

---

### 2.2 查询任务列表

**GET** `/api/tasks?status=pending`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 状态过滤: pending/completed/archived |

---

### 2.3 查询任务详情

**GET** `/api/tasks/{id}`

---

### 2.4 更新任务

**PUT** `/api/tasks/{id}`

- **请求体** 同创建接口

---

### 2.5 删除任务

**DELETE** `/api/tasks/{id}`

---

### 2.6 AI 任务解析（核心接口）

**POST** `/api/tasks/parse`

- **请求体** (`application/json`)
  ```json
  {
    "taskDescription": "明天下午3点前完成高数作业，优先级高，属于考试复习"
  }
  ```

- **响应**
  ```json
  {
    "success": true,
    "code": 200,
    "data": {
      "parsedTask": {
        "title": "完成高数作业",
        "description": "明天下午3点前完成高数作业",
        "due_date": "2026-05-24T15:00:00",
        "priority": "high",
        "category": "考试复习"
      },
      "createdTask": { "id": 1, "title": "完成高数作业", ... },
      "fromCache": false,
      "responseTimeMs": 850
    }
  }
  ```

  | 字段 | 说明 |
  |------|------|
  | parsedTask | AI解析的结构化数据 |
  | fromCache | true=命中语义缓存，false=调用AI |
  | responseTimeMs | 响应耗时(ms) |

---

### 2.7 缓存统计

**GET** `/api/tasks/cache-stats`

- **响应**
  ```json
  {
    "data": {
      "totalRequests": 120,
      "cacheHits": 85,
      "cacheMisses": 35,
      "hitRate": 0.71,
      "avgResponseTimeMs": 320,
      "cacheHitAvgMs": 45,
      "cacheMissAvgMs": 980
    }
  }
  ```

---

### 2.8 更新任务状态（分布式锁保护）

**PUT** `/api/tasks/{id}/status?status=completed`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 是 | pending/completed/archived |

- **响应码**
  | 状态码 | 说明 |
  |--------|------|
  | 200 | 更新成功 |
  | 403 | 无权操作 |
  | 404 | 任务不存在 |
  | 409 | 并发冲突（任务被其他操作锁定），需重试 |

---

## 三、笔记摘要异步处理模块

### 3.1 同步上传笔记

**POST** `/api/notes`

- **请求体** (`multipart/form-data`)
  | 参数 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | title | string | 是 | 笔记标题 |
  | content | string | 是 | 笔记内容 |
  | tags | string | 否 | 标签（逗号分隔） |
  | file | file | 否 | 附件 |

- **响应** `201 Created`

---

### 3.2 异步上传笔记（推荐）

**POST** `/api/notes/async`

- **请求体** (`multipart/form-data`) 同同步上传

- **响应** `201 Created`
  ```json
  {
    "success": true,
    "code": 201,
    "message": "笔记已提交，摘要异步处理中",
    "data": {
      "noteId": 42,
      "status": "processing",
      "message": "笔记已提交，摘要处理中",
      "note": { "id": 42, "title": "...", "status": "processing", ... }
    }
  }
  ```

  > 处理完成后通过 WebSocket 实时推送结果 → 见 [WebSocket 章节](#五websocket-实时推送)

---

### 3.3 查询笔记列表

**GET** `/api/notes?keyword=高数`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键字（匹配标题和内容） |

---

### 3.4 查询笔记详情

**GET** `/api/notes/{id}`

- **响应中包含 AI 生成字段**
  ```json
  {
    "data": {
      "id": 42,
      "title": "高数第三章 - 微积分基础",
      "content": "...",
      "tags": "高数,微积分,极限",
      "summary": "本笔记涵盖微积分基础概念，包括极限定义...",
      "category": "考试复习",
      "status": "completed",
      ...
    }
  }
  ```

  | 新字段 | 说明 |
  |--------|------|
  | summary | AI 生成的摘要 |
  | category | AI 推荐的分类 |
  | status | processing/completed/failed |

---

### 3.5 更新笔记

**PUT** `/api/notes/{id}`

---

### 3.6 删除笔记

**DELETE** `/api/notes/{id}`

---

### 3.7 同步生成摘要（调试用，已废弃）

**POST** `/api/notes/{id}/summary`

> 建议使用 `POST /api/notes/async` 异步方式

---

## 四、智能提醒模块

### 4.1 创建提醒

**POST** `/api/reminders`

- **请求体** (`application/json`)
  ```json
  {
    "message": "记得复习高数第三章",
    "remindType": "DAILY",
    "remindTime": "09:00",
    "targetId": 1,
    "targetType": "TASK",
    "enabled": true
  }
  ```

  | 字段 | 类型 | 必填 | 说明 |
  |------|------|------|------|
  | message | string | 是 | 提醒内容 |
  | remindType | string | 是 | ONCE/DAILY/WEEKLY/CUSTOM |
  | remindTime | string | 是 | 提醒时间 (HH:mm) |
  | remindDate | string | 否 | 一次性提醒日期 (yyyy-MM-dd) |
  | dayOfWeek | string | 否 | 每周几 (MON/TUE/...) |
  | cronExpression | string | 否 | 自定义 cron 表达式 |
  | targetId | long | 否 | 关联的目标ID |
  | targetType | string | 否 | TASK/NOTE |
  | enabled | boolean | 否 | 是否启用（默认 true） |

- **响应** `201 Created`

---

### 4.2 查询提醒列表

**GET** `/api/reminders`

---

### 4.3 查询提醒详情

**GET** `/api/reminders/{id}`

---

### 4.4 更新提醒

**PUT** `/api/reminders/{id}`

---

### 4.5 删除提醒

**DELETE** `/api/reminders/{id}`

---

## 五、WebSocket 实时推送

### 连接信息

| 项目 | 说明 |
|------|------|
| 端点 | `ws://localhost:8090/ws` |
| 协议 | STOMP over SockJS |
| 心跳 | 自动（STOMP 内置） |

### 客户端订阅

| 订阅目标 | 说明 |
|----------|------|
| `/user/queue/reminders` | 提醒通知（任务到期 / 用户自定义提醒） |
| `/user/queue/note-summary` | 笔记摘要处理完成通知 |
| `/topic/reminders` | 广播提醒（全局） |

### 客户端发送

| 目标 | 说明 |
|------|------|
| `/app/note-subscribe` | 订阅笔记摘要推送通道 |

### 推送消息格式

**任务到期提醒** → `/user/queue/reminders`
```json
{
  "type": "TASK_DUE",
  "taskId": 1,
  "title": "完成高数作业",
  "dueDate": "2026-05-24 15:00",
  "message": "任务「完成高数作业」即将到期！",
  "author": "student01"
}
```

**用户自定义提醒** → `/user/queue/reminders`
```json
{
  "type": "REMINDER",
  "reminderId": 5,
  "message": "记得复习高数第三章",
  "remindTime": "09:00",
  "targetId": 1,
  "targetType": "TASK"
}
```

**笔记摘要完成** → `/user/queue/note-summary`
```json
{
  "noteId": 42,
  "title": "高数第三章 - 微积分基础",
  "summary": "本笔记涵盖微积分基础概念...",
  "tags": "高数,微积分,极限",
  "category": "考试复习",
  "status": "completed",
  "author": "student01",
  "processingTimeMs": 1250
}
```

### JS 客户端示例 (SockJS + STOMP)

```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2/lib/stomp.min.js"></script>
<script>
  var socket = new SockJS('http://localhost:8090/ws');
  var client = Stomp.over(socket);
  client.connect({}, function() {
    // 订阅提醒
    client.subscribe('/user/queue/reminders', function(msg) {
      console.log('收到提醒:', JSON.parse(msg.body));
    });
    // 订阅笔记摘要结果
    client.subscribe('/user/queue/note-summary', function(msg) {
      console.log('摘要完成:', JSON.parse(msg.body));
    });
    // 建立笔记摘要订阅通道
    client.send('/app/note-subscribe', {}, JSON.stringify({username: 'student01'}));
  });
</script>
```

---

## 六、通用响应格式

### 成功响应
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": <具体数据>,
  "timestamp": "2026-05-23T15:30:00"
}
```

### 错误响应
```json
{
  "success": false,
  "code": 500,
  "message": "错误描述",
  "data": null,
  "timestamp": "2026-05-23T15:30:00"
}
```

### 标准 HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 并发冲突（分布式锁保护） |
| 500 | 服务器错误 |

---

## 七、技术架构概览

```
┌────────────────────────────────────────────────┐
│                   前端 / 智能体                   │
├────────────────────────────────────────────────┤
│  REST API  │  WebSocket  │  OpenAPI (Swagger)  │
├────────────┴─────────────┴─────────────────────┤
│              Spring Boot 3.1.5                   │
├──────────┬──────────┬──────────┬────────────────┤
│  MyBatis │  Redis   │ RabbitMQ │ Spring Security│
├──────────┴──────────┴──────────┴────────────────┤
│  MySQL 8.0  │  Redis 7.0  │  RabbitMQ 3.x       │
├─────────────────────────────────────────────────┤
│  Python AI Agent (LangChain + Ollama qwen2.5)   │
└─────────────────────────────────────────────────┘
```

| 模块 | 关键能力 |
|------|----------|
| AI 任务解析 | 自然语言→结构化任务，语义缓存去重 |
| 笔记摘要异步处理 | RabbitMQ 解耦，AI 摘要+分类，WebSocket 实时推送 |
| 智能提醒 | 动态提醒引擎，Redis 分布式锁防并发冲突 |
