# 本地启动生产环境

后台管理前端和门户前端都新增了 `dev:prod` 模式，可以在本地启动，直接连接生产后端：

```text
http://120.26.36.225:7369
```

## 启动后台管理

```powershell
cd F:\IdeaProjects\living-frame-demo\living-frame-vue3
npm run dev:prod
```

访问：

```text
http://127.0.0.1:5173
```

后台管理请求链路：

```text
浏览器 -> 127.0.0.1:5173/api -> Vite 代理 -> http://120.26.36.225:7369/api
```

## 启动门户

另开一个终端：

```powershell
cd F:\IdeaProjects\living-frame-demo\living-frame-portal
npm run dev:prod
```

访问：

```text
http://127.0.0.1:5175
```

门户请求链路：

```text
浏览器 -> http://120.26.36.225:7369/api
```

门户生产环境使用真实生产 API 地址，不再经过本地后端。

## 配置文件

### 后台管理

- [living-frame-vue3/.env.production](living-frame-vue3/.env.production)
- [living-frame-vue3/vite.config.ts](living-frame-vue3/vite.config.ts)

```env
VITE_API_BASE_URL=/api
VITE_AVATAR_BASE_URL=/api/public/avatars/
VITE_API_PROXY=http://120.26.36.225:7369
```

### 门户

- [living-frame-portal/.env.production](living-frame-portal/.env.production)
- [living-frame-portal/vite.config.ts](living-frame-portal/vite.config.ts)

```env
VITE_API_BASE_URL=http://120.26.36.225:7369/api
VITE_AVATAR_BASE_URL=http://120.26.36.225:7369/api/public/avatars/
VITE_API_PROXY=http://120.26.36.225:7369
```

## 普通本地开发

如果需要连接本地后端（默认 `8080`），继续使用：

```powershell
npm run dev
```

后台管理默认代理：

```text
http://127.0.0.1:8080
```

门户默认代理同理。

## 验证生产后端

```powershell
curl.exe http://120.26.36.225:7369/actuator/health
curl.exe http://120.26.36.225:7369/api/auth/captcha
```

两个接口都应返回 `200`。

## 生产构建

后台管理：

```powershell
cd living-frame-vue3
npm run build
```

门户：

```powershell
cd living-frame-portal
npm run build
```

构建产物分别在：

```text
living-frame-vue3/dist
living-frame-portal/dist
```
