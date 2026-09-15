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

### API 模块（前端）
- `src/api/auth.js` `src/api/user.js` `src/api/role.js`
- `src/api/group.js` `src/api/template.js` `src/api/form.js`
- `src/api/permission.js`

全部已就绪。

## 三、未完成模块（在电脑端继续）

`src/router/index.js` 已为以下页面注册路由，但 **Vue 文件尚未创建**，导航到这些页会 404：

| 待建文件 | 路由 | 权限码 | 后端 API |
|---|---|---|---|
| `src/views/admin/SystemUser.vue` | `/system/user` | `sys:user:update` | `SysUserController` |
| `src/views/admin/SystemRole.vue` | `/system/role` | `sys:role:update` | `SysRoleController` |
| `src/views/admin/SystemGroup.vue` | `/system/group` | `sys:group:update` | `SysUserGroupController` |
| `src/views/admin/SystemPerm.vue` | `/system/perm` | `sys:perm:update` | `SysPermissionController`（只读树） |

### 实现指引

1. **后端 API 与前端 API 模块均已就绪**，直接照 `TemplateList.vue` 的模式写这 4 个页面即可。
2. 关键 DTO 字段参考（位于 `backend/src/main/java/com/example/form/dto/`）：
   - `user/UserVO.java`：`id / username / nickname / email / phone / status / roleIds / groupIds`
   - `user/UserSaveRequest.java`：含 `password`（新建必填，编辑为空不改）
   - `role/RoleVO.java`：含 `permissionIds`；`RoleSaveRequest`：`roleCode` 大写字母开头
   - `group/GroupVO.java`：含 `memberCount / userIds`；`GroupSaveRequest`
   - `perm/PermissionNode.java`：树形结构，含 `children`
3. 特殊接口：
   - 用户分配角色：`PUT /sys/users/{id}/roles` body `{ roleIds }`
   - 用户分配分组：`PUT /sys/users/{id}/groups` body `{ groupIds }`
   - 用户重置密码：`PUT /sys/users/{id}/password` body `{ newPassword }`
   - 角色分配权限：`PUT /sys/roles/{id}/permissions` body `{ permissionIds }`
   - 分组分配用户：`PUT /sys/groups/{id}/users` body `{ userIds }`
   - 当前用户不可分配角色：`GET /sys/users/editable-roles`
   - 权限树：`GET /sys/permissions/tree`

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
