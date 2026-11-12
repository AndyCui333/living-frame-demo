# Living Frame Demo

一个基于 **Spring Boot 3 + Vue 3** 的前后端分离后台管理系统示例，覆盖企业后台常见的权限、系统管理、文件存储和 AI 能力。

## 功能概览

### 系统管理

- 用户管理：分页查询、搜索、创建、编辑、删除、重置密码、分配角色
- 角色管理：角色维护、菜单权限和按钮权限分配
- 权限管理：菜单树、按钮权限编码、前端路由配置
- 登录日志、操作日志
- 系统黑名单：IP / 用户名永久黑名单，登录前拦截
- 验证码登录：60 秒有效期、倒计时、自动刷新

### 权限模型

- `SUPER_ADMIN` 代码级放行全部权限
- 只有超级管理员可以维护角色和权限管理菜单
- 系统管理员负责用户、日志、黑名单和文件管理
- 按钮级权限通过前端 `v-permission` 指令控制

### 文件管理

- 集成 MinIO 对象存储
- Bucket 创建、删除、切换
- 目录浏览、面包屑导航
- 文件上传、下载、删除
- 图片、视频、音频、PDF、文本/代码在线预览
- 用户头像上传到 MinIO，数据库保存半路径，前端通过环境变量拼接完整地址

### AI 能力

- 用户独立维护 AI 厂商、Base URL、模型和 API Key
- API Key 使用 AES-GCM 加密存储，接口不返回密钥
- 支持 DeepSeek、通义千问、OpenAI 和自定义 OpenAI 兼容厂商
- AI 对话和 AI Agent 工具调用
- MySQL + Redis + Qdrant 对话记忆
  - MySQL 持久化会话和消息
  - Redis 缓存最近对话
  - Qdrant 跨会话语义记忆召回

### 客服工单

- 门户用户创建工单草稿，每个用户最多 5 条草稿
- 提交后按客服类型随机派遣给空闲客服坐席
- 客服聊天和技术客服两类坐席
- 客服可回复、转接给同类型空闲坐席
- 客服列表展示名称、空闲状态和客服类型
- 截单归档后门户用户进行 1-5 分评价
- 仅超级管理员、客服聊天角色和客服技术角色可见

### 前端体验

- Vue 3 + TypeScript + Element Plus
- 响应式后台布局、可收起侧边栏
- 仪表盘、系统管理、业务管理、AI 能力等模块
- 独立移动端门户：手机 App / 小程序风格，底部只有首页和我的，可拖拽圆形客服入口
- 统一卡片、表格、工具栏、空状态和表单样式

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

## 项目结构

```text
living-frame-demo
├── living-frame-common      通用结果、异常、上下文、工具
├── living-frame-system      用户、角色、权限、日志、黑名单、文件
├── living-frame-framework   安全、Redis、MyBatis-Plus、Web 配置
├── living-frame-business    业务用户模块
├── living-frame-ai          AI 对话、Agent、配置、会话记忆
├── living-frame-admin       启动模块与配置
├── living-frame-vue3        管理后台 Vue 3 前端
└── living-frame-portal      移动端门户 Vue 3 前端
```

## 分支说明

- `master`：项目功能简介，只保留本说明文档
- `maven-version`：当前 **Maven 多模块版本** 的完整代码
- `gradle-version`：后续计划提供的 Gradle 版本

当前代码实现位于 `maven-version` 分支，是 **Maven 构建版本**；后续会提供对应的 Gradle 版本。
