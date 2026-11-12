# Living Frame Portal

Living Frame 门户用户端，独立的移动端 Web App，采用 Vue 3 + Vite 编写。

## 特点

- 手机 App / 微信小程序风格
- 底部导航只保留「首页」「我的」
- 每个页面右下角固定圆形客服入口
- 客服入口支持拖拽，并自动保存位置
- 点击客服入口打开客服聊天弹层
- 用户发送第一条消息时自动创建并提交工单
- 2 分钟未继续且工单不处于进行中时，下一条消息自动开启新工单
- 右上角查看历史工单
- 工单归档后可在聊天中评价

## 开发

```bash
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5175`

Vite 将 `/api` 代理到 `VITE_API_PROXY`，未设置时默认代理到 `http://127.0.0.1:8080`。

## 构建

```bash
npm run build
```

产物在 `dist/`。

## 环境变量

`.env.development` / `.env.production`：

```env
VITE_API_BASE_URL=/api
VITE_AVATAR_BASE_URL=/api/public/avatars/
```

## 演示账号

```text
biz01 / Biz@123
```

## 页面

- `/login`：门户登录
- `/`：首页
- `/me`：我的
- 客服聊天为全局弹层，不占用底部导航

客服 API 使用 `/api/biz/cs/tickets`，与后台客服工单模块共用同一套数据。
