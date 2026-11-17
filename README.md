# living-frame

基于 JDK 25 + Spring Boot 3.5.16 的多模块单体后台项目，包含后台用户体系、业务用户、Sa-Token 登录、Redis、MySQL、Redisson 与多厂商 AI（DeepSeek / 通义千问 / OpenAI）。

> 本项目不引入 springdoc / Swagger，接口文档后续由第三方工具生成。

## 1. 技术栈

| 组件 | 版本 |
|---|---|
| JDK | 25.0.2 |
| Spring Boot | 3.5.16 |
| Spring Security | 6.5.x（Boot 托管） |
| sa-token | 1.46.0 |
| MyBatis-Plus | 3.5.18 |
| Spring AI | 1.1.8 |
| MySQL | 9.7.0 |
| Redis | 8.8.0 |
| Redisson | 3.52.0 |
| MinIO | 8.5.17 |
| Hutool | 5.8.48 |
| easy-captcha | 1.6.2 |
| Lombok | 1.18.48 |
| Maven | 3.9.16 |

## 2. 模块结构

```
living-frame-demo
├── living-frame-common      统一响应、异常、登录用户模型、通用工具
├── living-frame-system      后台用户/角色/权限、认证、验证码、日志、系统黑名单
├── living-frame-framework   Spring Security、sa-token、Redis、Redisson、MyBatis-Plus、异常处理、操作日志切面
├── living-frame-business    业务用户、业务登录、业务用户管理
├── living-frame-ai          多厂商 AI 配置、对话、Agent 工具调用
└── living-frame-admin       启动类、配置、SQL 初始化、可执行 jar
```

依赖方向：

```
living-frame-common <- living-frame-system <- living-frame-framework <- living-frame-business <- living-frame-ai <- living-frame-admin
```

### 2.1 前端模块 living-frame-vue3

`living-frame-vue3/` 是独立的前端工程（Vue 3 + TypeScript + Element Plus + Vite），不参与 Maven reactor，使用 npm 构建。包含登录、动态菜单、工作台、用户/角色/权限/登录日志/操作日志/系统黑名单、业务用户管理、AI 对话与 Agent 页面。

```powershell
cd living-frame-vue3
npm install
npm run dev
```

默认访问 `http://127.0.0.1:5173`，Vite 将 `/api` 代理到 `http://127.0.0.1:8080`。生产构建：

```powershell
npm run build
```

详细说明见 [living-frame-vue3/README.md](living-frame-vue3/README.md)。

### 2.2 门户移动端 living-frame-portal

`living-frame-portal/` 是独立的门户用户移动端工程（Vue 3 + Vite），不参与 Maven reactor，采用手机 App / 微信小程序风格。

- 底部导航只有「首页」「我的」
- 每个页面右下角固定可拖拽圆形客服入口
- 点击客服入口打开聊天弹层，发送消息自动创建工单
- 右上角查看历史工单
- 未结束工单优先复用，工单结束前不会创建新工单
- 工单结束后再次咨询才开启新工单
- 门户用户只展示一个进行中工单，历史中展示多个已完结工单
- 客服端可以同时查看并处理多个门户用户工单

```powershell
cd living-frame-portal
npm install
npm run dev
```

默认访问 `http://127.0.0.1:5175`。详细说明见 [living-frame-portal/README.md](living-frame-portal/README.md)。

## 3. 快速启动

### 3.1 准备

- JDK 25，`JAVA_HOME` 指向 `C:\Program Files\Java\jdk-25.0.2`
- 本机 Maven 3.9.16：`D:\Work Tools\apache-maven-3.9.16`
- MySQL、Redis 网络可达

### 3.2 建库（可选）

JDBC URL 带了 `createDatabaseIfNotExist=true`，首次启动会自动建库。也可以手工执行：

