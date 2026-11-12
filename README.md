# Living Frame Demo

一个基于 **Spring Boot 3 + Vue 3** 的前后端分离后台管理系统示例，包含完整的 RBAC 权限、系统管理、MinIO 文件管理、AI 对话与长期记忆能力。

> 当前仓库是 **Maven 版本**。  
> `master` / `maven-version` 分支使用 Maven 多模块构建；后续会提供对应的 **Gradle 版本**。

## 功能概览

### 系统管理

- 用户管理：分页、搜索、创建、编辑、删除、重置密码、分配角色
- 角色管理：角色维护、菜单/按钮权限分配
- 权限管理：菜单树、按钮权限编码、路由配置
- 登录日志、操作日志
- 系统黑名单：IP / 用户名永久黑名单，登录前拦截
- 验证码登录：60 秒有效期、倒计时、自动刷新

### 权限模型

- `SUPER_ADMIN` 代码级放行全部权限
- 只有超级管理员可以查看和维护角色、权限管理菜单
- 系统管理员可管理用户、日志、黑名单、文件，但不能维护角色和权限
- 按钮级权限通过 `v-permission` 指令控制

### 文件管理

- 集成 MinIO 对象存储
- Bucket 创建、删除、切换
- 目录浏览、面包屑导航
- 文件上传、下载、删除
- 图片、视频、音频、PDF、文本/代码等文件在线预览
- 用户头像上传到 MinIO，数据库保存半路径，前端通过 `VITE_AVATAR_BASE_URL` 拼接访问地址

### AI 能力

- 用户独立维护 AI 厂商、Base URL、模型和 API Key
- API Key 使用 AES-GCM 加密存储，接口只返回是否已配置，不返回密钥
- 支持 DeepSeek、通义千问、OpenAI 和自定义 OpenAI 兼容厂商
- AI 对话、AI Agent 工具调用
- MySQL + Redis + Qdrant 对话记忆
  - MySQL 持久化会话与消息
  - Redis 缓存最近对话
  - Qdrant 跨会话语义记忆召回

### 前端体验

- Vue 3 + TypeScript + Element Plus
- 响应式后台布局、可收起侧边栏
- 统一卡片、表格、工具栏和空状态样式
- 仪表盘、系统管理、业务管理、AI 能力等模块

## 技术栈

| 分类 | 技术 |
|---|---|
| 后端 | JDK 25、Spring Boot 3.5、Spring Security 6、sa-token |
| 持久化 | MyBatis-Plus、MySQL |
| 缓存 | Redis、Redisson |
| 文件存储 | MinIO |
| AI | Spring AI、OpenAI 兼容协议 |
| 向量记忆 | Qdrant |
| 前端 | Vue 3.5、TypeScript、Element Plus、Pinia、Vite 7 |
| 构建 | Maven 3.9+、npm |

## 模块结构

```text
living-frame-demo
├── living-frame-common      通用结果、异常、上下文、工具
├── living-frame-system      用户、角色、权限、日志、黑名单、文件
├── living-frame-framework   安全、Redis、MyBatis-Plus、Web 配置
├── living-frame-business    业务用户模块
├── living-frame-ai          AI 对话、Agent、配置、会话记忆
├── living-frame-admin       启动模块与配置文件
└── living-frame-vue3        Vue 3 前端
```

## 快速开始（Maven 版本）

### 环境要求

- JDK 25
- Maven 3.9+
- Node.js 20+
- MySQL 8+
- Redis 6+
- 可选：MinIO、Qdrant

### 1. 配置环境变量

仓库只保留 `application.yml` 和 `application-prod.yml`。本地开发用的 `application-dev.yml` 不提交，请根据下方变量在本地创建，配置结构可参考 `application-prod.yml`。

开发环境默认连接本机服务，可通过环境变量覆盖：

```bash
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=living_frame
MYSQL_USERNAME=root
MYSQL_PASSWORD=root

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_DEFAULT_BUCKET=living-frame

AI_CONFIG_SECRET=change-this-ai-config-secret
QDRANT_URL=http://localhost:6333
```

生产环境请参考 `application-prod.yml` 使用环境变量，不要使用演示默认值。

### 2. 启动后端

```bash
mvn clean package
java -jar living-frame-admin/target/living-frame-admin.jar --spring.profiles.active=dev
```

默认端口：`8080`  
健康检查：`http://localhost:8080/actuator/health`

### 3. 启动前端

```bash
cd living-frame-vue3
npm install
npm run dev
```

前端默认端口：`5173`。

### 4. 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `Admin@123` | 超级管理员 |
| `sysadmin` | `Sys@123` | 系统管理员 |
| `viewer` | `View@123` | 只读用户 |
| `biz01` | `Biz@123` | 业务用户 |

## 分支说明

- `master`：项目介绍、Maven 版本代码
- `maven-version`：Maven 多模块版本代码
- 后续计划：`gradle-version`

如果你从 `master` 或 `maven-version` 克隆项目，当前获取的是 **Maven 构建版本**。

## 接口概览

```text
POST   /api/auth/login
GET    /api/auth/me

GET    /api/system/users
GET    /api/system/roles
GET    /api/system/permissions
GET    /api/system/login-logs
GET    /api/system/operation-logs
GET    /api/system/blacklist
GET    /api/system/files/buckets
POST   /api/system/files/upload

GET    /api/ai/status
GET    /api/ai/configs
GET    /api/ai/conversations
POST   /api/ai/chat
POST   /api/ai/agent
```

## 安全说明

- 公开仓库中的开发配置仅使用本机占位默认值，不包含真实服务器凭据。
- 生产环境必须通过环境变量或配置中心提供数据库、Redis、MinIO、AI 密钥。
- AI API Key 在数据库中加密存储；前端不会返回或展示密钥。
- 请勿在公开仓库提交 `.env`、日志、构建产物或本地配置。

## 说明

该项目用于学习与演示，生产使用前请补充审计、限流、监控、备份、密钥管理和正式数据库迁移流程。
