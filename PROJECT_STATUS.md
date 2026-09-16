# SmartTable 工程建设现状与待办

> 仓库：https://github.com/augensternsong/SmartTable
> 分支：`trae/agent-eEshb0`，提交 `ee7375d feat: 动态表单填写管理系统`

## 一、项目概述

个人情况表单填写管理系统：
- 管理员动态维护表单模板（栏位、填写规则、排序、填写周期）
- 普通用户按模板填写，系统记录填写时间，超期自动提醒
- 技术栈：MySQL 8 + Spring Boot + Vue3 + Naive-UI + Redis
- 数据库 ID：8 位 NanoId
- 权限模型：RBAC（角色 + 用户分组）

## 二、已完成模块

### 后端（/workspace/backend）

| 模块 | 路径 | 状态 |
|---|---|---|
| 数据库 schema | `src/main/resources/db/migration/V1__init_schema.sql` | 13 张表，含初始角色权限数据 |
| 工程骨架 | `pom.xml` + `FormSystemApplication.java` | Spring Boot + MyBatis-Plus + Redis + JWT |
| NanoId 生成器 | `common/NanoIdGenerator.java` | 自实现 8 位 NanoId（排除易混字符） |
| 用户/角色/权限/分组 | `controller/SysUserController.java` 等 | 完整 RBAC |
| 模板 + 栏位 + 选项管理 | `controller/FormTemplateController.java` + `FormTemplateFieldController.java` | 含软版本策略 |
| 用户填写模块 | `service/UserFormService.java` | 填写/历史/超期提醒计算 |
| 模板缓存 | `service/TemplateCacheService.java` | Redis 缓存已发布模板结构 |

### 前端（/workspace/frontend）

| 模块 | 路径 | 状态 |
|---|---|---|
| 工程骨架 | `package.json` + `vite.config.js` + `src/main.js` | vite + vue3 + naive-ui + pinia + vue-router |
| axios 拦截器 | `src/utils/request.js` | 含 401 自动刷新 token |
| auth store | `src/stores/auth.js` | token 持久化 + 权限判断 |
| 路由 + 守卫 | `src/router/index.js` | 全部路由已注册，含权限校验 |
| 登录页 | `src/views/login/Login.vue` | ✅ |
| 主布局 + 动态菜单 | `src/layouts/MainLayout.vue` | ✅ |
| 用户工作台 | `src/views/user/Dashboard.vue` | 待办提醒统计 ✅ |
| 我的表单 | `src/views/user/MyForm.vue` | 模板卡片列表 ✅ |
| 填写表单 | `src/views/user/FormFill.vue` | 动态渲染 + 校验 + 提交 + 历史 ✅ |
| 个人中心 | `src/views/user/Profile.vue` | 修改密码 ✅ |
| 404 页 | `src/views/error/NotFound.vue` | ✅ |
| 管理端-模板列表 | `src/views/admin/TemplateList.vue` | 增删改查/发布/归档/分配分组 ✅ |
| 管理端-栏位管理 | `src/views/admin/TemplateField.vue` | 栏位增改/启停/选项编辑器 ✅ |
| 管理端-用户管理 | `src/views/admin/SystemUser.vue` | 分页查询/增改删/分配角色/分配分组/重置密码 ✅ |
| 管理端-角色管理 | `src/views/admin/SystemRole.vue` | 分页查询/增改删（内置禁删）/权限树分配 ✅ |
| 管理端-用户分组 | `src/views/admin/SystemGroup.vue` | 分页查询/增改删/成员数/分配用户 ✅ |
| 管理端-权限管理 | `src/views/admin/SystemPerm.vue` | 权限只读树/类型标记/筛选 ✅ |

### API 模块（前端）
- `src/api/auth.js` `src/api/user.js` `src/api/role.js`
- `src/api/group.js` `src/api/template.js` `src/api/form.js`
- `src/api/permission.js`

全部已就绪。