```sql
-- living-frame-admin/src/main/resources/db/00-create-database.sql
CREATE DATABASE IF NOT EXISTS `living_frame`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

表结构与演示数据由 `living-frame-admin`、`data.sql` 在启动时自动执行，脚本幂等。

### 3.3 构建

```powershell
& 'D:\Work Tools\apache-maven-3.9.16\bin\mvn.cmd' clean package
```

> 当前 DSH 会话的沙箱不能写 `~/.m2`，在会话内构建时可用：
> ```powershell
> $env:MAVEN_OPTS = "-Dmaven.repo.local=$env:TEMP\living-frame-m2"
> ```
> 用户本机 / IDEA 不需要这一步。

### 3.4 启动

```powershell
java -jar living-frame-admin\target\living-frame-admin.jar --spring.profiles.active=dev
```

默认端口 `8080`。健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

### 3.5 演示账号

| 账号 | 密码 | 角色 | 说明 |
|---|---|---|---|
| `admin` | `Admin@123` | SUPER_ADMIN | 超级管理员，代码内放行全部权限 |
| `sysadmin` | `Sys@123` | SYSTEM_ADMIN | 用户/日志/黑名单管理；无角色和权限管理菜单 |
| `smoke_test` | `Smoke@123` | SMOKE_TEST | 冒烟测试保留账号，无业务权限 |
| `viewer` | `View@123` | VIEWER | 只读 |
| `blacklist_test` | `Blacklist@123` | VIEWER | 系统黑名单测试账号，已预置为永久黑名单 |
| `biz01` | `Biz@123` | BIZ_USER | 门户/业务用户 |
| `cs_chat01` | `Cs@123` | CS_CHAT | 在线客服坐席 |
| `cs_chat02` | `Cs@123` | CS_CHAT | 在线客服坐席 |
| `cs_tech01` | `Cs@123` | CS_TECH | 技术客服坐席 |
| `cs_tech02` | `Cs@123` | CS_TECH | 技术客服坐席 |

密码可用 `INIT_ADMIN_PASSWORD`、`INIT_SYSADMIN_PASSWORD`、`INIT_VIEWER_PASSWORD`、`INIT_BUSINESS_PASSWORD`、`INIT_CS_PASSWORD` 覆盖；账号初始化只补缺，不覆盖已有账号。

### 3.6 生产环境启动

`prod` 不提供本地演示默认值，以下环境变量必须显式配置：

```text
MYSQL_URL / MYSQL_USERNAME / MYSQL_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD / REDIS_DATABASE
MINIO_ENDPOINT / MINIO_ACCESS_KEY / MINIO_SECRET_KEY / MINIO_DEFAULT_BUCKET
AI_CONFIG_SECRET
QDRANT_URL / QDRANT_COLLECTION
INIT_ADMIN_PASSWORD / INIT_SYSADMIN_PASSWORD / INIT_VIEWER_PASSWORD
INIT_BLACKLIST_PASSWORD / INIT_SMOKE_PASSWORD / INIT_BUSINESS_PASSWORD
```

可选：

```text
DB_POOL_MAX / DB_POOL_MIN
MINIO_PRESIGNED_EXPIRY
```

启动：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
java -jar living-frame-admin\target\living-frame-admin.jar
```

`prod` 不执行 `schema.sql`/`data.sql`，数据库升级需使用正式迁移流程。

## 4. 认证与权限

- `GET /api/auth/captcha`：获取验证码，返回 `captchaId`、`expiresIn`（有效期秒数）和可直接用于 `<img src>` 的 `data:image/png;base64,...`。
- 验证码有效期 **60 秒**，登录页展示失效倒计时，到期自动刷新；点击验证码或“立即刷新”可手动刷新。
- `POST /api/auth/login`：`username/password/captchaId/captchaCode`，成功返回 sa-token。
- 后续请求头：`Authorization: Bearer <token>`。
- `GET /api/auth/me`：当前用户、角色、权限。
- `GET /api/auth/menus`：当前用户菜单树。
- 业务登录：`POST /api/biz/auth/login`，请求字段同上。
- 权限校验：`@PreAuthorize("@ss.hasPermi('system:user:list')")`；超管直接放行。
- **角色边界**：
  - `SUPER_ADMIN`：代码级放行全部权限，可编辑角色、编辑权限、给角色分配权限、分配 `SUPER_ADMIN` 角色、重置超级管理员密码，并维护全局 AI 厂商 Key。
  - `SYSTEM_ADMIN`：可管理用户、登录日志、操作日志、系统黑名单；**没有角色管理和权限管理菜单**，也不能调用角色/权限管理查询接口。
  - `SMOKE_TEST`：保留的唯一冒烟测试角色；对应账号 `smoke_test`，无业务权限。
  - 其他角色按自身绑定权限执行，不能通过用户角色分配接口提权到 `SUPER_ADMIN`。
  - 角色/权限写接口额外使用 `@ss.hasRole('SUPER_ADMIN')`，即使误把编辑按钮权限授予其他角色，后端仍会拒绝。
