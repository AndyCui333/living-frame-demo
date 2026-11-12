# living-frame-vue3

Living Frame 后台管理前端，技术栈：Vue 3 + TypeScript + Element Plus + Vite + Pinia + Vue Router。

## 功能

- 验证码登录（60 秒倒计时、到期自动刷新）、token 持久化、401 自动退出
- 动态菜单：由后端 `/api/auth/menus` 返回的菜单树渲染
- 工作台：用户、角色、权限、业务用户统计与快捷入口
- 用户管理：分页、搜索、新增/编辑、启用/禁用、分配角色、重置密码、删除
- 角色管理（仅超级管理员）：分页、新增/编辑、启用/禁用、分配菜单/按钮权限、删除
- 权限管理（仅超级管理员）：树形表格、菜单/按钮类型、父子节点、路由地址维护
- 登录日志：登录时间、用户、端、登录 IP、状态、说明、浏览器 UA
- 操作日志：操作人、请求方法/地址、IP、状态、耗时、失败原因
- 系统黑名单：永久黑名单新增、启用禁用、移出，新增时填写完整加入原因
- 文件管理：MinIO Bucket 管理、目录浏览、批量上传、下载、删除文件、图片/视频/文本预览
- 用户头像：上传到 MinIO，数据库保存半路径，前端通过 `VITE_AVATAR_BASE_URL` 拼接显示
- 业务用户管理：独立于后台账号的 CRUD 与状态管理
- AI 对话：MySQL/Redis 持久化多轮会话，Qdrant 长期记忆召回；支持历史会话列表、加载、删除、快捷问题、复制回复
- AI Agent：任务执行、工具调用结果展示、可用工具说明
- AI 设置：每个账号可维护多个模型和厂商地址；新用户默认生成 DeepSeek、通义千问、OpenAI 三条配置；API Key 加密存储，前端只显示配置状态
- 按钮级权限：`v-permission` 指令
- 403 / 404 页面、响应式布局

## 开发

要求 Node.js 20+（当前机器为 Node 26），后端运行在 `http://127.0.0.1:8080`。

```powershell
cd living-frame-vue3
npm install
npm run dev
```

打开 `http://127.0.0.1:5173`。Vite 已将 `/api` 代理到 `http://127.0.0.1:8080`。

生产构建预览：

```powershell
npm run preview
```

预览地址 `http://127.0.0.1:4173`，同样代理 `/api` 到后端。

## 构建

```powershell
npm run build
```

产物在 `living-frame-vue3/dist/`。`npm run build` 会先执行 `vue-tsc --noEmit` 做类型检查，再执行 Vite 构建。

## 环境变量

`.env.development` / `.env.production`：

```ini
VITE_API_BASE_URL=/api
VITE_AVATAR_BASE_URL=/api/public/avatars/
```

`VITE_AVATAR_BASE_URL` 用于拼接用户头像半路径；生产环境可以改成 MinIO/CDN 的公开访问前缀。

如果前后端分开部署，把该值改成后端完整地址，例如 `https://api.example.com/api`，并在后端 CORS 中放行前端域名。

## 目录

```
src/
├── api/          Axios 封装与接口模块
├── directives/   v-permission 按钮权限
├── layout/       后台整体布局、侧边菜单、顶栏
├── router/       路由与登录/权限守卫
├── stores/       Pinia 认证状态
├── styles/       全局样式与 Element Plus 主题变量
├── types/        API 类型、路由 meta 扩展
├── utils/        token、ID 归一化
└── views/        登录、工作台、系统管理、业务用户、AI、错误页
```

## 与后端的数据约定

- 统一响应：`{ code, message, data }`，`code === 200` 为成功。
- 认证头：`Authorization: Bearer <token>`。
- 大整数 ID：使用 `json-bigint` 解析，避免雪花 ID 在 JavaScript 中精度丢失；API 层统一转成字符串。
- 菜单：后端返回 `children` 树，前端按 `path` 注册的路由跳转。
- 按钮权限：后端返回权限编码数组，前端 `v-permission="'system:user:create'"`。
- AI 配置：`/api/ai/status`、`/api/ai/configs`；模型和厂商按账号保存，API Key 后端加密，前端不展示 Key。

## 注意

- 后端验证码默认开启，登录页会请求 `/api/auth/captcha`。
- 后端 AI 未配置时，AI 页面会提示未启用，不影响其他功能。
- 不要在前端代码中写死 API Key；模型 Key 只放在后端 `application-dev.yml` 或环境变量。
