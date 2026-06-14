# MindPulse - AI-Enhanced Student Personal Productivity Tool

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A lightweight AI-powered productivity backend for university students, providing AI task parsing, async note summarization, and smart reminders. Standard REST API + WebSocket real-time push, supporting frontend and AI agent integration.

## Core Features

| Module | Description | Highlights |
|--------|-------------|------------|
| **AI Task Parsing** | Natural language → structured task, auto-persist | Semantic cache deduplication, 70%+ cache hit rate |
| **Async Note Summary** | Instant response on upload, RabbitMQ async summary + tags | QPS boosted from 50 to 110, no long request blocking |
| **Smart Reminders** | Dynamic reminder engine + Redis distributed lock | <3% conflict rate, 99%+ push accuracy |

## Architecture

```
┌─────────────────────────────────────────────────┐
│           Frontend / AI Agent / Apifox           │
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

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 3.1.5 |
| ORM | MyBatis | 3.0.2 |
| Database | MySQL | 8.0 |
| Cache | Redis | 7.0 |
| Message Queue | RabbitMQ | 3.x |
| Realtime | WebSocket (STOMP) | — |
| Auth | JWT (jjwt) | 0.11.5 |
| AI | LangChain + Ollama | qwen2.5:1.5b |
| API Docs | SpringDoc OpenAPI | 2.2.0 |
| Container | Docker | — |
| Build | Maven | — |

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- MySQL 8.0
- Redis 7.0
- RabbitMQ 3.x
- Ollama (optional, for AI features)

### 1. Start Middleware

```bash
# MySQL (create database first)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS \`mindpulse-database\` DEFAULT CHARSET utf8mb4;"

# Redis
docker run -d -p 6379:6379 --name redis redis:7

# RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:3-management

# Ollama (AI service)
ollama run qwen2.5:1.5b
```

### 2. Initialize Database

```bash
mysql -u root -p mindpulse-database < sql/mindpulse.sql
```

### 3. Configure Environment Variables (Optional)

```bash
export JWT_SECRET="your-secret-key-at-least-32-chars"
export AI_SERVICE_URL="http://localhost:8000/api/v1/analyze"
export RABBITMQ_HOST="localhost"
export REDIS_HOST="localhost"
```

### 4. Start Service

```bash
# Development mode
mvn spring-boot:run