- token 默认 7 天，活跃超时 30 分钟；用户禁用、重置密码、角色/权限变更会踢出相关用户。
- 验证码默认开启；仅本地调试可加 `--app.security.captcha-enabled=false` 关闭（生产禁止）。

### 4.1 登录风控与日志

- 同一账号连续登录失败 **5 次**后锁定 **5 分钟**，期间返回 423。
- 账号或来源 IP 失败累计 **10 次**后，自动把来源 IP 写入系统黑名单，后续登录直接拒绝。
- 登录成功会清理账号/IP 失败计数和锁定状态。
- 每次登录成功、失败、锁定、黑名单拦截都会写入 `sys_login_log`，包含登录 IP、User-Agent、端类型、结果说明。
- 登录日志可在「系统管理 → 登录日志」按用户名、IP、状态、端筛选。

### 4.2 操作日志

- 通过 `@OperationLog("操作描述")` 注解 + `OperationLogAspect` 记录后台写操作。
- 记录操作人、HTTP 方法、请求地址、操作 IP、成功/失败、失败原因、耗时。
- 覆盖后台用户、角色、权限、业务用户、AI 配置、系统黑名单等写操作；不记录请求体和密码等敏感信息。
- 可在「系统管理 → 操作日志」查看和筛选。

### 4.3 系统黑名单

- 所有黑名单均为**永久生效**，统一存储在 `sys_blacklist`，不使用过期时间。
- 登录失败累计 10 次后，系统自动把来源 IP 加入永久黑名单，原因记录为“来源 IP 连续登录失败达到 10 次，系统自动加入永久系统黑名单”。
- 「系统管理 → 系统黑名单」支持按 IP / 用户名新增、启用/禁用、移出，新增时必须填写完整的加入原因。
- 预置测试账号：`blacklist_test` / `Blacklist@123`，该账号已写入 `sys_blacklist`，登录会被直接拦截。
- 相关表：`sys_login_log`、`sys_operation_log`、`sys_blacklist`。

### 4.4 门户用户活跃度与一键拉黑

- 门户端每 30 秒调用一次 `/api/biz/auth/heartbeat`，后端更新 `biz_user.last_active_at`。
- 后台「业务管理 → 业务用户」展示：
  - 门户用户总数
  - 当前在线（最近 5 分钟有心跳）
  - 今日活跃、近 7 日活跃
  - 在线率、今日活跃率
  - 禁用、黑名单数量
- 用户表格展示在线状态和最近活跃时间，统计每 30 秒自动刷新。
- 操作列提供「拉黑」按钮，一键把门户用户加入 `USERNAME` 类型永久黑名单并禁用账号、踢出登录。
- 手机号、邮箱统一在服务层返回前脱敏，例如 `138****8000`、`ab****c@example.com`；数据库保留原值。

## 5. 主要接口

| 分组 | 路径 |
|---|---|
| 认证 | `/api/auth/*` |
| 后台用户 | `/api/system/users/*` |
| 角色 | `/api/system/roles/*` |
| 权限/菜单 | `/api/system/permissions/*` |
| 登录日志 | `/api/system/login-logs` |
| 操作日志 | `/api/system/operation-logs` |
| 黑名单 | `/api/system/blacklist/*` |
| 业务登录 | `/api/biz/auth/*` |
| 业务用户管理 | `/api/business/users/*`、`/api/business/users/activity`、`/api/business/users/{id}/blacklist` |
| AI | `/api/ai/status`、`/api/ai/configs`、`/api/ai/conversations`、`/api/ai/chat`、`/api/ai/agent` |
| 文件管理 | `/api/system/files/*` |

