# SmartTable · 个人情况表单填写管理系统

管理员动态维护表单模板（栏位、填写规则、排序、填写周期），普通用户按模板填写信息；
系统记录每个栏位的填写时间，超过填写周期自动在界面提醒更新。

## 技术栈

- 数据库：MySQL 8
- 后端：Spring Boot 3 + MyBatis-Plus + Spring Security + JWT + Redis
- 前端：Vue 3 + Naive UI（JavaScript）+ Pinia + Vite
- 主键：8 位 NanoId（排除易混字符）
- 权限：RBAC（用户 / 角色 / 权限 / 用户分组）+ 模板按分组授权

## 快速开始

### 1. 启动依赖（MySQL + Redis，Docker Compose）

本仓库的 [docker-compose.yml](docker-compose.yml) 直接使用本机已有的镜像
`mysql:8.4.5` 与 `redis:7-alpine`，并设置 `pull_policy: never`，**不会联网拉取镜像**。

```bash
docker compose up -d      # 启动; 首次启动 MySQL 数据卷会自动初始化
docker compose ps         # 查看健康状态
docker compose down       # 停止
docker compose down -v    # 停止并删除数据卷(清空数据)
```

默认端口/密码可用根目录 `.env` 覆盖（复制 [.env.example](.env.example) 为 `.env`）：
`MYSQL_PORT` / `REDIS_PORT` / `MYSQL_ROOT_PASSWORD`。
后端默认连接 `localhost:3306`（root/root）、`localhost:6379`，与 compose 一致；
数据库 `form_system` 由连接串 `createDatabaseIfNotExist=true` 自动创建，表结构由 Flyway 自动迁移。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run        # http://localhost:8080/api
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev                # http://localhost:5173 (自动代理 /api -> 8080)
```

默认管理员账号：`admin / admin123`（首次启动由 `DataInitializer` 创建，请尽快改密）。

## 功能概览

- 模板管理：草稿 / 发布 / 归档，按用户分组授权可见范围
- 栏位管理：文本、多行文本、数字、日期、日期时间、单选、多选；
  必填、长度、数值范围、正则等规则；排序；**填写周期按栏位可选**（不设置周期的栏位永不超期）
- 用户填写：动态渲染、前后端双重校验、当前值、按栏位查看填写历史
- 超期提醒：工作台待办统计、填写页「已超期 · 需更新」标记
- 系统管理：用户、角色、权限（只读树）、用户分组，按钮级权限控制

## 待办（Roadmap）

- [ ] **管理员查看 / 导出用户填报数据**：当前 `/forms/**` 仅支持用户查看自己的数据。
      待补充管理员视角：按模板 / 用户查看填报内容与完成情况、超期统计，并支持导出（如 Excel）。
- [ ] 主动通知：在界面内提醒之外，增加邮件 / 站内消息等超期推送渠道（需定时任务）。
- [ ] 带周期栏位的「确认仍有效」：信息未变化时也可刷新填写时间、消除超期提醒。

## 目录结构

```
backend/    Spring Boot 服务
frontend/   Vue3 前端
docker-compose.yml  本地 MySQL + Redis
```