# Package and run
mvn clean package -DskipTests
java -jar target/mindpulse-system-0.0.1-SNAPSHOT.jar
```

After startup:

- API Docs: http://localhost:8090/doc.html
- OpenAPI JSON: http://localhost:8090/v3/api-docs
- RabbitMQ Console: http://localhost:15672 (guest/guest)

## Project Structure

```
mindpulse-system/
├── src/main/java/com/mindpulse/backend/
│   ├── MindPulseBackendApplication.java  # Entry point
│   ├── config/                           # Configuration
│   │   ├── SecurityConfig.java           # Spring Security + JWT
│   │   ├── RabbitMQConfig.java           # RabbitMQ queues/exchanges
│   │   ├── RedisConfig.java              # Redis serialization
│   │   ├── WebSocketConfig.java          # STOMP endpoints
│   │   ├── ScheduledTasksConfig.java     # Dynamic reminder scheduling
│   │   └── CacheConfig.java              # Spring Cache
│   ├── controller/                       # REST API layer
│   │   ├── AuthController.java           # Register/Login
│   │   ├── TaskController.java           # Task CRUD + AI parsing
│   │   ├── NoteController.java           # Note CRUD + async summary
│   │   ├── ReminderController.java       # Reminder CRUD
│   │   ├── PomodoroController.java       # Pomodoro timer
│   │   ├── DashboardController.java      # Data dashboard
│   │   └── AdminController.java          # Admin management
│   ├── service/                          # Business logic
│   │   ├── ai/                           # AI services
│   │   │   ├── SemanticCacheService.java # Semantic cache
│   │   │   └── TaskValidationService.java# Result validation
│   │   ├── AiAgentClient.java            # Python AI client
│   │   ├── NoteSummaryProducer.java      # RabbitMQ producer
│   │   ├── NoteSummaryConsumer.java      # RabbitMQ consumer
│   │   ├── TaskService.java              # Task service (distributed lock)
│   │   ├── NoteService.java              # Note service
│   │   ├── ReminderService.java          # Reminder service
│   │   ├── PomodoroService.java          # Pomodoro service
│   │   ├── DashboardService.java         # Dashboard service
│   │   ├── AuditLogService.java          # Audit log service
│   │   └── UserService.java              # User service
│   ├── mapper/                           # MyBatis interfaces
│   ├── entity/                           # Domain entities
│   ├── dto/                              # Data transfer objects
│   ├── security/                         # JWT filter/util
│   ├── websocket/                        # WebSocket handlers
│   ├── util/                             # Utilities (DistributedLock)
│   ├── annotation/                       # Custom annotations
│   ├── aspect/                           # AOP aspects
│   ├── interceptor/                      # HTTP interceptors
│   └── exception/                        # Global exception handling
├── src/main/resources/
│   ├── application.yml                   # Main config
│   ├── application-dev.yml               # Dev profile
│   ├── application-prod.yml              # Prod profile
│   └── mapper/                           # MyBatis XML
├── sql/                                  # Database schema
│   └── mindpulse.sql
├── interface.md                          # Full API documentation
├── CLAUDE.md                             # Claude Code config
├── Dockerfile
└── pom.xml
```

## API Endpoints

Full API documentation: [interface.md](interface.md), Swagger UI at `/doc.html`.

### Authentication

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | User registration |
| POST | `/api/auth/login` | User login, returns JWT |

### Tasks (Auth Required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/tasks` | List tasks |
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks/{id}` | Get task detail |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |
| POST | `/api/tasks/parse` | **AI task parsing** (NL → structured) |
| PUT | `/api/tasks/{id}/status` | Status update (distributed lock) |
| GET | `/api/tasks/cache-stats` | Semantic cache stats |

### Notes (Auth Required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notes` | List notes |
| POST | `/api/notes` | Sync upload note |
| POST | `/api/notes/async` | **Async upload** (recommended, RabbitMQ + AI summary) |
| GET | `/api/notes/{id}` | Get note detail |
| PUT | `/api/notes/{id}` | Update note |
| DELETE | `/api/notes/{id}` | Delete note |

### Reminders (Auth Required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/reminders` | List reminders |
| POST | `/api/reminders` | Create reminder (DAILY/WEEKLY/CUSTOM) |
| GET | `/api/reminders/{id}` | Get reminder detail |
| PUT | `/api/reminders/{id}` | Update reminder |
| DELETE | `/api/reminders/{id}` | Delete reminder |

### Pomodoro (Auth Required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/pomodoro/start` | Start pomodoro session |
| PUT | `/api/pomodoro/{id}/complete` | Complete session |
| PUT | `/api/pomodoro/{id}/cancel` | Cancel session |
| GET | `/api/pomodoro/active` | Get active session |
| GET | `/api/pomodoro/stats` | Get study statistics |
| GET | `/api/pomodoro/history` | Get session history |

### Dashboard (Auth Required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard/summary` | Dashboard summary |
| GET | `/api/dashboard/productivity` | Productivity trend |
| GET | `/api/dashboard/category-distribution` | Category distribution |
| GET | `/api/dashboard/study-heatmap` | Study heatmap |

### WebSocket

| Endpoint | Description |
|----------|-------------|
| `/ws` | STOMP connection (SockJS) |
| `/user/queue/reminders` | Subscribe: reminder push |
| `/user/queue/note-summary` | Subscribe: note summary complete |

## Import to Apifox

1. Start the service and visit `http://localhost:8090/v3/api-docs`
2. Copy the JSON content
3. Apifox → Import → URL Import → paste `http://localhost:8090/v3/api-docs`

## Run Tests

```bash
mvn test
```

## Common Commands

```bash
# Start dev server
mvn spring-boot:run

# Compile
mvn compile

# Package (skip tests)
mvn clean package -DskipTests
```

## License

MIT