分页参数：`pageNum`、`pageSize`（上限 100），返回 `records/total/pageNum/pageSize`。

## 6. AI 配置（多模型 + Key 加密存储）

每个后台账号都有独立的 `sys_user_ai_config` 模型配置列表，API Key 在数据库中使用 AES-GCM 加密保存，前端接口只返回“是否已配置”，不返回 Key 明文或密文。

### 6.1 数据表

`sys_user_ai_config`：

- `user_id`
- `provider`：`DEEPSEEK` / `QWEN` / `OPENAI` / `CUSTOM`
- `provider_name`
- `base_url`
- `model`
- `api_key_encrypted`：AES-GCM 密文
- `reasoning_level`、`temperature`、`max_tokens`
- `is_default`

一个用户可以对应多条记录，同一厂商也可以配置多个模型。唯一键为 `(user_id, provider, model)`。

### 6.2 新用户默认配置

创建新用户时，系统自动为该用户写入三条默认配置：

| 厂商 | 模型 | Base URL |
|---|---|---|
| DeepSeek | `deepseek-flash` | `https://api.deepseek.com` |
| 通义千问 | `qwen3.8-max` | `https://dashscope.aliyuncs.com/compatible-mode` |
| OpenAI | `gpt-4o` | `https://api.openai.com` |

- 三条默认配置初始没有 Key。
- DeepSeek 默认作为当前默认模型。
- 用户可以删除默认配置，也可以新增自定义厂商和模型。
- 删除用户时会同步清理该用户的 AI 配置。

### 6.3 Key 加密与脱敏

- 加密算法：AES/GCM/NoPadding，随机 IV，Base64 存储。
- 加密密钥来自 `AI_CONFIG_SECRET` 环境变量，默认值仅用于本地演示。
- 前端永远拿不到 Key：列表接口只返回 `hasApiKey`。
- 编辑时留空表示保留原 Key，可以勾选“清除已保存的 API Key”。
- 调用模型时后端内部解密，Key 不写日志、不返回前端。

```yaml
app:
  ai:
    key-secret: ${AI_CONFIG_SECRET:living-frame-demo-ai-secret-change-me}
```

### 6.4 前端入口

- `/ai/settings`：模型列表、添加/编辑/删除、设置默认模型。
- 支持默认三家厂商和自定义 OpenAI 兼容厂商。
- 每个模型单独配置 Key；前端只显示“已加密配置/未配置”。
- AI 对话和 Agent 使用当前默认模型。

### 6.5 接口

- `GET /api/ai/status`
- `GET /api/ai/configs`
- `POST /api/ai/configs`
- `PUT /api/ai/configs/{id}`
- `DELETE /api/ai/configs/{id}`
- `PUT /api/ai/configs/{id}/default`
- `POST /api/ai/chat`
- `POST /api/ai/agent`

对话和 Agent 请求不再携带 API Key，后端根据当前用户默认配置解密后调用模型。

### 6.6 安全说明

- 数据库泄露时只暴露密文；但加密密钥必须通过环境变量或 KMS 管理，不能使用默认值上线。
- 前端不展示 Key，降低页面泄露和截图风险。
- 生产环境必须使用 HTTPS。
- 轮换 `AI_CONFIG_SECRET` 后，旧 Key 将无法解密，需要用户重新填写。

## 7. MinIO 文件管理

后台「系统管理 → 文件管理」已集成 MinIO 对象存储。

### 7.1 连接配置

```yaml
app:
  minio:
    endpoint: http://192.168.28.212:9510
    access-key: admin
    secret-key: Minio@123456
    default-bucket: living-frame
    presigned-expiry-seconds: 3600
```

支持环境变量覆盖：`MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_DEFAULT_BUCKET`。

- S3 API：`http://192.168.28.212:9510`
- 默认 Bucket `living-frame` 不存在时会自动创建。
- 上传大小上限 100MB，下载使用 1 小时有效的预签名 URL。

