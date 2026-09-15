-- ============================================================
-- Form System 初始化 Schema
-- MySQL 8.x, 主键统一使用 8位 nanoid (VARCHAR(8))
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 1. 用户表 ----------
CREATE TABLE sys_user (
  id          VARCHAR(8)   NOT NULL COMMENT '8位nanoid',
  username    VARCHAR(64)  NOT NULL COMMENT '登录名',
  password    VARCHAR(128) NOT NULL COMMENT 'bcrypt加密',
  nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  email       VARCHAR(128) DEFAULT NULL,
  phone       VARCHAR(32)  DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  remark      VARCHAR(255) DEFAULT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 2. 角色表 ----------
CREATE TABLE sys_role (
  id          VARCHAR(8)   NOT NULL,
  role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码: SUPER_ADMIN/ADMIN/USER',
  role_name   VARCHAR(64)  NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ---------- 3. 用户-角色关联 ----------
CREATE TABLE sys_user_role (
  id         VARCHAR(8) NOT NULL,
  user_id    VARCHAR(8) NOT NULL,
  role_id    VARCHAR(8) NOT NULL,
  created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- ---------- 4. 权限表(菜单/按钮) ----------
CREATE TABLE sys_permission (
  id          VARCHAR(8)   NOT NULL,
  parent_id   VARCHAR(8)   DEFAULT NULL,
  perm_code   VARCHAR(128) NOT NULL COMMENT '权限编码 如 form:template:create',
  perm_name   VARCHAR(64)  NOT NULL,
  perm_type   VARCHAR(16)  NOT NULL DEFAULT 'MENU' COMMENT 'MENU菜单 BUTTON按钮 API接口',
  route_path  VARCHAR(255) DEFAULT NULL COMMENT '前端路由path',
  route_name  VARCHAR(128) DEFAULT NULL COMMENT '前端路由name',
  component   VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
  icon        VARCHAR(64)  DEFAULT NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  visible     TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (perm_code),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ---------- 5. 角色-权限关联 ----------
CREATE TABLE sys_role_permission (
  id            VARCHAR(8) NOT NULL,
  role_id       VARCHAR(8) NOT NULL,
  permission_id VARCHAR(8) NOT NULL,
  created_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

-- ---------- 6. 用户分组 ----------
CREATE TABLE sys_user_group (
  id          VARCHAR(8)   NOT NULL,
  group_code  VARCHAR(64)  NOT NULL,
  group_name  VARCHAR(64)  NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户分组';

-- ---------- 7. 用户-分组关联 ----------
CREATE TABLE sys_user_group_member (
  id         VARCHAR(8) NOT NULL,
  user_id    VARCHAR(8) NOT NULL,
  group_id   VARCHAR(8) NOT NULL,
  created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_group (user_id, group_id),
  KEY idx_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-分组关联';

-- ---------- 8. 表单模板 ----------
CREATE TABLE form_template (
  id            VARCHAR(8)   NOT NULL,
  template_code VARCHAR(64)   NOT NULL,
  template_name VARCHAR(128)  NOT NULL,
  description   VARCHAR(512) DEFAULT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿 PUBLISHED已发布 ARCHIVED已归档',
  version       INT          NOT NULL DEFAULT 1 COMMENT '软版本号,发布+1',
  created_by    VARCHAR(8)   NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单模板';

-- ---------- 9. 模板栏位(软版本: status标志, 不物理删除) ----------
CREATE TABLE form_template_field (
  id             VARCHAR(8)     NOT NULL,
  template_id    VARCHAR(8)     NOT NULL,
  field_code     VARCHAR(64)    NOT NULL COMMENT '栏位编码, 软版本下保持稳定, 用户数据按此绑定',
  field_name     VARCHAR(128)   NOT NULL,
  field_type     VARCHAR(32)    NOT NULL COMMENT 'TEXT/TEXTAREA/NUMBER/DATE/DATETIME/SELECT_SINGLE/SELECT_MULTI',
  sort_order     INT            NOT NULL DEFAULT 0,
  required       TINYINT        NOT NULL DEFAULT 0,
  max_length     INT            DEFAULT NULL,
  min_value      DECIMAL(20, 4) DEFAULT NULL,
  max_value      DECIMAL(20, 4) DEFAULT NULL,
  regex_pattern  VARCHAR(512)   DEFAULT NULL,
  fill_cycle_days INT           DEFAULT NULL COMMENT '填写周期天数, NULL表示无周期',
  placeholder    VARCHAR(255)   DEFAULT NULL,
  description    VARCHAR(512)   DEFAULT NULL,
  status         VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE生效 INACTIVE停用(软删除)',
  version        INT            NOT NULL DEFAULT 1 COMMENT '所属模板版本',
  created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_field_code (template_id, field_code),
  KEY idx_template (template_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板栏位';

-- ---------- 10. 选择类栏位选项 ----------
CREATE TABLE form_field_option (
  id           VARCHAR(8)   NOT NULL,
  field_id     VARCHAR(8)   NOT NULL,
  option_value VARCHAR(128) NOT NULL,
  option_label VARCHAR(128) NOT NULL,
  sort_order   INT          NOT NULL DEFAULT 0,
  status       TINYINT      NOT NULL DEFAULT 1,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_field (field_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='栏位选项';

-- ---------- 11. 模板-用户分组分配 ----------
CREATE TABLE form_template_group_assign (
  id          VARCHAR(8) NOT NULL,
  template_id VARCHAR(8) NOT NULL,
  group_id    VARCHAR(8) NOT NULL,
  created_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_group (template_id, group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板-分组分配';

-- ---------- 12. 用户填写当前值 ----------
CREATE TABLE user_form_value (
  id           VARCHAR(8) NOT NULL,
  user_id      VARCHAR(8) NOT NULL,
  template_id  VARCHAR(8) NOT NULL,
  field_id     VARCHAR(8) NOT NULL,
  field_value  TEXT       DEFAULT NULL COMMENT '填写值, 多选存JSON数组',
  filled_at    DATETIME   NOT NULL COMMENT '本次填写时间',
  filled_by    VARCHAR(8) NOT NULL COMMENT '填写人ID(可能代填)',
  created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_field (user_id, field_id) COMMENT '一个用户一个栏位一条当前值',
  KEY idx_user_template (user_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户填写当前值';

-- ---------- 13. 用户填写历史(全量) ----------
CREATE TABLE user_form_value_history (
  id           VARCHAR(8) NOT NULL,
  user_id      VARCHAR(8) NOT NULL,
  template_id  VARCHAR(8) NOT NULL,
  field_id     VARCHAR(8) NOT NULL,
  field_value  TEXT       DEFAULT NULL,
  filled_at    DATETIME   NOT NULL,
  filled_by    VARCHAR(8) NOT NULL,
  created_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_field (user_id, field_id, filled_at),
  KEY idx_filled_at (filled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户填写历史';

-- ============================================================
-- 初始数据: 角色 + 权限菜单树 (不含密码哈希, 用户由 Java 启动器初始化)
-- ============================================================

-- 角色
INSERT INTO sys_role (id, role_code, role_name, description, status) VALUES
  ('r0000001', 'SUPER_ADMIN', '超级管理员', '拥有全部权限, 不可删除', 1),
  ('r0000002', 'ADMIN',       '管理员',     '管理模板与用户, 不可删除', 1),
  ('r0000003', 'USER',        '普通用户',   '填写个人表单',           1);

-- 权限: 一级菜单
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, route_path, route_name, component, icon, sort_order, visible) VALUES
  ('p0000001', NULL,     'dashboard',       '工作台',     'MENU', '/dashboard',        'Dashboard',       'user/Dashboard',           'dashboard', 1, 1),
  ('p0000002', NULL,     'form-fill',       '我的表单',   'MENU', '/form/my',          'MyForm',          'user/MyForm',              'edit',      2, 1),
  ('p0000003', NULL,     'template',        '模板管理',   'MENU', '/template',         'Template',        'admin/TemplateList',       'list',      3, 1),
  ('p0000004', NULL,     'system',          '系统管理',   'MENU', '/system',           'System',          'Layout',                   'settings',  4, 1);

-- 子菜单: 模板管理下
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, route_path, route_name, component, icon, sort_order, visible) VALUES
  ('p0000031', 'p0000003', 'template:list',    '模板列表',  'MENU', '/template/list',     'TemplateList',     'admin/TemplateList',     NULL, 1, 1),
  ('p0000032', 'p0000003', 'template:field',    '栏位管理',  'MENU', '/template/field/:id', 'TemplateField',   'admin/TemplateField',    NULL, 2, 0);

-- 子菜单: 系统管理下
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, route_path, route_name, component, icon, sort_order, visible) VALUES
  ('p0000041', 'p0000004', 'system:user',   '用户管理', 'MENU', '/system/user',   'SystemUser',   'admin/SystemUser',   NULL, 1, 1),
  ('p0000042', 'p0000004', 'system:role',   '角色管理', 'MENU', '/system/role',   'SystemRole',   'admin/SystemRole',   NULL, 2, 1),
  ('p0000043', 'p0000004', 'system:group',  '用户分组', 'MENU', '/system/group',  'SystemGroup',  'admin/SystemGroup',  NULL, 3, 1),
  ('p0000044', 'p0000004', 'system:perm',   '权限管理', 'MENU', '/system/perm',   'SystemPerm',   'admin/SystemPerm',   NULL, 4, 1);

-- 按钮级权限(API粒度)
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, route_path, route_name, component, icon, sort_order, visible) VALUES
  ('p0000101', 'p0000031', 'form:template:create', '新建模板', 'BUTTON', NULL, NULL, NULL, NULL, 1, 0),
  ('p0000102', 'p0000031', 'form:template:update', '编辑模板', 'BUTTON', NULL, NULL, NULL, NULL, 2, 0),
  ('p0000103', 'p0000031', 'form:template:delete', '删除模板', 'BUTTON', NULL, NULL, NULL, NULL, 3, 0),
  ('p0000104', 'p0000031', 'form:template:publish','发布模板', 'BUTTON', NULL, NULL, NULL, NULL, 4, 0),
  ('p0000111', 'p0000032', 'form:field:create',    '新建栏位', 'BUTTON', NULL, NULL, NULL, NULL, 1, 0),
  ('p0000112', 'p0000032', 'form:field:update',    '编辑栏位', 'BUTTON', NULL, NULL, NULL, NULL, 2, 0),
  ('p0000113', 'p0000032', 'form:field:delete',    '停用栏位', 'BUTTON', NULL, NULL, NULL, NULL, 3, 0),
  ('p0000121', 'p0000041', 'sys:user:create',     '新建用户', 'BUTTON', NULL, NULL, NULL, NULL, 1, 0),
  ('p0000122', 'p0000041', 'sys:user:update',     '编辑用户', 'BUTTON', NULL, NULL, NULL, NULL, 2, 0),
  ('p0000123', 'p0000041', 'sys:user:delete',     '删除用户', 'BUTTON', NULL, NULL, NULL, NULL, 3, 0),
  ('p0000124', 'p0000041', 'sys:user:reset-pwd',  '重置密码', 'BUTTON', NULL, NULL, NULL, NULL, 4, 0),
  ('p0000131', 'p0000042', 'sys:role:create',     '新建角色', 'BUTTON', NULL, NULL, NULL, NULL, 1, 0),
  ('p0000132', 'p0000042', 'sys:role:update',     '编辑角色', 'BUTTON', NULL, NULL, NULL, NULL, 2, 0),
  ('p0000133', 'p0000042', 'sys:role:delete',     '删除角色', 'BUTTON', NULL, NULL, NULL, NULL, 3, 0),
  ('p0000134', 'p0000042', 'sys:role:assign-perm', '分配权限', 'BUTTON', NULL, NULL, NULL, NULL, 4, 0),
  ('p0000141', 'p0000043', 'sys:group:create',     '新建分组', 'BUTTON', NULL, NULL, NULL, NULL, 1, 0),
  ('p0000142', 'p0000043', 'sys:group:update',     '编辑分组', 'BUTTON', NULL, NULL, NULL, NULL, 2, 0),
  ('p0000143', 'p0000043', 'sys:group:delete',     '删除分组', 'BUTTON', NULL, NULL, NULL, NULL, 3, 0),
  ('p0000144', 'p0000043', 'sys:group:assign-user', '分配用户', 'BUTTON', NULL, NULL, NULL, NULL, 4, 0);

-- 角色-权限分配:
-- 超级管理员: 全部权限
-- 管理员: 模板+系统管理(除角色/权限管理)
-- 普通用户: 工作台 + 我的表单
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT CONCAT('rp', LPAD(ROW_NUMBER() OVER (ORDER BY id), 4, '0')) AS id, 'r0000001', id FROM sys_permission;

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
  ('rp20001', 'r0000002', 'p0000001'),
  ('rp20002', 'r0000002', 'p0000002'),
  ('rp20003', 'r0000002', 'p0000003'),
  ('rp20004', 'r0000002', 'p0000031'),
  ('rp20005', 'r0000002', 'p0000032'),
  ('rp20006', 'r0000002', 'p0000004'),
  ('rp20007', 'r0000002', 'p0000041'),
  ('rp20008', 'r0000002', 'p0000101'),
  ('rp20009', 'r0000002', 'p0000102'),
  ('rp20010', 'r0000002', 'p0000103'),
  ('rp20011', 'r0000002', 'p0000104'),
  ('rp20012', 'r0000002', 'p0000111'),
  ('rp20013', 'r0000002', 'p0000112'),
  ('rp20014', 'r0000002', 'p0000113'),
  ('rp20015', 'r0000002', 'p0000121'),
  ('rp20016', 'r0000002', 'p0000122'),
  ('rp20017', 'r0000002', 'p0000123'),
  ('rp20018', 'r0000002', 'p0000124'),
  ('rp20019', 'r0000002', 'p0000141'),
  ('rp20020', 'r0000002', 'p0000142'),
  ('rp20021', 'r0000002', 'p0000143'),
  ('rp20022', 'r0000002', 'p0000144');

INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
  ('rp30001', 'r0000003', 'p0000001'),
  ('rp30002', 'r0000003', 'p0000002');
