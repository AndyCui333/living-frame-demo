# Living Frame Portal

Living Frame 门户用户端，独立的移动端 Web App，采用 Vue 3 + Vite 编写。

## 特点

- 手机 App / 微信小程序风格
- 底部导航只保留「首页」「我的」
- 每个页面右下角固定圆形客服入口
- 客服入口支持拖拽，并自动保存位置
- 点击客服入口打开客服聊天弹层
- 用户发送第一条消息时自动创建并提交工单
- 未结束工单优先复用，工单结束前不会创建新工单
- 工单结束后再次咨询才开启新工单
- 客服申请完结后，需要门户用户确认；用户拒绝则继续处理
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
- `/me/profile`：个人资料二级页
- 客服聊天为全局弹层，不占用底部导航

## 我的

- 首页快捷服务只保留「在线客服」「我的工单」
- 「我的」首页只展示头像和昵称
- 个人资料二级页支持修改：
  - 昵称
  - 手机号
  - 邮箱
  - 头像
  - 登录密码
- 二级页不展示角色
- 退出登录会清除本地草稿

客服 API 使用 `/api/biz/cs/tickets`，与后台客服工单模块共用同一套数据。