### 7.2 功能与接口

- 文件列表：`GET /api/system/files/objects`
- Bucket 列表：`GET /api/system/files/buckets`
- 创建/删除 Bucket：`POST /api/system/files/buckets`、`DELETE /api/system/files/buckets/{bucket}`
- 上传：`POST /api/system/files/upload`（multipart）
- 下载地址：`GET /api/system/files/download`
- 删除文件：`DELETE /api/system/files/objects`

页面支持 Bucket 切换、目录进入与面包屑、批量上传、下载、删除文件、新建/删除 Bucket。

权限：

- `system:file:list`：查看文件管理菜单
- `system:file:upload`：上传文件
- `system:file:delete`：删除文件
- `system:file:bucket`：创建/删除 Bucket

系统管理员默认拥有文件管理权限；其他角色按需授权。

### 7.3 用户头像

- 上传接口：`POST /api/system/files/avatar`（multipart，字段 `file`）
- 公开读取：`GET /api/public/avatars/{objectName}`
- 头像只允许 JPG、PNG、GIF、WebP，最大 5MB
- MinIO 对象路径：`avatar/yyyy/MM/{uuid}.{ext}`
- 数据库 `avatar` 字段保存上述半路径，不保存完整 URL
- 前端环境变量 `VITE_AVATAR_BASE_URL` 默认 `/api/public/avatars/`，通过 `resolveAvatarUrl()` 拼接完整地址
- 兼容历史数据中的完整 `http(s)` URL

## 8. AI 对话记忆

对话记忆采用 MySQL + Redis + Qdrant 三层结构：

- MySQL：长期保存 `ai_conversation`、`ai_message`。
- Redis：缓存最近 20 条会话消息，key 为 `living-frame:ai:chat:{conversationId}`，默认 6 小时过期。
- Qdrant：保存按用户隔离的对话轮次向量，用于跨会话语义召回。

### 8.1 Qdrant 配置

当前环境 Qdrant HTTP REST 实际端口为 `6033`（`6034` 不是 HTTP REST 端口）：

```yaml
app:
  ai:
    memory:
      enabled: true
      qdrant-url: ${QDRANT_URL:http://192.168.28.212:6033}
      collection: ${QDRANT_COLLECTION:living-frame-ai-memory}
      vector-size: 384
      top-k: 5
      score-threshold: 0.18
      history-limit: 10
      redis-ttl-hours: 6
```

应用首次写入记忆时会自动创建 Qdrant collection，距离函数为 Cosine。

### 8.2 记忆流程

1. 创建或恢复当前用户的会话。
2. 从 Redis 读取最近消息；缓存未命中时从 MySQL 读取并回填 Redis。
3. 使用当前问题在 Qdrant 中检索该用户其他会话的相关记忆，排除当前会话。
4. 将长期记忆、最近对话和当前问题组合后调用模型。
5. 将用户消息和助手回复写入 MySQL，并更新 Redis 缓存。
6. 将本轮对话写入 Qdrant，供后续会话检索。
7. Qdrant 不可用时自动降级为 MySQL + Redis，不影响正常对话。

### 8.3 记忆接口

```text
GET    /api/ai/conversations
GET    /api/ai/conversations/{id}
DELETE /api/ai/conversations/{id}
```

前端 AI 对话页支持历史会话列表、加载历史消息、新建会话和删除会话；助手回复会显示本次参考的长期记忆数量。

### 8.4 embedding 说明

当前使用本地轻量哈希 embedding（中文单字/双字 + 英文/数字 token，384 维），不依赖额外的 embedding API 和模型 Key。后续如需更高语义质量，可将 `AiEmbeddingService` 替换为 OpenAI/Qwen embedding 接口，同时保持 Qdrant 维度一致。

## 9. 客服工单

客服功能面向门户用户与后台客服坐席，支持草稿、派遣、聊天、转接、归档和评价。

### 9.1 角色与菜单

新增角色：

- `CS_CHAT`：客服聊天坐席
- `CS_TECH`：客服技术坐席