## 三、系统管理页面（已完成 ✅）

4 个页面均已创建，`npm run build` 通过：

| 文件 | 路由 | 功能 |
|---|---|---|
| `src/views/admin/SystemUser.vue` | `/system/user` | 分页（用户名/昵称/角色/分组/状态过滤）、新建/编辑/删除（自己和超管禁删）、分配角色（非超管禁用超管选项）、分配分组、重置密码 |
| `src/views/admin/SystemRole.vue` | `/system/role` | 分页/关键词、新建/编辑（编码不可改）、删除（内置角色禁删、有关联禁删）、`n-tree` 级联勾选分配权限（含半选父节点） |
| `src/views/admin/SystemGroup.vue` | `/system/group` | 分页/关键词、新建/编辑/删除、成员数展示、勾选分配用户（带搜索） |
| `src/views/admin/SystemPerm.vue` | `/system/perm` | 权限只读树，菜单/按钮类型标签，名称/编码筛选，展开收起 |

> 注：路由守卫中权限管理页使用菜单权限码 `system:perm`（DB 中不存在 `sys:perm:update` 按钮权限，权限为只读）。

## 三·补、本轮增强（2026-09-16）

1. **填写周期按栏位可选**：栏位编辑弹窗改为「启用填写周期」开关 + 周期天数；关闭则 `fillCycleDays=null`，该栏位永不超期。后端对可清空字段加 `@TableField(updateStrategy = ALWAYS)`，修复 `updateById` 默认忽略 null 导致无法取消周期的问题。
2. **容器化依赖**：根目录新增 `docker-compose.yml`（`mysql:8.4.5` + `redis:7-alpine`，`pull_policy: never` 复用本机镜像）与 `.env.example`。
3. **技术债**：删除草稿模板时级联清理 `form_field_option`，不再留孤儿选项。
4. **单元测试**：新增 `NanoIdGeneratorTest`（10 项）与 `UserFormValueValidationTest`（8 项，覆盖必填/长度/正则/数值/日期/选项），`mvn test` 全绿。
5. README 已补完整启动说明与待办（管理员查看/导出填报数据等）。
6. **修复冷启动致命 bug（冒烟发现）**：全部 13 个实体的 String 主键原标 `IdType.ASSIGN_ID`，MyBatis-Plus 该策略会调用生成器的 Number 版 `nextId()`（被我们主动抛异常），导致全新库首次插入（初始化 admin）失败、应用无法启动。已统一切为 `IdType.ASSIGN_UUID`（走 `NanoIdGenerator.nextUUID()`），并同步 `application.yml` 全局 `id-type`。已用全新容器库验证：启动成功、admin 初始化为 8 位 NanoId、登录/JWT/分页接口正常。

## 四、桌面端启动步骤

```bash
git clone https://github.com/augensternsong/SmartTable.git
cd SmartTable
git checkout trae/agent-eEshb0

# 后端
cd backend
mvn spring-boot:run   # 监听 :8080

# 前端（另开终端）
cd frontend
npm install
npm run dev           # 监听 :5173，自动代理 /api -> :8080
```

**默认管理员账号**：`admin / admin123`

## 五、关键设计决策（备忘）

1. **栏位软版本策略**：新栏位 `status=ACTIVE` + `version=当前模板版本`；修改就地更新；删除仅置 `INACTIVE`；`field_code` 全生命周期稳定。
2. **模板缓存**：Redis 缓存「已发布模板」结构（模板 + ACTIVE 栏位 + 选项 + 已分配分组），避免用户每次填写时多表 JOIN。
3. **NanoId**：自实现，字符表排除 `0/O/1/I/l`，8 位，`SecureRandom`。
4. **权限**：3 个内置角色 `SUPER_ADMIN / ADMIN / USER` + 用户分组控制模板可见范围。
5. **填写周期**：`fill_cycle_days` 字段，超期在前端标记「已超期 · 需更新」并出现在工作台待办列表。