默认菜单只有：

- AI 对话
- 客服中心 → 客服聊天
- 客服中心 → 工单流程

其他角色（包括系统管理员）默认没有客服菜单；超级管理员代码级放行。

### 9.2 工单流程

- 门户用户创建草稿，草稿上限 5 条
- 提交草稿后按客服类型随机分配空闲坐席
- 门户新会话默认只进入在线聊天客服，技术客服不直接接新用户聊天
- 聊天客服可以把工单转接给在线技术客服，工单自动升级为技术工单
- 没有在线客服时分配给随机聊天客服，并提示客服工作时间
- 客服上线时弹窗提醒未读会话，可直接跳转最早未读聊天
- 单个客服默认最多同时处理 5 个会话，超出后用户排队等待
- 接单客服只能提交完结申请，不能直接归档
- 门户用户确认后工单才结束；用户拒绝则继续处理
- 归档后门户用户进行 1-5 分评价
- 客服排名按已归档处理数量排序，数量相同按平均评分排序

状态流转：

```text
DRAFT -> PENDING -> ASSIGNED -> PROCESSING -> WAIT_CONFIRM -> ARCHIVED
```

### 9.3 接口

门户用户：

```text
GET    /api/biz/cs/tickets
POST   /api/biz/cs/tickets/drafts
PUT    /api/biz/cs/tickets/{id}
DELETE /api/biz/cs/tickets/{id}
POST   /api/biz/cs/tickets/{id}/submit
GET    /api/biz/cs/tickets/{id}
POST   /api/biz/cs/tickets/{id}/messages
POST   /api/biz/cs/tickets/{id}/feedback
POST   /api/biz/cs/tickets/{id}/confirm-complete
POST   /api/biz/cs/tickets/{id}/reject-complete
```

客服坐席：

```text
GET    /api/cs/tickets
GET    /api/cs/tickets/{id}
POST   /api/cs/tickets/{id}/reply
POST   /api/cs/tickets/{id}/read
POST   /api/cs/tickets/{id}/dispatch
POST   /api/cs/tickets/{id}/transfer
POST   /api/cs/tickets/{id}/archive
GET    /api/cs/agents
POST   /api/cs/agents/heartbeat
POST   /api/cs/agents/offline
GET    /api/cs/agents/ranking
```

## 10. 数据库

核心表：

- `sys_user`、`sys_role`、`sys_permission`
- `sys_user_role`、`sys_role_permission`
- `biz_user`
- `sys_user_ai_config`
- `sys_login_log`、`sys_operation_log`、`sys_blacklist`

初始化数据包含 5 个角色、35 个菜单/按钮权限及角色授权；账号由 `AccountInitializer` 创建。

注意：雪花 ID 为 `BIGINT`，前端/第三方调用方应按字符串处理，避免 JS 精度丢失。

## 11. 测试

```powershell
& 'D:\Work Tools\apache-maven-3.9.16\bin\mvn.cmd' test
```

端到端冒烟：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
```

冒烟覆盖：健康检查、验证码登录、`/me`、用户分页、创建用户、角色-权限绑定、VIEWER 403、业务登录、AI 未配置 Key 时拒绝、日志与黑名单接口、登出后 401。

## 12. 常见问题

- **401**：检查 `Authorization` 是否为 `Bearer <token>`，token 是否被踢出/过期。
- **403**：当前角色未绑定对应权限；用 admin 在 `/api/system/roles/{id}/permissions` 分配。
- **验证码 500**：项目使用 `SpecCaptcha`，不依赖 JDK 的 Nashorn 脚本引擎；若自行改回算术验证码，需要额外引入脚本引擎。
- **MySQL 连接失败**：确认 `192.168.28.212:3306` 可达、root 密码正确；MySQL 9.x 使用 `caching_sha2_password`，URL 已带 `allowPublicKeyRetrieval=true`。
- **Redis 连接失败**：确认密码 `Cjx991121!` 和 6854 端口；Redisson 与 Spring Data Redis 使用同一实例。
- **8080 被占用**：启动参数加 `--server.port=8081`。
